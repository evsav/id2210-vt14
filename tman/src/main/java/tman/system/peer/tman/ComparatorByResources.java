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
public class ComparatorByResources implements Comparator<PeerDescriptor> {

    private Address self;
    private AvailableResources resources;
    
    public ComparatorByResources(Address address, AvailableResources resources){
        this.self = address;
        this.resources = resources;
    }
    
    @Override
    public int compare(PeerDescriptor p1, PeerDescriptor p2) {

        assert (p1.getAddress().getId() == p2.getAddress().getId());
        
        int p1resources = p1.getMemInMB() + p1.getCpus();
        int p2resources = p2.getMemInMB() + p2.getCpus();
        int selfresources = this.resources.getFreeMemInMbs() + this.resources.getNumFreeCpus();
        
        if (p1resources < selfresources && p2resources > selfresources) {
            return 1;
        } else if (p2resources < selfresources && p1resources > selfresources) {
            return -1;
        } else if (Math.abs(p1resources - selfresources) < Math.abs(p2resources - selfresources)) {
            return -1;
        }
        return 1;
    }
}
