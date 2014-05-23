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
public class JobComplete extends Message {
    private static final long serialVersionUID = -8171298201785447762L;

    private final long jobId;
    
    public JobComplete(Address source, Address destination, long jobId){
        super(source, destination);
        
        this.jobId = jobId;
    }
    
    public long getJobId(){
        return this.jobId;
    }
}
