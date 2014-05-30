
package tman.system.peer.tman;

import common.peer.AvailableResources;
import cyclon.system.peer.cyclon.PeerDescriptor;
import se.sics.kompics.address.Address;

/**
 *
 * @author vangelis
 */
public class CpuComparator extends CustomComparator<PeerDescriptor> {

    private Address self;
    private AvailableResources resources;

    public CpuComparator(Address address, AvailableResources resources) {
        this.self = address;
        this.resources = resources;
    }

    /**
     * Compares two peers based on the free cpus they have. It is used in 
     * the building of the overlay
     * 
     * @param p1 - The first peer
     * @param p2 - The second peer
     * @return 
     */
    @Override
    public int compare(PeerDescriptor p1, PeerDescriptor p2) {

        //assert (p1.getAddress().getId() == p2.getAddress().getId());

        int p1cpu = p1.getCpus();
        int p2cpu = p2.getCpus();
        int selfcpu = this.resources.getNumFreeCpus();

        if (p1cpu < selfcpu && p2cpu > selfcpu) {
            return 1;
        } else if (p1cpu > selfcpu && p2cpu < selfcpu) {
            return -1;
        } else if (Math.abs(p1cpu - selfcpu) < Math.abs(p2cpu - selfcpu)) {
            return -1;
        } else if (Math.abs(p1cpu - selfcpu) > Math.abs(p2cpu - selfcpu)) {
            return 1;
        }
             
        return new QueueComparator().compare(p1, p2);
    }
}
