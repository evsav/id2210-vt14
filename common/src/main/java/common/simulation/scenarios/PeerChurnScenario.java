/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class PeerChurnScenario extends Scenario {

    public PeerChurnScenario() {
        super(scenario);
    }

    private static SimulationScenario scenario = new SimulationScenario() {
        {

            SimulationScenario.StochasticProcess process0 = new SimulationScenario.StochasticProcess() {
                {
                    eventInterArrivalTime(constant(1000));
                    raise(100, Operations.peerJoin(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(8), constant(12000)
                    );
                }
            };

            SimulationScenario.StochasticProcess process1 = new SimulationScenario.StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(5000, Operations.requestResources(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(2), //number of requested cpus
                            constant(2000), //amount of requested memory
                            constant(1000 * 60) //job duration (1 minute)
                    );
                }
            };

            /**
             * this process simulates peer churn. It adds 70 peers in the system and then kills 30
             */
            StochasticProcess churnProcess = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(500));
                    raise(70, Operations.peerJoin(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(8), constant(12000)
                    );
                    raise(30, Operations.peerFail,
                            uniform(0, Integer.MAX_VALUE));
                }
            };

            SimulationScenario.StochasticProcess terminateProcess = new SimulationScenario.StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(1, Operations.terminate);
                }
            };

            process0.start();
            process1.startAfterTerminationOf(500, process0);
            //introduce churn in the system after 10 seconds
            churnProcess.startAfterTerminationOf(10 * 1000, process0);
            terminateProcess.startAfterTerminationOf(1000 * 1000, process1);
        }
    };
}
