package umd.cs.shop;

public class MCTSSimulationExpand implements MCTSSimulation {

    int budget_recursive;

    int available_budget;
    boolean bbPruning;
    boolean bbPruningFast;
    JSJshopVars vars;

    MCTSSimulationExpand(int budget_recursive, boolean bbPruning, boolean bbPruningFast, JSJshopVars vars) {
        this.budget_recursive = budget_recursive;
        this.bbPruning = bbPruning || bbPruningFast;
        this.bbPruningFast = bbPruningFast;
        this.vars = vars;
    }

    @Override
    public double simulation(MCTSNode current, int depth ) {
        this.available_budget = this.budget_recursive;
        return simulation_rec_expanded(current, depth);
    }

    public double simulation_rec_expanded(MCTSNode current, int depth) {
        MCTSNode child = vars.policy.randomChild(current);
        double result = this.simulation_rec(child, depth +1);

        current.checkFullyExplored(vars);

        while (child.isDeadEnd() && !current.isFullyExplored() && this.available_budget > 0) {
            if(vars.landmarks){
                vars.policy.recordDeadEndCost(current, result);
            }
            this.available_budget --;
            child = vars.policy.randomChild(current);
            result = this.simulation_rec(child, depth +1);
            current.checkFullyExplored(vars);
        }
        current.setCost(result);
        return result;
    }



    public double simulation_rec(MCTSNode current, int depth) {
        if (current.taskNetwork().isEmpty()) {
            current.setGoal(vars);
            if(vars.landmarks) {
                //JSUtil.println("Task Landmark cost at Goal: " + current.tState().state().taskLandmarks.size() + " Fact landmark cost: " + current.tState().state().factLandmarks.size());
                //JSUtil.println(current.tState().state.factLandmarks.toString());
            }
            vars.foundPlan(current.plan, depth);
            return current.getCost();
        }

        if (vars.bb_pruning(current.plan.planCost())) {
            double cost = current.getCost();
            if (this.bbPruningFast) {
                MCTSSimulationFast simulation_fast = new MCTSSimulationFast(this.available_budget, cost);
                cost = simulation_fast.simulation(current, depth);
            }
            current.setFullyExplored();
            return cost;
        }

        current.expand(vars);
        if (current.isDeadEnd()) {
            return current.getCost();
        }

        return simulation_rec_expanded(current, depth);
    }
}
