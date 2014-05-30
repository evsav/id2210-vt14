package tman.system.peer.tman;

import common.configuration.TManConfiguration;
import common.peer.AvailableResources;
import java.util.ArrayList;

import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.DescriptorBuffer;
import cyclon.system.peer.cyclon.PeerDescriptor;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

import tman.simulator.snapshot.Snapshot;

public final class TMan extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(TMan.class);

    Negative<TManSamplePort> tmanPort = negative(TManSamplePort.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    private long period;
    private Address self;
    private List<PeerDescriptor> tmanPartnersRes;
    private List<PeerDescriptor> tmanPartnersCpu;
    private List<PeerDescriptor> tmanPartnersMem;

    private List<PeerDescriptor> cyclonPartners;
    private TManConfiguration tmanConfiguration;
    private Random r;
    private AvailableResources availableResources;

    public class TManSchedule extends Timeout {

        public TManSchedule(SchedulePeriodicTimeout request) {
            super(request);
        }

        public TManSchedule(ScheduleTimeout request) {
            super(request);
        }
    }

    public TMan() {
        tmanPartnersRes = new LinkedList<PeerDescriptor>();
        tmanPartnersCpu = new LinkedList<PeerDescriptor>();
        tmanPartnersMem = new LinkedList<PeerDescriptor>();
        cyclonPartners = new ArrayList<PeerDescriptor>();

        subscribe(handleInit, control);
        subscribe(handleRound, timerPort);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleTManPartnersResponse, networkPort);
        subscribe(handleTManPartnersRequest, networkPort);
    }

    Handler<TManInit> handleInit = new Handler<TManInit>() {
        @Override
        public void handle(TManInit init) {
            self = init.getSelf();
            tmanConfiguration = init.getConfiguration();
            period = tmanConfiguration.getPeriod();
            r = new Random(tmanConfiguration.getSeed());

            availableResources = init.getAvailableResources();

            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new TManSchedule(rst));
            trigger(rst, timerPort);
        }
    };

    Handler<TManSchedule> handleRound = new Handler<TManSchedule>() {
        @Override
        public void handle(TManSchedule event) {

            buildGradient(1);
            //buildGradient(2);
            //buildGradient(3);
        }
    };

    private void buildGradient(int gradientType) {
        // merge cyclonPartners into TManPartners
        List<PeerDescriptor> partners = getList(gradientType);
                
        partners = merge(partners, cyclonPartners);

        if (!partners.isEmpty()) {
            partners = queueBasedDeduplication(new LinkedList<PeerDescriptor>(partners));
            Collections.sort(partners, getComparator(gradientType, self, availableResources));

            int randomIndex = (((int) partners.size() / 3) == 0)
                    ? 0 : new Random().nextInt((int) partners.size() / 3);
            PeerDescriptor peer = partners.get(randomIndex);

            PeerDescriptor mydescriptor = new PeerDescriptor(self, availableResources);
            List<PeerDescriptor> md = new ArrayList<PeerDescriptor>();
            md.add(mydescriptor);

            partners = merge(partners, md);

            Collections.sort(partners, getComparator(gradientType, peer.getAddress(), peer.getResources()));
            DescriptorBuffer buffer = new DescriptorBuffer(self, partners);

            //send a gossip message to the selected peer
            trigger(new ExchangeMsg.Request(UUID.randomUUID(), buffer, self, peer.getAddress(), gradientType), networkPort);

            // Publish the sample to connected components
            List<PeerDescriptor> toSend = new LinkedList<PeerDescriptor>(partners.subList(0, randomIndex));

            trigger(new TManSample(toSend, gradientType), tmanPort);
        }
    }

    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {

            cyclonPartners = event.getSample();
            
            tmanPartnersRes.clear();
            tmanPartnersRes.addAll(cyclonPartners);
            
            tmanPartnersCpu.clear();
            tmanPartnersCpu.addAll(cyclonPartners);
            
            tmanPartnersMem.clear();
            tmanPartnersMem.addAll(cyclonPartners);
        }
    };

    Handler<ExchangeMsg.Request> handleTManPartnersRequest = new Handler<ExchangeMsg.Request>() {
        @Override
        public void handle(ExchangeMsg.Request event) {
            int gradientType = event.getGradientType();

            //RECEIVE BUFFER FROM Q
            List<PeerDescriptor> buffer = event.getRandomBuffer().getDescriptors();

            PeerDescriptor mydescriptor = new PeerDescriptor(self, availableResources);
            List<PeerDescriptor> md = new ArrayList<PeerDescriptor>();
            md.add(mydescriptor);

            List<PeerDescriptor> partners = getList(gradientType);
            partners = merge(partners, md);

            //FIND OUT WHO Q IS
            PeerDescriptor q = null;
            for (PeerDescriptor peer : buffer) {
                if (peer.getAddress() == event.getSource()) {
                    q = peer;
                }
            }

            Collections.sort(partners, getComparator(gradientType, q.getAddress(), q.getResources()));

            DescriptorBuffer debuffer = new DescriptorBuffer(self, partners);
            //send a message back to the peer that initiated the gossip
            trigger(new ExchangeMsg.Response(event.getRequestId(), debuffer, self, event.getSource(), gradientType), networkPort);

            setList(gradientType, merge(partners, buffer));
        }
    };

    Handler<ExchangeMsg.Response> handleTManPartnersResponse = new Handler<ExchangeMsg.Response>() {
        @Override
        public void handle(ExchangeMsg.Response event) {
            int gradientType = event.getGradientType();

            List<PeerDescriptor> buffer = event.getSelectedBuffer().getDescriptors();

            List<PeerDescriptor> partners = getList(gradientType);
            partners = merge(partners, buffer);
            Collections.sort(partners, getComparator(gradientType, self, availableResources));
        }
    };

    // TODO - if you call this method with a list of entries, it will
    // return a single node, weighted towards the 'best' node (as defined by
    // ComparatorById) with the temperature controlling the weighting.
    // A temperature of '1.0' will be greedy and always return the best node.
    // A temperature of '0.000001' will return a random node.
    // A temperature of '0.0' will throw a divide by zero exception :)
    // Reference:
    // http://webdocs.cs.ualberta.ca/~sutton/book/2/node4.html
    public PeerDescriptor getSoftMaxAddress(List<PeerDescriptor> entries) {
        //Collections.sort(entries, new ComparatorById(self));

        if (entries.isEmpty()) {
            return null;
        }

        Collections.sort(entries, new ResourceComparator(self, availableResources));

        double rnd = r.nextDouble();
        double total = 0.0d;
        double[] values = new double[entries.size()];
        int j = entries.size() + 1;

        for (int i = 0; i < entries.size(); i++) {
            // get inverse of values - lowest have highest value.
            double val = j;
            j--;
            values[i] = Math.exp(val / tmanConfiguration.getTemperature());
            total += values[i];
        }

        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                values[i] += values[i - 1];
            }
            // normalise the probability for this entry
            double normalisedUtility = values[i] / total;
            if (normalisedUtility >= rnd) {
                return entries.get(i);
            }
        }
        return entries.get(entries.size() - 1);
    }

    /**
     * merges two lists and removes duplicates
     */
    private List<PeerDescriptor> merge(List<PeerDescriptor> l1, List<PeerDescriptor> l2) {

        l1.addAll(l2);
        l1 = queueBasedDeduplication(new LinkedList<PeerDescriptor>(l1));

        return l1;
    }

    /**
     * Performs list deduplication. Depending on the list size it executes a faster
     * version of the basic deduplication function
     * 
     * @param input - The list to be deduplicated
     * @return 
     */
    private List<PeerDescriptor> queueBasedDeduplication(List<PeerDescriptor> input) {

        int size = input.size();
        if (size % 10 != 0) {
            return this.slowDeduplication(input);
        }

        return this.FastDeduplication(input);
    }

    /**
     * inspects duplicate elements - PeerDescriptors in the input list and keeps
     * those that have the smallest queue size. 
     * 
     * @param input - The list to be deduplicated
     * @return - The deduplicated list
     */
    private List<PeerDescriptor> slowDeduplication(List<PeerDescriptor> input) {

        List<PeerDescriptor> copy = new LinkedList<PeerDescriptor>(input);
        Map<Address, PeerDescriptor> clear = new HashMap<Address, PeerDescriptor>();

        for (PeerDescriptor pd1 : copy) {

            if (!clear.containsKey(pd1.getAddress())) {
                clear.put(pd1.getAddress(), pd1);
            } else {
                PeerDescriptor test = clear.get(pd1.getAddress());
                test = (test.getResources().getQueueSize() > pd1.getResources().getQueueSize()) ? pd1 : test;
                clear.put(test.getAddress(), test);
            }
        }

        return new LinkedList<PeerDescriptor>(clear.values());
    }

    /**
     * inspects duplicate elements - PeerDescriptors in the input list and keeps
     * those that have the smallest queue size. Performs loop tiling for list
     * scanning performance
     *
     * @param input - The list to be deduplicated
     * @return - The deduplicated list
     */
    private List<PeerDescriptor> FastDeduplication(List<PeerDescriptor> input) {

        List<PeerDescriptor> copy = new LinkedList<PeerDescriptor>(input);
        Map<Address, PeerDescriptor> clear = new HashMap<Address, PeerDescriptor>();

        int size = copy.size();

        //some Loop Tiling for performance
        for (int i = 0; i < size; i += 5) {
            for (int j = i; j < i + 5; j++) {
                PeerDescriptor pd1 = copy.get(j);

                if (!clear.containsKey(pd1.getAddress())) {
                    clear.put(pd1.getAddress(), pd1);
                } else {
                    PeerDescriptor test = clear.get(pd1.getAddress());
                    test = (test.getResources().getQueueSize() > pd1.getResources().getQueueSize()) ? pd1 : test;
                    clear.put(test.getAddress(), test);
                }
            }
        }

        return new LinkedList<PeerDescriptor>(clear.values());
    }

    /**
     * selects the corresponding comparator depending on the current gradient
     * 
     * @param index - The current gradient
     * @param address - The peer, the peer-comparison is based on
     * @param resources - The peer's available resources
     * @return 
     */
    private CustomComparator getComparator(int index, Address address, AvailableResources resources) {

        switch (index) {
            case 1:
                return new ResourceComparator(address, resources);
            case 2:
                return new CpuComparator(address, resources);
            case 3:
                return new MemComparator(address, resources);
            case 100:
                return new QueueComparator();
        }

        return null;
    }

    private List<PeerDescriptor> getList(int gradientType) {

        switch (gradientType) {
            case 1:
                return this.tmanPartnersRes;
            case 2:
                return this.tmanPartnersCpu;
            case 3:
                return this.tmanPartnersMem;
        }

        return null;
    }

    private void setList(int gradientType, List<PeerDescriptor> list) {
        switch (gradientType) {
            case 1:
                this.tmanPartnersRes = list;
            case 2:
                this.tmanPartnersCpu = list;
            case 3:
                this.tmanPartnersMem = list;
        }
    }
}
