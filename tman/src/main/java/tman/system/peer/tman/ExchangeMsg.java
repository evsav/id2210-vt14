package tman.system.peer.tman;

import java.util.UUID;

import cyclon.system.peer.cyclon.DescriptorBuffer;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class ExchangeMsg {

    public static class Request extends Message {

        private static final long serialVersionUID = 8493601671018888143L;
        private final UUID requestId;
        private final DescriptorBuffer randomBuffer;
        private int gradientType;

        public Request(UUID requestId, DescriptorBuffer randomBuffer, Address source,
                Address destination, int gradientType) {
            super(source, destination);
            this.requestId = requestId;
            this.randomBuffer = randomBuffer;
            this.gradientType = gradientType;
        }

        public UUID getRequestId() {
            return requestId;
        }

        public DescriptorBuffer getRandomBuffer() {
            return randomBuffer;
        }

        public int getSize() {
            return 0;
        }
        
        public int getGradientType(){
            return this.gradientType;
        }
    }

    public static class Response extends Message {

        private static final long serialVersionUID = -5022051054665787770L;
        private final UUID requestId;
        private final DescriptorBuffer selectedBuffer;
        private int gradientType;

        public Response(UUID requestId, DescriptorBuffer selectedBuffer, Address source,
                Address destination, int gradientType) {
            
            super(source, destination);
            this.requestId = requestId;
            this.selectedBuffer = selectedBuffer;
            this.gradientType = gradientType;
        }

        public UUID getRequestId() {
            return requestId;
        }

        public DescriptorBuffer getSelectedBuffer() {
            return selectedBuffer;
        }

        public int getSize() {
            return 0;
        }
        
        public int getGradientType(){
            return this.gradientType;
        }
    }

    public static class RequestTimeout extends Timeout {

        private final Address peer;

        public RequestTimeout(ScheduleTimeout request, Address peer) {
            super(request);
            this.peer = peer;
        }

        public Address getPeer() {
            return peer;
        }
    }
}
