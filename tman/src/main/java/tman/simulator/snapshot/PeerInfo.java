package tman.simulator.snapshot;

import cyclon.system.peer.cyclon.PeerDescriptor;
import java.util.ArrayList;
import java.util.List;
import se.sics.kompics.address.Address;

public class PeerInfo {

    private List<PeerDescriptor> tmanPartnersRes;
    private List<PeerDescriptor> tmanPartnersCpu;
    private List<PeerDescriptor> tmanPartnersMem;
    private List<Address> cyclonPartners;

    public PeerInfo() {
        this.tmanPartnersRes = new ArrayList<PeerDescriptor>();
        this.tmanPartnersCpu = new ArrayList<PeerDescriptor>();
        this.tmanPartnersMem = new ArrayList<PeerDescriptor>();
        this.cyclonPartners = new ArrayList<Address>();
    }

    public void updateCyclonPartners(List<Address> partners) {
        this.cyclonPartners = partners;
    }

    public List<PeerDescriptor> getTManPartnersRes() {
        return this.tmanPartnersRes;
    }

    public List<PeerDescriptor> getTManPartnersCpu() {
        return this.tmanPartnersCpu;
    }

    public List<PeerDescriptor> getTManPartnersMem() {
        return this.tmanPartnersMem;
    }

    public List<Address> getCyclonPartners() {
        return this.cyclonPartners;
    }
}
