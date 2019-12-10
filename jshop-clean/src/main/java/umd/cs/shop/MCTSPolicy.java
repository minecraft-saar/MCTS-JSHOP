package umd.cs.shop;

import umd.cs.shop.costs.BasicCost;
import umd.cs.shop.costs.CostFunction;
import umd.cs.shop.costs.StateDependentCostMinecraft;
import umd.cs.shop.costs.UnitCost;

public interface MCTSPolicy {

    MCTSNode randomChild(MCTSNode parent);

    MCTSNode bestChild(MCTSNode parent);

    void updateCostAndVisits(MCTSNode parent, double reward);

    void computeCost(MCTSNode child);

    public static MCTSPolicy getPolicy(String policy) {
        switch (policy) {
            case "uct1":
                return new UCTVersion1();
            case "uct2":
                return new UCTVersion2();
            default:
                System.err.println("Unknown policy name: " + policy);
                System.exit(-1);
        }
        return null;
    }

}
