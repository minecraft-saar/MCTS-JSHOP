package umd.cs.shop;

public class MCTSSimulationExpand implements MCTSSimulation {

    int budget_recursive;

    int available_budget;

    MCTSSimulationExpand(int budget_recursive) {
        this.budget_recursive = budget_recursive;
    }

    @Override
    public double simulation(MCTSNode current, int depth ) {
        this.available_budget = this.budget_recursive;
        return simulation_rec_expanded(current, depth);
    }

    public double simulation_rec_expanded(MCTSNode current, int depth) {
        MCTSNode child = JSJshopVars.policy.randomChild(current);
        double result = this.simulation_rec(child, depth +1);

        current.checkFullyExplored();

        while (child.isDeadEnd() && !current.isFullyExplored() && this.available_budget > 0) {
            this.available_budget --;
            child = JSJshopVars.policy.randomChild(current);
            result = this.simulation_rec(child, depth +1);
            current.checkFullyExplored();
        }
        current.setCost(result);
        return result;
    }

    public double simulation_rec(MCTSNode current, int depth) {
        if (current.taskNetwork().isEmpty()) {
            current.setGoal();
            JSJshopVars.FoundPlan(current.plan, depth);
            return current.getCost();
        }

        current.expand();
        if (current.isDeadEnd()) {
            return current.getCost();
        }

        return simulation_rec_expanded(current, depth);
    }
}
