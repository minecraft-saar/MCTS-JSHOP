package umd.cs.shop;

import java.util.Random;
import java.util.Vector;

public interface UCTPolicy extends MCTSPolicy {

    static final Random randgen = new Random(42);

    @Override
    public default MCTSNode randomChild(MCTSNode parent) {
        if (parent.isFullyExplored()) {
            System.err.println("Error: randomly selecting children of fully explored node");
            System.exit(-1);
        }
        int rand = this.randgen.nextInt(parent.children.size());
        while (parent.children.get(rand).isFullyExplored()) {
            rand = this.randgen.nextInt(parent.children.size());
        }
        return parent.children.get(rand);
    }

    @Override
    public default MCTSNode bestChild(MCTSNode parent) {
        if (parent.isFullyExplored()) {
            System.err.println("Error: randomly selecting children of fully explored node");
            System.exit(-1);
        }
        Double maxValue = Double.NEGATIVE_INFINITY;
        MCTSNode bestChild = null;
        //boolean allDeadEnd = true;

        for (MCTSNode child : parent.children) {
            if (child.isFullyExplored()) {
                //JSUtil.println("Child is dead end: " + child.isDeadEnd());
                continue;
            }
            //allDeadEnd = allDeadEnd && child.deadEnd;
            //if (child.deadEnd) continue;
            if (child.visited() == 0) {
                return child;  //You can't divide by 0 and it is common practise to ensure every child gets expanded
            }

            Double childValue = computeChildValue(parent, child);
            //JSUtil.println("CHILD VALUE: " + childValue + "MAXVALUE: " + maxValue);
            if (childValue.compareTo(maxValue) > 0) {
                bestChild = child;
                maxValue = childValue;
            }
        }

        if (bestChild == null /*&& !allDeadEnd*/) {
            JSUtil.println("NO CHILD SELECTED");
            for (MCTSNode child : parent.children) {
                JSUtil.println("This child  has cost: " + child.getCost() + " is dead-end: " + child.isDeadEnd() + " is fully explored: " + child.isFullyExplored());
            }
            System.exit(0);
        }
        /*if (allDeadEnd) {
            parent.setDeadEnd();
            parent.children = new Vector<>();
            return parent;
        }*/
        return bestChild;
    }

    public double computeChildValue(MCTSNode parent, MCTSNode child);


    @Override
    public default void updateCostAndVisits(MCTSNode node, double cost) {
        double newCost;

        if (!Double.isInfinite(cost)) {
            if (node.solvedVisits() == 0) {
                newCost = cost;
            } else if (JSJshopVars.updateMaximum) {
                newCost = Math.min(node.getCost(), cost);
            } else {
                newCost = (cost + node.getCost() * (node.solvedVisits())) / (node.solvedVisits() + 1);
            }
            node.incSolvedVisits();
            node.setCost(newCost);
        }
        node.incVisited();
    }

    @Override
    public default void computeCost(MCTSNode node) {
        double cost = 0.0;
        for (int i = 0; i < node.plan.size(); i++) {
            cost = cost + node.plan.elementCost(i);
        }
        node.setCost(cost);
    }
}
