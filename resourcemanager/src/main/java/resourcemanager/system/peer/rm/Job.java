/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 *
 * @author vangelis
 */
public class Job extends Message {
    private static final long serialVersionUID = 450032122944785443L;
    
    private final int numCpus;
    private final int amountMemInMb;
    private final long jobId;
    private final long timetoholdResource;
    
    public Job(Address source, Address destination, int numCpus, 
            int amountMemInMb, long timetoholdResource, long jobId){
        
        super(source, destination);
        
        this.numCpus = numCpus;
        this.amountMemInMb = amountMemInMb;
        this.timetoholdResource = timetoholdResource;
        this.jobId = jobId;
    }
    
    public int getNumCpus(){
        return this.numCpus;
    }
    
    public int getAmountMemInMb(){
        return this.amountMemInMb;
    }
    
    public long getJobId(){
        return this.jobId;
    }
    
    public long getTimetoholdResource(){
        return this.timetoholdResource;
    }
}
