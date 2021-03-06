package common.simulation.scenarios;

import java.util.Random;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class Scenario1 extends Scenario {

    private static SimulationScenario scenario = new SimulationScenario() {
        {

            final Random r = new Random();
            
            StochasticProcess process0 = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(1000));
                    raise(5, Operations.peerJoin(),
                            uniform(0, r.nextInt(500)),
                            constant(8), //cpu num
                            constant(12000) //memory amount
                    );
                }
            };

            StochasticProcess process1 = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(10, Operations.requestResources(),
                            uniform(0, Integer.MAX_VALUE),
                            constant(2), //cpu num
                            constant(2000), //memory amount
                            //constant(1 + r.nextInt(300)) // 100ms
                            constant(500)
                    );
                }
            };

            // TODO - not used yet
            StochasticProcess failPeersProcess = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(1, Operations.peerFail,
                            uniform(0, Integer.MAX_VALUE));
                }
            };

            StochasticProcess terminateProcess = new StochasticProcess() {
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
    public Scenario1() {
        super(scenario);
    }
}
