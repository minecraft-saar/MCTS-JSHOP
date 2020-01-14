package umd.cs.shop;

public class MCTSSimulationExpand implements MCTSSimulation {

    int budget_recursive;

    MCTSSimulationExpand(int budget_recursive) {
        this.budget_recursive = budget_recursive;
    }

    @Override
    public double simulation(MCTSNode current, int depth ) {
        return this.simulation(current, depth, budget_recursive);
    }

    public double simulation(MCTSNode current, int depth, Integer budget) {
        if (current.taskNetwork().isEmpty()) {
            current.setGoal();
            JSJshopVars.FoundPlan(current.plan, depth);
            return current.getCost();
        }

        current.expand();
        if (current.isDeadEnd()) {
            return Double.POSITIVE_INFINITY;
        }

        //JSUtil.println("Size of child list : " + current.children.size());
        MCTSNode child = JSJshopVars.policy.randomChild(current);
        double cost = this.simulation(child, depth+1);
        if(child.isFullyExplored()) {
            current.checkFullyExplored();
            if (budget > 0 && child.isDeadEnd() && !current.isFullyExplored()) {
                budget--;
                return simulation(current, depth, budget);
            }
        }

        child.setCost(cost);
        return cost;
    }
}
