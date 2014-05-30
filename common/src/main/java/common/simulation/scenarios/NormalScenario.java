package common.simulation.scenarios;

import java.util.Random;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class NormalScenario extends Scenario {

    /**
     * defines a normal scenario, where peers are created and then receive requests
     * for resources allocation. There is no peer churn (or failures) in this scenario
     */
    private static SimulationScenario scenario = new SimulationScenario() {
        {
            
            StochasticProcess process0 = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(1000));
                    raise(100, Operations.peerJoin(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(8), //cpu num
                            constant(12000) //memory amount
                    );
                }
            };
            
            /**
             * raising 5000 jobs, requesting either a combination of resources, or a single 
             * resource based on the user selection. The interarrival time is set to 300ms,
             * in order to avoid job requests timing out. Each job has duration of 1 minute
             */
            StochasticProcess process1 = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(5000, Operations.requestResources(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(2), 
                            constant(2000), 
                            constant(1000 * 60),
                            constant(1) //gradient type, 1 resources combined, 2 cpu, 3 memory
                    );
                }
            };

            StochasticProcess terminateProcess = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(1, Operations.terminate);
                }
            };
            
            process0.start();
            process1.startAfterTerminationOf(500, process0);
            terminateProcess.startAfterTerminationOf(100 * 1000, process1);
        }
    };

    // -------------------------------------------------------------------
    public NormalScenario() {
        super(scenario);
    }
}
