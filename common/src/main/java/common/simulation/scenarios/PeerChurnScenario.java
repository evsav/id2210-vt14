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

    /**
     * defines a scenario, where peer churn is introduced in the system There is
     * no peer churn (or failures) in this scenario
     */
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

            /**
             * raising 5000 jobs, requesting either a combination of resources,
             * or a single resource based on the user selection. The
             * interarrival time is set to 300ms, in order to avoid job requests
             * timing out. Each job has duration of 1 minute
             */
            SimulationScenario.StochasticProcess process1 = new SimulationScenario.StochasticProcess() {
                {
                    eventInterArrivalTime(constant(300));
                    raise(5000, Operations.requestResources(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(2), 
                            constant(2000),
                            constant(1000 * 60),
                            constant(1)//gradient type, 1 combined resources, 2 cpu, 3 memory
                    );
                }
            };

            /**
             * peer churn. 70 peers join the system, and 40 fail
             */
            StochasticProcess churnProcess = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(500));
                    raise(70, Operations.peerJoin(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(8), constant(12000)
                    );
                    raise(40, Operations.peerFail,
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
            //introduce churn in the system
            churnProcess.startAfterTerminationOf(10 * 1000, process0);
            terminateProcess.startAfterTerminationOf(100 * 1000, process1);
        }
    };
}
