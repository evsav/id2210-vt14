
package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class BatchRequestScenario extends Scenario {

    private static SimulationScenario scenario = new SimulationScenario() {
        {

            SimulationScenario.StochasticProcess process0 = new SimulationScenario.StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(100, Operations.peerJoin(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(4), constant(6000)
                    );
                }
            };

            /**
             * A composite request. Apart from the requested resources it contains the number of
             * tasks as well.
             */
            System.out.println("Batch resource request");
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

            SimulationScenario.StochasticProcess terminateProcess = new SimulationScenario.StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(1, Operations.terminate);
                }
            };
            
            process0.start();
            process1.startAfterTerminationOf(500, process0);
            terminateProcess.startAfterTerminationOf(1000 * 1000, process1);
        }
    };

    // -------------------------------------------------------------------
    public BatchRequestScenario() {
        super(scenario);
    }
}
