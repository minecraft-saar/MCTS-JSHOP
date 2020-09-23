package umd.cs.shop;

public class MCTSAlgorithm {

    JSJshopVars vars;

    MCTSAlgorithm(JSJshopVars vars){
        this.vars = vars;
    }

    public double runMCTS(MCTSNode tst, int depth) {
        if (tst.isFullyExplored()) {
            System.err.println("Error: we are coming back to a fully explored node");
            System.exit(-1);
        }
        if (tst.taskNetwork().isEmpty()) {
            tst.setGoal(vars);
            JSUtil.println("Task Landmark cost at Goal: " + tst.tState().state().taskLandmarks.size() + " Fact landmark cost: " + tst.tState().state().factLandmarks.size());
            //tst.plan.printPlan();
            this.vars.foundPlan(tst.plan, depth);
            return tst.getCost(vars);
        }

        if (tst.isInTree()) {
            if (tst.isDeadEnd()) {
                JSUtil.println("Returned to dead end at depth: " + depth ); //t.print(); JSUtil.println("\n");
                tst.incVisited();
                return tst.getCost(vars);
            }
            if (tst.children.size() == 0) {
                JSUtil.println("No children depth: " + depth + " SHOULD NOT HAVE HAPPENED");
                System.exit(0);
            }
            MCTSNode child = this.vars.policy.bestChild(tst);
            if (tst.isDeadEnd()) {
                //JSUtil.println("In MCTSAlgorithm DeadEnd with cost: " + tst.getCost(vars));
                return tst.getCost(vars);
            }

            if (this.vars.bb_pruning(tst.plan.planCost())) {
                tst.setFullyExplored();
                return tst.getCost(vars);
            }

            double reward = runMCTS(child, depth + 1);
            if(child.isFullyExplored()){
                tst.checkFullyExplored();
            }

            this.vars.policy.updateCostAndVisits(tst, reward);
            if (depth > vars.treeDepth) {
                vars.treeDepth = depth;
                long currentTime = System.currentTimeMillis();
                if(vars.print)
                    JSUtil.println("Increased tree depth to " + depth + " at run " + this.vars.mctsRuns + " after " + (currentTime - this.vars.startTime) + " ms");
            }

            return reward;
        }

        tst.expand(vars);
        tst.setInTree();

        if(tst.isDeadEnd()){
            //JSUtil.println("In MCTSAlgorithm DeadEnd with cost: " + tst.getCost(vars));
            return tst.getCost(vars);
        }

        if (this.vars.bb_pruning(tst.plan.planCost())) {
            tst.setFullyExplored();
            return tst.getCost(vars);
        }

        double result = this.vars.simulationPolicy.simulation(tst, depth);
        this.vars.policy.updateCostAndVisits(tst, result);



        return result;
    }

}
