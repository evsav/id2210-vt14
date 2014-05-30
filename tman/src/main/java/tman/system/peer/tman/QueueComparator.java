/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tman.system.peer.tman;

import cyclon.system.peer.cyclon.PeerDescriptor;

/**
 *
 * @author vangelis
 */
public class QueueComparator extends CustomComparator<PeerDescriptor> {

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
