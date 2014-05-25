package cyclon.system.peer.cyclon;

import common.peer.AvailableResources;
import java.io.Serializable;
import se.sics.kompics.address.Address;

public class PeerDescriptor implements Comparable<PeerDescriptor>, Serializable {

    private static final long serialVersionUID = 1906679375438244117L;
    private final Address peerAddress;
    private int age;
    private AvailableResources resources;
    
    public PeerDescriptor(Address peerAddress, AvailableResources resources) {
        this.peerAddress = peerAddress;
        this.age = 0;
        
        this.resources = resources;
    }

    public int incrementAndGetAge() {
        age++;
        return age;
    }

    public int getAge() {
        return age;
    }

    public Address getAddress() {
        return peerAddress;
    }
    
    public AvailableResources getResources(){
        return this.resources;
    }

    public int getMemInMB(){
        
        return this.resources.getFreeMemInMbs();
    }
    
    public int getCpus(){
        
        return this.resources.getNumFreeCpus();
    }
    
    @Override
    public int compareTo(PeerDescriptor that) {
        if (this.age > that.age) {
            return 1;
        }
        if (this.age < that.age) {
            return -1;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((peerAddress == null) ? 0 : peerAddress.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PeerDescriptor other = (PeerDescriptor) obj;
        if (peerAddress == null) {
            if (other.peerAddress != null) {
                return false;
            }
        } else if (!peerAddress.equals(other.peerAddress)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return peerAddress + "";
    }

}
