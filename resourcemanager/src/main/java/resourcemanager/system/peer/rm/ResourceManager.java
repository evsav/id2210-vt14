package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.RequestResource;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import simulator.snapshot.Snapshot;
import system.peer.RmPort;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * Should have some comments here.
 *
 * @author jdowling
 */
public final class ResourceManager extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

    Positive<RmPort> indexPort = positive(RmPort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    Negative<Web> webPort = negative(Web.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);
    ArrayList<Address> neighbours = new ArrayList<Address>();

    private Address self;
    private RmConfiguration configuration;
    Random random;
    private AvailableResources availableResources;

    private final int PROBES = 2;
    private ConcurrentHashMap<Long, LinkedList<RequestResources.Response>> probes;
    private ConcurrentHashMap<Long, RequestResource> jobQueue;
    private Map<Long, RequestResource> inProgress;
    private LinkedList<Job> pendingJobs;
    
    
    Comparator<PeerDescriptor> peerAgeComparator = new Comparator<PeerDescriptor>() {
        @Override
        public int compare(PeerDescriptor t, PeerDescriptor t1) {
            if (t.getAge() > t1.getAge()) {
                return 1;
            } else {
                return -1;
            }
        }
    };

    public ResourceManager() {

        subscribe(handleInit, control);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleRequestResource, indexPort);
        subscribe(handleUpdateTimeout, timerPort);
        subscribe(releaseResources, timerPort);
        subscribe(handleResourceAllocationRequest, networkPort);
        subscribe(handleResourceAllocationResponse, networkPort);
        subscribe(jobDaemon, networkPort);
        subscribe(handleJobComplete, networkPort);
        subscribe(handleTManSample, tmanPort);
    }

    Handler<RmInit> handleInit = new Handler<RmInit>() {
        @Override
        public void handle(RmInit init) {
            self = init.getSelf();
            configuration = init.getConfiguration();
            random = new Random(init.getConfiguration().getSeed());

            availableResources = init.getAvailableResources();
            long period = configuration.getPeriod();
            availableResources = init.getAvailableResources();

            probes = new ConcurrentHashMap<Long, LinkedList<RequestResources.Response>>();
            jobQueue = new ConcurrentHashMap<Long, RequestResource>();
            inProgress = new HashMap<Long, RequestResource>();
            pendingJobs = new LinkedList<Job>();

            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new UpdateTimeout(rst));
            trigger(rst, timerPort);
        }
    };

    Handler<UpdateTimeout> handleUpdateTimeout = new Handler<UpdateTimeout>() {
        @Override
        public void handle(UpdateTimeout event) {

            // pick a random neighbour to ask for index updates from. 
            // You can change this policy if you want to.
            // Maybe a gradient neighbour who is closer to the leader?
            if (neighbours.isEmpty()) {
                return;
            }
            Address dest = neighbours.get(random.nextInt(neighbours.size()));
        }
    };

    Handler<RequestResources.Request> handleResourceAllocationRequest = new Handler<RequestResources.Request>() {
        @Override
        public void handle(RequestResources.Request event) {

            System.out.println(self.getId() + " PROBED BY " + event.getSource().getId());

            boolean success = availableResources.isAvailable(event.getNumCpus(), event.getAmountMemInMb());

            trigger(new RequestResources.Response(self, event.getSource(),
                    availableResources.getNumFreeCpus(),
                    availableResources.getFreeMemInMbs(),
                    event.getJobId(), success), networkPort);
        }
    };

    Handler<RequestResources.Response> handleResourceAllocationResponse = new Handler<RequestResources.Response>() {
        @Override
        public void handle(RequestResources.Response event) {

            System.out.println(self.getId() + " RECEIVED FROM PROBE " + event.getSource().getId());

            int bound = (neighbours.size() < PROBES) ? neighbours.size() : PROBES;

            //collect the probes
            LinkedList<RequestResources.Response> p = probes.get(event.getJobId());
            p = (p == null) ? new LinkedList<RequestResources.Response>() : p;
            p.add(event);
            probes.put(event.getJobId(), p);

            if (p.size() == bound) {

                if (findAvailability(p) > 0) {
                    //find the least loaded peer to assign the job
                    Address peer = findLeastLoaded(p);
                    System.out.println("TWO PROBES ");
                    RequestResource job = jobQueue.get(event.getJobId());
                    
                    Job assign = new Job(self, peer, job.getNumCpus(),
                            job.getMemoryInMbs(), job.getId(), job.getTimeToHoldResource());

                    trigger(assign, networkPort);
                }else{
                    //this job needs rescheduling
                    inProgress.remove(event.getJobId());
                }
            }
        }
    };

    Handler<Job> jobDaemon = new Handler<Job>() {

        @Override
        public void handle(Job event) {

            //put the job in the job queue
            pendingJobs.add(event);

            Job job = null;

            while ((job = pendingJobs.poll()) != null) {

                //reserve the resources
                availableResources.allocate(job.getNumCpus(), job.getAmountMemInMb());

                System.out.println("\nJOB " + job.getJobId() + " ASSIGNED TO " + self + "\n");

                //logger.info("Sleeping {} milliseconds...", job.getJobDuration());
                ScheduleTimeout st = new ScheduleTimeout(job.getJobDuration());
                st.setTimeoutEvent(new JobTimeout(st, job));
                trigger(st, timerPort);
            }
        }
    };

    Handler<JobTimeout> releaseResources = new Handler<JobTimeout>() {

        @Override
        public void handle(JobTimeout event) {

            Job job = event.getJob();

            System.out.println("\nWORKER " + self + " FINISHED JOB " + job.getJobId() + "\n");
            //release the resources
            availableResources.release(job.getNumCpus(), job.getAmountMemInMb());
            Snapshot.record(job.getJobId());
            
            trigger(new JobComplete(self, job.getSource(), job.getJobId()), networkPort);
        }
    };

    Handler<JobComplete> handleJobComplete = new Handler<JobComplete>() {

        @Override
        public void handle(JobComplete event) {

            System.out.println(self + " JOB COMPLETE. REMOVE IT FROM THE QUEUE");            
            probes.remove(event.getJobId());
            jobQueue.remove(event.getJobId());
            inProgress.remove(event.getJobId());
        }
    };

    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            //System.out.println("Received samples: " + event.getSample().size());

            // receive a new list of neighbours
            neighbours.clear();
            neighbours.addAll(event.getSample());
        }
    };

    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        @Override
        public void handle(RequestResource event) {

            System.out.println("Allocate resources: " + event.getNumCpus() + " + " + event.getMemoryInMbs());

            jobQueue.put(event.getId(), event);
            Set<Entry<Long, RequestResource>> set = jobQueue.entrySet();

            int index = 0;
            int bound = (neighbours.size() < PROBES) ? neighbours.size() : PROBES;

            List<Address> copy = new LinkedList<Address>(neighbours);

            for (Entry<Long, RequestResource> entry : set) {
                RequestResource job = entry.getValue();

                //System.out.println("\n JOB " + job.getId() + " SCHEDULED "+ job.isScheduled());
                if (!schedulingInProgress(job)) {
                    
                    inProgress.put(job.getId(), job);
                    Snapshot.record(job.getId());
                    
                    //System.out.println("GOING TO SCHEDULE JOB " + job.getId() + "\n");
                    //probe bound neighbours
                    while (index++ < bound) {

                        Address peer = copy.get(random.nextInt(copy.size()));
                        //System.out.println(self + " PROBING " + current + " NEIGHBOURS " + neighbours.size());
                        RequestResources.Request req = new RequestResources.Request(self, peer, job.getNumCpus(), job.getMemoryInMbs(), job.getId());
                        trigger(req, networkPort);
                        copy.remove(peer);
                    }
                }
            }
        }
    };

    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {
            //event.get
//            System.out.print("[RMMANAGER]: TMAN SAMPLE RECEIVED     ");
//
//            ArrayList<Address> list = event.getSample();
//            for (int i = 0; i < list.size(); i++) {
//                Address a = list.get(i);
//                System.out.print("" + a.getId() + ", ");
//            }
//            System.out.println();
            //System.exit(0);
        }
    };

    private int findAvailability(LinkedList<RequestResources.Response> list) {

        int success = 0;

        for (RequestResources.Response res : list) {
            if (res.getSuccess()) {
                success++;
            }
        }
        return success;
    }

    private Address findLeastLoaded(LinkedList<RequestResources.Response> list) {

        int load = 0;
        Address peer = null;

        for (RequestResources.Response res : list) {
            int total = res.getNumCpus() + res.getAmountMemInMb();
            if (total > load) {
                load = total;
                peer = res.getSource();
            }
        }

        return peer;
    }
    
    private boolean schedulingInProgress(RequestResource job){
        
        return inProgress.containsKey(job.getId());
    }
}
