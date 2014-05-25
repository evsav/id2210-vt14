/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common.simulation;

import se.sics.kompics.Event;

/**
 *
 * @author vangelis
 */
public class BatchRequestResource extends Event {

    private final long id;
    private final int numCpus;
    private final int memoryInMbs;
    private final int timeToHoldResource;
    private final int noofJobs;

    public BatchRequestResource(long id, int numCpus, int memoryInMbs, int noofJobs, int timeToHoldResource) {
        this.id = id;
        this.numCpus = numCpus;
        this.memoryInMbs = memoryInMbs;
        this.noofJobs = noofJobs;
        this.timeToHoldResource = timeToHoldResource;
    }

    public long getId() {
        return this.id;
    }

    public int getNumCpus() {
        return this.numCpus;
    }

    public int getMemoryInMbs() {
        return this.memoryInMbs;
    }

    public int getTimeToHoldResource() {
        return this.timeToHoldResource;
    }

    public int getNoofJobs() {
        return this.noofJobs;
    }   
}
