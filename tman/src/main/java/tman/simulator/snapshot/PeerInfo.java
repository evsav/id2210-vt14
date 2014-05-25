package tman.simulator.snapshot;

import cyclon.system.peer.cyclon.PeerDescriptor;
import java.util.ArrayList;
import java.util.List;
import se.sics.kompics.address.Address;

public class PeerInfo {

    private List<PeerDescriptor> tmanPartners;
    private List<Address> cyclonPartners;

    public PeerInfo() {
        this.tmanPartners = new ArrayList<PeerDescriptor>();
        this.cyclonPartners = new ArrayList<Address>();
    }

    public void updateTManPartners(List<PeerDescriptor> partners) {
        this.tmanPartners = partners;
    }

    public void updateCyclonPartners(List<Address> partners) {
        this.cyclonPartners = partners;
    }

    public List<PeerDescriptor> getTManPartners() {
        return this.tmanPartners;
    }

    public List<Address> getCyclonPartners() {
        return this.cyclonPartners;
    }
}
