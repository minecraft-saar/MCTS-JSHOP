package umd.cs.shop;

import java.util.Vector;

public class MCTSAlgorithm {

    public static double runMCTS(MCTSNode tst, int depth) {
        if (tst.isFullyExplored()) {
            System.err.println("Error: we are coming back to a fully explored node");
            System.exit(-1);
        }
        if (tst.taskNetwork().isEmpty()) {
            tst.setGoal();
            JSJshopVars.FoundPlan(tst.plan, depth);
            return tst.getCost();
        }

        if (tst.isInTree()) {
            if (tst.isDeadEnd()) {
                System.out.println("Returned to dead end at depth: " + depth ); //t.print(); JSUtil.println("\n");
                tst.incVisited();
                return tst.getCost();
            }
            if (tst.children.size() == 0) {
                JSUtil.println("No children depth: " + depth + " SHOULD NOT HAVE HAPPENED");
                System.exit(0);
            }
            MCTSNode child = JSJshopVars.policy.bestChild(tst);
            if (tst.isDeadEnd()) {
                return tst.getCost();
            }
            double reward = runMCTS(child, depth + 1);
            if(child.isFullyExplored()){
                tst.checkFullyExplored();
            }
            JSJshopVars.policy.updateCostAndVisits(tst, reward);
            if (depth > JSJshopVars.treeDepth) {
                JSJshopVars.treeDepth = depth;
                long currentTime = System.currentTimeMillis();
                JSUtil.println("Increased tree depth to " + depth + " at run " + JSJshopVars.mctsRuns + " after " + (currentTime - JSJshopVars.startTime) + " ms");
            }
            return tst.getCost();
        }

        tst.expand();
        tst.setInTree();

        if(tst.isDeadEnd()){
            return tst.getCost();
        }

        double result = JSJshopVars.simulationPolicy.simulation(tst, depth);
        JSJshopVars.policy.updateCostAndVisits(tst, result);

        return result;
    }

}
