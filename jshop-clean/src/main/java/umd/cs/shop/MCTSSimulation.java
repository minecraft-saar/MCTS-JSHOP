package umd.cs.shop.costs;

import umd.cs.shop.*;

public interface MCTSSimulation {
    public double simulation(MCTSNode node, int depth);


    public static MCTSSimulation getPolicy(boolean expand, boolean recursive) {
        return new MCTSSimulationExpand();
    }

}
