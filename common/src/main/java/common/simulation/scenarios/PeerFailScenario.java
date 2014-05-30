
package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;


public class PeerFailScenario extends Scenario {
    
    public PeerFailScenario() {
        super(scenario);
    }

    /**
     * this scenario introduces peer failures in the system. No new nodes join the system
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

            SimulationScenario.StochasticProcess process1 = new SimulationScenario.StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(5000, Operations.requestResources(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(2), constant(2000),
                            constant(1000 * 60),
                            constant(1) //gradient type, 1 resources combined, 2 cpu, 3 memory
                    );
                }
            };

            StochasticProcess failPeersProcess = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
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
            process1.startAfterTerminationOf(500, process0);
            //introduce peer failure in the system after 10 seconds
            failPeersProcess.startAfterTerminationOf(10 * 1000, process0);
            terminateProcess.startAfterTerminationOf(1000 * 1000, process1);
        }
    };
}
