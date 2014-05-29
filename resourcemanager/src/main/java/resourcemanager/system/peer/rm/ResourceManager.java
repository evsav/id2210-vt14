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

    private final int PROBES = 4;
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

            boolean success = availableResources.isAvailable(event.getNumCpus(), event.getAmountMemInMb());

            trigger(new RequestResources.Response(self, event.getSource(),
                    availableResources.getNumFreeCpus(),
                    availableResources.getFreeMemInMbs(),
                    event.getJobId(), pendingJobs.size(), success), networkPort);
        }
    };

    Handler<RequestResources.Response> handleResourceAllocationResponse = new Handler<RequestResources.Response>() {
        @Override
        public void handle(RequestResources.Response event) {

            //collect the probes
            LinkedList<RequestResources.Response> p = probes.get(event.getJobId());
            p = (p == null) ? new LinkedList<RequestResources.Response>() : p;
            p.add(event);
            probes.put(event.getJobId(), p);

            //when the number of received responses becomes equal to the number of 
            //the probed neighbours, then it's time to find the best one
            if (p.size() == PROBES) {

                //find the least loaded peer to assign the job
                Address peer = findLeastLoaded(p);
                RequestResource job = jobQueue.get(event.getJobId());

                Job assign = new Job(self, peer, job.getNumCpus(),
                        job.getMemoryInMbs(), job.getId(), job.getTimeToHoldResource());

                trigger(assign, networkPort);
            }
        }
    };

    Handler<Job> jobDaemon = new Handler<Job>() {

        @Override
        public void handle(Job event) {

            //put the job in the job queue
            boolean success = availableResources.isAvailable(event.getNumCpus(), event.getAmountMemInMb());

            //if there are no resources available, and the job is new, put it in the queue
            if (!success && !pendingJobs.contains(event)) {
                pendingJobs.add(event);
                System.out.println("WORKER " + self + " QUEUE SIZE " + pendingJobs.size());
                return;
            }

            //record the timestamp the moment the resources are found, and before are allocated
            Snapshot.record(event.getJobId());
            //reserve the resources
            availableResources.allocate(event.getNumCpus(), event.getAmountMemInMb());

            if (!pendingJobs.isEmpty()) {
                pendingJobs.remove();
            }

            System.out.println("\nJOB " + event.getJobId() + " ASSIGNED TO " + self + "\n");

            ScheduleTimeout st = new ScheduleTimeout(event.getJobDuration());
            st.setTimeoutEvent(new JobTimeout(st, event));
            trigger(st, timerPort);
        }
    };

    Handler<JobTimeout> releaseResources = new Handler<JobTimeout>() {

        @Override
        public void handle(JobTimeout event) {

            Job job = event.getJob();

            System.out.println("\nWORKER " + self + " FINISHED JOB " + job.getJobId() + "\n");
            //release the resources
            availableResources.release(job.getNumCpus(), job.getAmountMemInMb());
            //Snapshot.record(job.getJobId());

            if (pendingJobs.size() > 0) {
                job = pendingJobs.peek();
                trigger(job, networkPort);
            }
            //trigger(new JobComplete(self, job.getSource(), job.getJobId()), networkPort);
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

            //receive a new list of neighbours
            neighbours.clear();
            neighbours.addAll(event.getSample());
        }
    };

    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        @Override
        public void handle(RequestResource event) {

            System.out.println("Allocate resources: " + event.getNumCpus() + " + " + event.getMemoryInMbs());

            jobQueue.put(event.getId(), event);
//            Set<Entry<Long, RequestResource>> set = jobQueue.entrySet();

            int index = 0;
            int bound = (neighbours.size() < PROBES) ? neighbours.size() : PROBES;

            List<Address> copy = new LinkedList<Address>(neighbours);

            Snapshot.record(event.getId());

            //probe bound neighbours
            while (index++ < bound) {
                Address peer = copy.get(random.nextInt(copy.size()));
                //System.out.println("JOB " + event.getId() + " PROBING " + peer.getId());
                RequestResources.Request req = new RequestResources.Request(self, peer, event.getNumCpus(), event.getMemoryInMbs(), event.getId());
                trigger(req, networkPort);
                copy.remove(peer);
            }
        }
    };

//    //not used
//    private int findAvailability(LinkedList<RequestResources.Response> list) {
//
//        int success = 0;
//
//        for (RequestResources.Response res : list) {
//            if (res.getSuccess()) {
//                success++;
//            }
//        }
//        return success;
//    }

    /**
     * finds the least loaded peer, based on the queue size
     * 
     * @param list. Contains the responses that were received from the probed neighbours
     * @return the least loaded neighbour
     */
    private Address findLeastLoaded(LinkedList<RequestResources.Response> list) {

        int min = Integer.MAX_VALUE;
        Address peer = null;

        for (RequestResources.Response res : list) {
            if (min > res.getQueueSize()) {
                min = res.getQueueSize();
                peer = res.getDestination();
            }
        }

        return peer;
    }
}
