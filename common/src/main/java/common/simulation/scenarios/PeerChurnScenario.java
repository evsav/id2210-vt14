/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

public class PeerChurnScenario extends Scenario {

    public PeerChurnScenario() {
        super(scenario);
    }

    private static SimulationScenario scenario = new SimulationScenario() {
        {

            SimulationScenario.StochasticProcess process0 = new SimulationScenario.StochasticProcess() {
                {
                    eventInterArrivalTime(constant(1000));
                    raise(100, Operations.peerJoin(), //add 100 peers in the system
                            uniform(0, Integer.MAX_VALUE),
                            constant(4), constant(6000)
                    );
                }
            };

            /**
             * A composite request containing the number of tasks
             */
            SimulationScenario.StochasticProcess process1 = new SimulationScenario.StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(5000, Operations.batchRequest(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(2), //the number of requested cpus
                            constant(2000), //the amount of requested memory
                            constant(2), //the number of tasks
                            constant(1000 * 60) //total duration of the job (1 minute)
                    );
                }
            };

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
            process1.startAfterTerminationOf(2000, process0);
            //introduce peer churn in the system after 10 seconds
            churnProcess.startAfterTerminationOf(10 * 1000, process0);
            terminateProcess.startAfterTerminationOf(100 * 1000, process1);
        }
    };
}
