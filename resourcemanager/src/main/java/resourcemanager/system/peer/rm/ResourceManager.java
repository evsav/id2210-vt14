package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.RequestResource;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;
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

    //--to work with Tman
    private List<PeerDescriptor> neighboursRes;
    private List<PeerDescriptor> neighboursCpu;
    private List<PeerDescriptor> neighboursMem;

    private Address self;
    private RmConfiguration configuration;
    Random random;
    private AvailableResources availableResources;

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
        subscribe(handleRequestResource, indexPort);
        subscribe(releaseResources, timerPort);
        subscribe(jobDaemon, networkPort);
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

            neighboursRes = new LinkedList<PeerDescriptor>();
            neighboursCpu = new LinkedList<PeerDescriptor>();
            neighboursMem = new LinkedList<PeerDescriptor>();

            probes = new ConcurrentHashMap<Long, LinkedList<RequestResources.Response>>();
            jobQueue = new ConcurrentHashMap<Long, RequestResource>();
            inProgress = new HashMap<Long, RequestResource>();
            pendingJobs = new LinkedList<Job>();

            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new UpdateTimeout(rst));
            trigger(rst, timerPort);
        }
    };

    /**
     * The heart of the worker. This handler either executes the incoming jobs if 
     * there are available resources, or places it in the job queue for a later 
     * execution
     */
    Handler<Job> jobDaemon = new Handler<Job>() {

        @Override
        public void handle(Job event) {

            boolean success = availableResources.isAvailable(event.getNumCpus(), event.getAmountMemInMb());

            //if there are no available resources and the job is new, put it in the queue
            //for a later execution and update the queue size
            if (!success && !pendingJobs.contains(event)) {

                pendingJobs.add(event);
                availableResources.setQueueSize(pendingJobs.size());
                System.out.println("WORKER " + self + " QUEUE SIZE " + pendingJobs.size());
                return;
            }

            Snapshot.record(event.getJobId());

            //reserve the resources
            availableResources.allocate(event.getNumCpus(), event.getAmountMemInMb());

            /*
                after the allocation of resources the job needs to be removed from the
                queue, and the queue size in the available resources needs to be updated accordingly.
                Queue size is vital for TMan to construct the overlay
            */
            if (!pendingJobs.isEmpty()) {
                pendingJobs.remove();
                availableResources.setQueueSize(pendingJobs.size());
            }

            ScheduleTimeout st = new ScheduleTimeout(event.getTimetoholdResource());
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

            /*
                if there are more jobs waiting in the queue, trigger jobDaemon
                to handle them
            */
            if (pendingJobs.size() > 0) {
                job = pendingJobs.peek();
                trigger(job, networkPort);
            }
        }
    };


    /**
     * This handler deals with incoming allocation requests from the application layer
     */
    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        @Override
        public void handle(RequestResource event) {

            List<PeerDescriptor> copy = new LinkedList<PeerDescriptor>(getList(event.getGradientType()));

            if (copy.size() > 0) {
                //record the timestamp when the allocation request arrives
                Snapshot.record(event.getId());

                /*
                    assign the job to the best peer. The TMan layer sends here
                    the first four best workers
                */                
                PeerDescriptor peer = copy.get(0);
                copy.remove(peer);

                Job assign = new Job(self, peer.getAddress(), event.getNumCpus(),
                        event.getMemoryInMbs(), event.getTimeToHoldResource(), event.getId());

                trigger(assign, networkPort);
            }
        }
    };

    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {

            updateSample(event.getGradientType(), event.getSample());
        }
    };

    /**
     * update the corresponding list of neighbours, depending on the gradient type
     * 1 is combined resources, 2 is cpu, 3 is memory. The gradient type is defined
     * in the scenario, as a fifth parameter to the scochastic process
     * 
     * @param gradientType - The current gradient type
     * @param sample - The incoming sample from the TMan layer
     */
    private void updateSample(int gradientType, List<PeerDescriptor> sample) {

        switch (gradientType) {
            case 1:
                this.neighboursRes.clear();
                this.neighboursRes.addAll(sample);
                break;
            case 2:
                this.neighboursCpu.clear();
                this.neighboursCpu.addAll(sample);
                break;
            case 3:
                this.neighboursMem.clear();
                this.neighboursMem.addAll(sample);
        }
    }
    
    /**
     * Returns the neighbour list according to the gradient type.
     * 1 is for combined resources, 2 is for cpu and 3 is for memory
     * 
     * @param gradientType - The current gradient type
     * @return 
     */
    private List<PeerDescriptor> getList(int gradientType){
        switch(gradientType){
            case 1:
                return this.neighboursRes;
            case 2:
                return this.neighboursCpu;
            case 3:
                return this.neighboursMem;
        }
        
        return null;
    }
}
