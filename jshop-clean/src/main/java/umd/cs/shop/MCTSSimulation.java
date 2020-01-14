package umd.cs.shop;

public interface MCTSSimulation {
    public double simulation(MCTSNode node, int depth);


    public static MCTSSimulation getPolicy(boolean expand, int  budget_recursive) {
        if (expand) {
            return new MCTSSimulationExpand(budget_recursive);
        } else {
            return new MCTSSimulationFast(budget_recursive);
        }
    }

}
