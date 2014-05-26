package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.BatchRequestResource;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private ConcurrentHashMap<Long, BatchRequestResource> jobQueue;
    private Map<Long, BatchRequestResource> inProgress;
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
        subscribe(handleBatchRequest, indexPort);
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
            jobQueue = new ConcurrentHashMap<Long, BatchRequestResource>();
            inProgress = new HashMap<Long, BatchRequestResource>();
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
            //Address dest = neighbours.get(random.nextInt(neighbours.size()));
            Address dest = neighbours.get(random.nextInt(neighbours.size()));
        }
    };

    Handler<RequestResources.Request> handleResourceAllocationRequest = new Handler<RequestResources.Request>() {
        @Override
        public void handle(RequestResources.Request event) {

            //System.out.println(self.getId() + " PROBED BY " + event.getSource().getId());
            //check for available resources for a task of the job
            boolean success = availableResources.isAvailable(event.getNumCpus() / 2, event.getAmountMemInMb() / 2);

            trigger(new RequestResources.Response(self, event.getSource(),
                    availableResources.getNumFreeCpus(),
                    availableResources.getFreeMemInMbs(),
                    event.getJobId(), event.getNoofJobs(), pendingJobs.size(), success), networkPort);
        }
    };

    Handler<RequestResources.Response> handleResourceAllocationResponse = new Handler<RequestResources.Response>() {
        @Override
        public void handle(RequestResources.Response event) {

            int noofjobs = event.getNoofJobs();

            //collect the probes
            LinkedList<RequestResources.Response> p = probes.get(event.getJobId());
            p = (p == null) ? new LinkedList<RequestResources.Response>() : p;
            p.add(event);

            probes.put(event.getJobId(), p);

            System.out.println("JOB " + event.getJobId() + " PROBES SIZE " + p.size() + " NOOFJOBS " + (noofjobs));
            if (p.size() == (PROBES * noofjobs)) {

                System.out.println((p.size()) + " PROBES ");
                BatchRequestResource job = jobQueue.get(event.getJobId());

                //assign tasks to the least loaded workers
                for (int i = 0; i < noofjobs; i++) {

                    //find the least loaded peer to assign the job
                    RequestResources.Response peer = findLeastLoaded(p);
                    p.remove(peer);

                    Job assign = new Job(self, peer.getSource(), job.getNumCpus() / 2,
                            job.getMemoryInMbs() / 2, job.getId(), i, job.getTimeToHoldResource());

                    trigger(assign, networkPort);
                }
            }
        }
    };

    Handler<Job> jobDaemon = new Handler<Job>() {

        @Override
        public void handle(Job event) {

            boolean success = availableResources.isAvailable(event.getNumCpus(), event.getAmountMemInMb());

            if (!success && !pendingJobs.contains(event)) {
                //put the job in the job queue
                pendingJobs.add(event);
                System.out.println("WORKER " + self + " QUEUE SIZE " + pendingJobs.size());
                return;
            }

            Snapshot.record(event.getJobId(), event.getJobNo());
            //reserve the resources
            availableResources.allocate(event.getNumCpus(), event.getAmountMemInMb());

            if (!pendingJobs.isEmpty()) {
                pendingJobs.remove();
            }

            System.out.println("\nJOB " + event.getJobId() + ", TASK " + event.getJobNo() + ", ASSIGNED TO " + self + "\n");

            //logger.info("Sleeping {} milliseconds...", job.getJobDuration());
            ScheduleTimeout st = new ScheduleTimeout(event.getJobDuration());
            st.setTimeoutEvent(new JobTimeout(st, event));
            trigger(st, timerPort);
        }
    };

    Handler<JobTimeout> releaseResources = new Handler<JobTimeout>() {

        @Override
        public void handle(JobTimeout event) {

            Job job = event.getJob();

            //System.out.println("\nWORKER " + self + " FINISHED JOB " + job.getJobId() + "\n");
            //release the resources
            availableResources.release(job.getNumCpus(), job.getAmountMemInMb());

            //trigger(new JobComplete(self, job.getSource(), job.getJobId()), networkPort);
            if (pendingJobs.size() > 0) {
                job = pendingJobs.peek();
                trigger(job, networkPort);
            }
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

    Handler<BatchRequestResource> handleBatchRequest = new Handler<BatchRequestResource>() {

        @Override
        public void handle(BatchRequestResource event) {

            int noOfJobs = event.getNoofJobs();

            if (noOfJobs > neighbours.size() || neighbours.isEmpty()) {
                return;
            }

            jobQueue.put(event.getId(), event);

            int index = 0;
            int bound = (neighbours.size() < PROBES) ? neighbours.size() : PROBES;
            bound *= noOfJobs;

            if (bound > neighbours.size()) {
                return;
            }

            List<Address> copy = new LinkedList<Address>(neighbours);

            Snapshot.record(event.getId(), bound);

            //System.out.println("NO OF PROBES " + bound + " NEIGHBORS SIZE " + copy.size());
            //probe PROBES * noofjobs workers
            while (index++ < bound) {

                int rand = random.nextInt(copy.size());

                //System.out.println(rand + " SCHEDULING JOB " + event.getId());
                Address peer = copy.get(rand);
                copy.remove(peer);
                //System.out.println(self + " REQUESTING RESOURCES for job " + event.getId() + " FROM NEIGHBR " + peer.getId());
                RequestResources.Request req = new RequestResources.Request(self, peer, event.getNumCpus(),
                        event.getMemoryInMbs(), event.getId(), event.getNoofJobs());

                trigger(req, networkPort);
            }
        }
    };

    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {

            //TMAN NOT USED IN BASIC SPARROW IMPLEMENTATION
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

    private RequestResources.Response findLeastLoaded(LinkedList<RequestResources.Response> list) {

        RequestResources.Response peer = null;
        //find by queue size
        int min = Integer.MAX_VALUE;

        for (RequestResources.Response res : list) {
            if (res.getQueueSize() < min) {
                min = res.getQueueSize();
                peer = res;
            }
        }

        return peer;
    }
}
