/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common.simulation.scenarios;

import java.util.Random;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class BatchRequestScenario extends Scenario {

    private static SimulationScenario scenario = new SimulationScenario() {
        {

            final Random random = new Random();

            SimulationScenario.StochasticProcess process0 = new SimulationScenario.StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(100, Operations.peerJoin(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(4), constant(6000)
                    );
                }
            };

            System.out.println("Batch resource request");
            SimulationScenario.StochasticProcess process1 = new SimulationScenario.StochasticProcess() {
                {
                    eventInterArrivalTime(constant(300));
                    raise(5000, Operations.batchRequest(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(2), constant(2000), constant(2),
                            constant(1000 * 60)
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
            process1.startAfterTerminationOf(2000, process0);
            terminateProcess.startAfterTerminationOf(100 * 1000, process1);
        }
    };

    // -------------------------------------------------------------------
    public BatchRequestScenario() {
        super(scenario);
    }
}
