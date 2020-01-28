package umd.cs.shop;

public interface MCTSSimulation {
    public double simulation(MCTSNode node, int depth);


    public static MCTSSimulation getPolicy(boolean expand, int  budget_recursive, boolean bbPruning, boolean bbPruningFast, JSJshopVars vars) {
        if (expand) {
            return new MCTSSimulationExpand(budget_recursive, bbPruning, bbPruningFast, vars);
        } else {
            return new MCTSSimulationFast(budget_recursive, vars);
        }
    }

}
