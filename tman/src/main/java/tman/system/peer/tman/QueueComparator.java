
package tman.system.peer.tman;

import cyclon.system.peer.cyclon.PeerDescriptor;

/**
 *
 * @author vangelis
 */
public class QueueComparator extends CustomComparator<PeerDescriptor> {

    /**
     * Compares two peerdescriptors based on the queue size. It is used in the
     * building of the overlay, when the other comparators cannot decide which peer has
     * the more available resources
     * 
     * @param p1 - The first peer - peerDescriptor
     * @param p2 - The second peer - peerDescriptor
     * @return 
     */
    @Override
    public int compare(PeerDescriptor p1, PeerDescriptor p2) {

        int p1queue = p1.getResources().getQueueSize();
        int p2queue = p2.getResources().getQueueSize();
        
        if(p1queue > p2queue){
            return 1;
        }
        else if(p1queue < p2queue){
            return -1;
        }
        
        return 0;
    }
}
