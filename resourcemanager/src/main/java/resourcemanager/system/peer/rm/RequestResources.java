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
        private final long timetoholdResource;
        
        public Request(Address source, Address destination, int numCpus, 
                int amountMemInMb, long timetoholdResource, long jobId) {
            
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.jobId = jobId;
            this.timetoholdResource = timetoholdResource;
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
        
        public long getTimetoholdResource(){
            return this.timetoholdResource;
        }
    }

    public static class Response extends Message {

        private static final long serialVersionUID = -3972449777898418316L;

        private final boolean success;
        private final int numCpus;
        private final int amountMemInMb;
        private final long jobId;
        private final int queueSize;

        public Response(Address source, Address destination, int numCpus, 
                int amountMemInMb, long jobId, int queueSize, boolean success) {
            
            super(source, destination);
            
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.jobId = jobId;
            this.success = success;
            this.queueSize = queueSize;
        }

        public int getNumCpus(){
            return this.numCpus;
        }
        
        public int getAmountMemInMb(){
            return this.amountMemInMb;
        }
        
        public long getJobId() {
            return this.jobId;
        }

        public boolean getSuccess() {
            return this.success;
        }
        
        public int getQueueSize(){
            return this.queueSize;
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
