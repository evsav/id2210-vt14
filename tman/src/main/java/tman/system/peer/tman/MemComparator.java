/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tman.system.peer.tman;

import common.peer.AvailableResources;
import cyclon.system.peer.cyclon.PeerDescriptor;
import java.util.Comparator;
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
