package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * User: jdowling
 */
public class RequestResources {

    public static class Request extends Message {

        private static final long serialVersionUID = -9141244294553635949L;

        private final int numCpus;
        private final int amountMemInMb;
        private final long jobId;
        private final int noofjobs;
        private final int timeToHoldResources;

        public Request(Address source, Address destination, int numCpus,
                int amountMemInMb, long jobId, int noofjobs, int timeToHoldResources) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.jobId = jobId;
            this.noofjobs = noofjobs;
            this.timeToHoldResources = timeToHoldResources;
        }

        public int getAmountMemInMb() {
            return amountMemInMb;
        }

        public int getNumCpus() {
            return numCpus;
        }

        public long getJobId() {
            return this.jobId;
        }

        public int getNoofJobs() {
            return this.noofjobs;
        }

        public int getTimeToHoldResources() {
            return this.timeToHoldResources;
        }
    }

    public static class Response extends Message {

        private static final long serialVersionUID = -3972449777898418316L;

        private final boolean success;
        private final int numCpus;
        private final int amountMemInMb;
        private final long jobId;
        private final int noofjobs;
        private final int queueSize;
        private final int timeToHoldResources;

        public Response(Address source, Address destination, int numCpus, int amountMemInMb,
                long jobId, int noofjobs, int queueSize, int timeToHoldResources, boolean success) {
            super(source, destination);

            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.jobId = jobId;
            this.success = success;
            this.noofjobs = noofjobs;
            this.queueSize = queueSize;
            this.timeToHoldResources = timeToHoldResources;
        }

        public int getNumCpus() {
            return this.numCpus;
        }

        public int getAmountMemInMb() {
            return this.amountMemInMb;
        }

        public long getJobId() {
            return this.jobId;
        }

        public boolean getSuccess() {
            return this.success;
        }

        public int getNoofJobs() {
            return this.noofjobs;
        }

        public int getQueueSize() {
            return this.queueSize;
        }

        public int getTimeToHoldResources() {
            return this.timeToHoldResources;
        }
    }

    public static class RequestTimeout extends Timeout {

        private final Address destination;

        RequestTimeout(ScheduleTimeout st, Address destination) {
            super(st);
            this.destination = destination;
        }

        public Address getDestination() {
            return destination;
        }
    }
}
