package main;

import simulator.core.DataCenterSimulationMain;
import common.configuration.Configuration;
import common.simulation.scenarios.Scenario;
import common.simulation.scenarios.PeerFailScenario;

public class Main {

    public static void main(String[] args) throws Throwable {
        // TODO - change the random seed, have the user pass it in.
        long seed = System.currentTimeMillis();
        Configuration configuration = new Configuration(seed);

        //Scenario scenario = new NormalScenario();
        //Scenario scenario = new PeerChurnScenario();
        Scenario scenario = new PeerFailScenario();
        scenario.setSeed(seed);
        scenario.getScenario().simulate(DataCenterSimulationMain.class);
    }
}
