package simulator.snapshot;

import common.peer.AvailableResources;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import se.sics.kompics.address.Address;

public class Snapshot {

    private static ConcurrentHashMap<Address, PeerInfo> peers
            = new ConcurrentHashMap<Address, PeerInfo>();

    private static ConcurrentHashMap<Long, List<Long>> performance
            = new ConcurrentHashMap<Long, List<Long>>();

    private static int counter = 0;
    private static String FILENAME = "resManager.out";

    public static void init(int numOfStripes) {
        FileIO.write("", FILENAME);
    }

    public static void addPeer(Address address, AvailableResources availableResources) {
        peers.put(address, new PeerInfo(availableResources));
    }

    public static void removePeer(Address address) {
        peers.remove(address);
    }

    public static void updateNeighbours(Address address, ArrayList<Address> partners) {
        PeerInfo peerInfo = peers.get(address);

        if (peerInfo == null) {
            return;
        }

        peerInfo.setNeighbours(partners);
    }

    public static void report() {
        String str = new String();
        str += "current time: " + counter++ + "\n";
        str += reportNetworkState();
        str += reportDetails();
        str += "###\n";

        System.out.println(str);
        //FileIO.append(str, FILENAME);
    }

    public static void flush(Long jobid, List<Long> measure) {

        LinkedList<Long> l = (LinkedList<Long>) measure;

        long start = l.poll();
        long end = l.poll();

        String line = "JOB " + jobid + " start: " + start + " end: " + end
                + " duration: " + (end - start) + "ms" + "\n";
        System.out.print(line);
        FileIO.append(line, FILENAME);
    }

    public static void record(Long jobid, int jobNo) {

        List<Long> test = null;
        List<Long> t = ((test = performance.get(jobid)) == null) ? new LinkedList<Long>() : test;

        if(t.size() <= 1){
            t.add(System.currentTimeMillis());
        }
        else {
            t.add(1, System.currentTimeMillis());
        }
        
        performance.put(jobid, t);
        
        //System.out.println("\n PERFORMANCE TABLE " + performance + "\n");
        
        //the batch is finished
        if(jobNo == 1){
            System.out.println("\nBENCHMARKING\n");
            flush(jobid, t);
            performance.remove(jobid);
        }
    }

    private static String reportNetworkState() {
        String str = "---\n";
        int totalNumOfPeers = peers.size();
        str += "total number of peers: " + totalNumOfPeers + "\n";

        return str;
    }

    private static String reportDetails() {
        String str = "---\n";
        int maxFreeCpus = 0;
        int minFreeCpus = Integer.MAX_VALUE;
        int maxFreeMemInMb = 0;
        int minFreeMemInMb = Integer.MAX_VALUE;
        for (PeerInfo p : peers.values()) {
            if (p.getNumFreeCpus() > maxFreeCpus) {
                maxFreeCpus = p.getNumFreeCpus();
            }
            if (p.getNumFreeCpus() < minFreeCpus) {
                minFreeCpus = p.getNumFreeCpus();
            }
            if (p.getFreeMemInMbs() > maxFreeMemInMb) {
                maxFreeMemInMb = p.getFreeMemInMbs();
            }
            if (p.getFreeMemInMbs() < minFreeMemInMb) {
                minFreeMemInMb = p.getFreeMemInMbs();
            }
        }
        str += "Peer with max num of free cpus: " + maxFreeCpus + "\n";
        str += "Peer with min num of free cpus: " + minFreeCpus + "\n";
        str += "Peer with max amount of free mem in MB: " + maxFreeMemInMb + "\n";
        str += "Peer with min amount of free mem in MB: " + minFreeMemInMb + "\n";

        return str;
    }
}
