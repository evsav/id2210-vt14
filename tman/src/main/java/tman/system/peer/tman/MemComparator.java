
package tman.system.peer.tman;

import common.peer.AvailableResources;
import cyclon.system.peer.cyclon.PeerDescriptor;
import se.sics.kompics.address.Address;

/**
 *
 * @author vangelis
 */
public class MemComparator extends CustomComparator<PeerDescriptor> {

    private Address self;
    private AvailableResources resources;

    public MemComparator(Address address, AvailableResources resources) {
        this.self = address;
        this.resources = resources;
    }

    /**
     * Compares two peers based on the amount of free memory they have. It is 
     * used in the building of the overlay
     * 
     * @param p1 - The first peer
     * @param p2 - The second peer
     * @return 
     */
    @Override
    public int compare(PeerDescriptor p1, PeerDescriptor p2) {

        assert (p1.getAddress().getId() == p2.getAddress().getId());

        int p1mem = p1.getMemInMB();
        int p2mem = p2.getMemInMB();
        int selfmem = this.resources.getFreeMemInMbs();

        if (p1mem < selfmem && p2mem > selfmem) {
            return 1;
        } else if (p1mem > selfmem && p2mem < selfmem) {
            return -1;
        } else if (Math.abs(p1mem - selfmem) < Math.abs(p2mem - selfmem)) {
            return -1;
        }else if (Math.abs(p1mem - selfmem) > Math.abs(p2mem - selfmem)) {
            return 1;
        }
        
        return new QueueComparator().compare(p1, p2);
    }
}
