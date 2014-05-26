/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package resourcemanager.system.peer.rm;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 *
 * @author vangelis
 */
public class JobTimeout extends Timeout {

    private Job job;
    
    public JobTimeout(ScheduleTimeout timeout, Job job){
        super(timeout);
        
        this.job = job;
    }
    
    public Job getJob() {
        return this.job;
    }
}
