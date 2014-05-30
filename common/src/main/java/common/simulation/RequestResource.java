package common.simulation;

import se.sics.kompics.Event;

public final class RequestResource extends Event {

    private final long id;
    private final int numCpus;
    private final int memoryInMbs;
    private final int timeToHoldResource;
    private boolean scheduled;
    private final int gradientType;

    public RequestResource(long id, int numCpus, int memoryInMbs, int timeToHoldResource, int gradientType) {
        this.id = id;
        this.numCpus = numCpus;
        this.memoryInMbs = memoryInMbs;
        this.timeToHoldResource = timeToHoldResource;
        this.gradientType = gradientType;
    }

    public long getId() {
        return id;
    }

    public int getTimeToHoldResource() {
        return timeToHoldResource;
    }

    public int getMemoryInMbs() {
        return memoryInMbs;
    }

    public int getNumCpus() {
        return numCpus;
    }

    public void setScheduled(boolean scheduled){
        this.scheduled = scheduled;
    }
    
    public boolean isScheduled(){
        return this.scheduled;
    }
    
    public int getGradientType(){
        return this.gradientType;
    }
}
