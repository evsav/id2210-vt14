package tman.system.peer.tman;

import cyclon.system.peer.cyclon.PeerDescriptor;
import java.util.ArrayList;
import java.util.List;

import se.sics.kompics.Event;

public class TManSample extends Event {

    List<PeerDescriptor> partners = new ArrayList<PeerDescriptor>();

    private int gradientType;
    
    public TManSample(List<PeerDescriptor> partners, int gradientType) {
        this.partners = partners;
        this.gradientType = gradientType;
    }

    public TManSample() {
    }

    public List<PeerDescriptor> getSample() {
        return this.partners;
    }
    
    public int getGradientType(){
        return this.gradientType;
    }
}
