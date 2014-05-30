/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
