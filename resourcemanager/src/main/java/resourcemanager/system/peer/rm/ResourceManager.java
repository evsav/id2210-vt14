package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.RequestResource;
import cyclon.system.peer.cyclon.CyclonSample;
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

    Handler<UpdateTimeout> handleUpdateTimeout = new Handler<UpdateTimeout>() {
        @Override
        public void handle(UpdateTimeout event) {

            // pick a random neighbour to ask for index updates from. 
            // You can change this policy if you want to.
            // Maybe a gradient neighbour who is closer to the leader?
//            if (neighbours.isEmpty()) {
//                return;
//            }
//            //Address dest = neighbours.get(random.nextInt(neighbours.size()));
//            PeerDescriptor dest = neighbours.get(random.nextInt(neighbours.size()));
        }
    };

    Handler<Job> jobDaemon = new Handler<Job>() {

        @Override
        public void handle(Job event) {

            boolean success = availableResources.isAvailable(event.getNumCpus(), event.getAmountMemInMb());

            if (!success && !pendingJobs.contains(event)) {
                //put the job in the job queue, and update the queue size
                pendingJobs.add(event);
                availableResources.setQueueSize(pendingJobs.size());
                System.out.println("WORKER " + self + " QUEUE SIZE " + pendingJobs.size());
                return;
            }

            Snapshot.record(event.getJobId());

            //reserve the resources
            availableResources.allocate(event.getNumCpus(), event.getAmountMemInMb());

            if (!pendingJobs.isEmpty()) {
                pendingJobs.remove();
                availableResources.setQueueSize(pendingJobs.size());
            }

            System.out.println("\nJOB " + event.getJobId() + " ASSIGNED TO " + self + "\n");

            //logger.info("Sleeping {} milliseconds...", job.getJobDuration());
            ScheduleTimeout st = new ScheduleTimeout(event.getTimetoholdResource());
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
            // IGNORE CYCLON
        }
    };

    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        @Override
        public void handle(RequestResource event) {

            System.out.println("Allocate resources: " + event.getNumCpus() + " + " + event.getMemoryInMbs());

            List<PeerDescriptor> copy = new LinkedList<PeerDescriptor>(getList(event.getGradientType()));

            if (copy.size() > 0) {
                Snapshot.record(event.getId());

                //assign the job to the best peer
                PeerDescriptor peer = copy.get(0); //copy.get(random.nextInt(copy.size()));
                copy.remove(peer);
                System.out.println(self + " PICKING NEIGHBOUR " + peer.getAddress());

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
