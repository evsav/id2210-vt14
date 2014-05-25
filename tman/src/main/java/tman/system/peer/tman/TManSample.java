package tman.system.peer.tman;

import cyclon.system.peer.cyclon.PeerDescriptor;
import java.util.ArrayList;
import java.util.List;

import se.sics.kompics.Event;

public class TManSample extends Event {

    List<PeerDescriptor> partners = new ArrayList<PeerDescriptor>();

    public TManSample(List<PeerDescriptor> partners) {
        this.partners = partners;
    }

    public TManSample() {
    }

    public List<PeerDescriptor> getSample() {
        return this.partners;
    }
}
