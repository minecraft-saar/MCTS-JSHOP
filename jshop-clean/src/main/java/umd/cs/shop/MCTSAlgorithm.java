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
            //tst.plan.printPlan();
            this.vars.foundPlan(tst.plan, depth);
            return tst.getCost();
        }

        if (tst.isInTree()) {
            if (tst.isDeadEnd()) {
                JSUtil.println("Returned to dead end at depth: " + depth ); //t.print(); JSUtil.println("\n");
                tst.incVisited();
                return tst.getCost();
            }
            if (tst.children.size() == 0) {
                JSUtil.println("No children depth: " + depth + " SHOULD NOT HAVE HAPPENED");
                System.exit(0);
            }
            MCTSNode child = this.vars.policy.bestChild(tst);
            if (tst.isDeadEnd()) {
                return tst.getCost();
            }

            if (this.vars.bb_pruning(tst.plan.planCost())) {
                tst.setFullyExplored();
                return tst.getCost();
            }

            double reward = runMCTS(child, depth + 1);
            if(child.isFullyExplored()){
                tst.checkFullyExplored();
            }

            this.vars.policy.updateCostAndVisits(tst, reward);
            if (depth > vars.treeDepth) {
                vars.treeDepth = depth;
                long currentTime = System.currentTimeMillis();
                JSUtil.println("Increased tree depth to " + depth + " at run " + this.vars.mctsRuns + " after " + (currentTime - this.vars.startTime) + " ms");
            }

            return reward;
        }

        tst.expand(vars);
        tst.setInTree();

        if(tst.isDeadEnd()){
            return tst.getCost();
        }

        if (this.vars.bb_pruning(tst.plan.planCost())) {
            tst.setFullyExplored();
            return tst.getCost();
        }

        double result = this.vars.simulationPolicy.simulation(tst, depth);
        this.vars.policy.updateCostAndVisits(tst, result);



        return result;
    }

}
