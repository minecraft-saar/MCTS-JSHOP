package umd.cs.shop;

import java.util.Vector;

public class MCTSAlgorithm {

    public static double runMCTS(MCTSNode tst, JSPlanningDomain dom, int depth) {
        if (tst.isFullyExplored()) {
            System.err.println("Error: we are coming back to a fully explored node");
            System.exit(-1);
        }
        if (tst.taskNetwork().isEmpty()) {
            tst.setGoal();
            //Get current best reward if it exists
            Double currentCost = Double.POSITIVE_INFINITY;
            if (JSJshopVars.planFound) {
                currentCost = JSJshopVars.bestPlans.lastElement().getCost();
            } else {
                long currentTime = System.currentTimeMillis();
                JSUtil.println("Found first plan of reward " + tst.getCost() + " in run " + dom.mctsRuns + " after " + (currentTime - JSJshopVars.startTime) + " ms at depth " + depth);
            }

            Double foundCost = tst.getCost();
            if (foundCost.compareTo(currentCost) < 0) {
                JSJshopVars.bestPlans.addElement(tst);
                JSJshopVars.bestCost = foundCost;
                if (JSJshopVars.planFound) {
                    long currentTime = System.currentTimeMillis();
                    JSUtil.println("Found better plan of reward " + tst.getCost() + " in run " + dom.mctsRuns + " after " + (currentTime - JSJshopVars.startTime) + " ms at depth " + depth);
                }
            }
            JSJshopVars.FoundPlan();
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
            double reward = runMCTS(child, dom, depth + 1);
            if(child.isFullyExplored()){
                tst.checkFullyExplored();
            }
            if (!child.isInTree()) {
                //JSUtil.println("Adding new node to tree in run " + dom.mctsRuns + "at depth " + depth);
                child.setInTree();
                JSJshopVars.policy.updateCostAndVisits(child, reward);
            }
            JSJshopVars.policy.updateCostAndVisits(tst, reward);
            if (depth > JSJshopVars.treeDepth) {
                JSJshopVars.treeDepth = depth;
                long currentTime = System.currentTimeMillis();
                JSUtil.println("Increased tree depth to " + depth + " at run " + dom.mctsRuns + " after " + (currentTime - JSJshopVars.startTime) + " ms");
            }
            return tst.getCost();
        }

        Vector<MCTSNode> children = JSJshopVars.expansionPolicy.expand(tst, dom);
        tst.addChildren(children);
        tst.checkFullyExplored();
        if(tst.children.isEmpty()){
            tst.setDeadEnd();
        }
        if(tst.isDeadEnd()){
            return tst.getCost();
        }

        //JSUtil.println("Size of child list : " + tst.children.size());
        MCTSNode child = JSJshopVars.policy.randomChild(tst);
        double cost = runMCTS(child, dom, depth + 1);
        if(child.isFullyExplored()){
            tst.checkFullyExplored();
        }
        child.setCost(cost);
        return cost;

    }

}
