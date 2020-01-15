package umd.cs.shop;

public class MCTSSimulationExpand implements MCTSSimulation {

    int budget_recursive;

    MCTSSimulationExpand(int budget_recursive) {
        this.budget_recursive = budget_recursive;
    }

    @Override
    public double simulation(MCTSNode current, int depth ) {
        return simulation_rec_expanded(current, depth, this.budget_recursive);
    }

    public double simulation_rec_expanded(MCTSNode current, int depth, int budget) {
        MCTSNode child = JSJshopVars.policy.randomChild(current);
        double result = this.simulation_rec(child, depth +1, budget);
        current.checkFullyExplored();

        while (child.isDeadEnd() && !current.isFullyExplored() && budget > 0) {
            budget--;
            child = JSJshopVars.policy.randomChild(current);
            result = this.simulation_rec(child, depth +1, budget);
            current.checkFullyExplored();
        }
        return result;
    }

    public double simulation_rec(MCTSNode current, int depth, int budget) {
        if (current.taskNetwork().isEmpty()) {
            current.setGoal();
            JSJshopVars.FoundPlan(current.plan, depth);
            return current.getCost();
        }

        current.expand();
        if (current.isDeadEnd()) {
            return current.getCost();
        }

        return simulation_rec_expanded(current, depth, this.budget_recursive);
    }
}


/*
    public double simulation_rec(MCTSNode current, int depth, Integer budget) {
        if (current.taskNetwork().isEmpty()) {
            current.setGoal();
            JSJshopVars.FoundPlan(current.plan, depth);
            return current.getCost();
        }

        if (current.isFullyExplored()) {
            return current.getCost();
        }

        //JSUtil.println("Size of child list : " + current.children.size());
        MCTSNode child = JSJshopVars.policy.randomChild(current);
        child.expand();
        double cost = this.simulation_rec(child, depth + 1, budget);
        current.checkFullyExplored();

        while (child.isDeadEnd() && !current.isFullyExplored() && budget > 0) {
            child = JSJshopVars.policy.randomChild(current);
            child.expand();
            budget --;
            cost = this.simulation_rec(child, depth + 1, budget);
            current.checkFullyExplored();
        }

        current.setCost(cost);
        return cost;
    }

    /*
 */