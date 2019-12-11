package umd.cs.shop;

import java.util.Vector;


public class MCTSNode {
    private static int NEXT_ID = 0;
    private int id;

    private JSTState tState;
    private JSTasks taskNetwork;
    private int visited;
    private int solvedVisits;
    private double cost;
    Vector<MCTSNode> children;
    private boolean inTree = false;
    JSPlan plan;
    private boolean deadEnd = false;

    private boolean fullyExplored = false;

    //private JSTaskAtom primitiveAction; // method that generated this state
    //boolean primitive = false;

    MCTSNode(JSTState state, JSTasks tasks, JSPlan plan) {
        this.tState = state;
        this.taskNetwork = tasks;
        this.visited = 0;
        this.solvedVisits = 0;
        this.cost = Double.POSITIVE_INFINITY;
        this.plan = new JSPlan();
        this.plan.addElements(plan);
        this.children = new Vector<MCTSNode>();
        this.id = NEXT_ID++;
    }

    MCTSNode(JSTState state, JSPlan plan) {
        this.tState = state;
        this.plan = new JSPlan();
        this.plan.addElements(plan);
        this.cost = Double.POSITIVE_INFINITY;
        this.visited = 0;
        this.solvedVisits = 0;
        this.id = NEXT_ID++;

    }

    JSTState tState() {
        return this.tState;
    }

    JSTasks taskNetwork() {
        return this.taskNetwork;
    }

    void incVisited() {
        this.visited += 1;
    }

    void incSolvedVisits() {
        this.solvedVisits += 1;
    }

    double getCost() {
        return this.cost;
    }

    int visited() {
        return this.visited;
    }

    int solvedVisits() {
        return this.solvedVisits;
    }

    /**Also sets fullyExplored**/
    void setDeadEnd() {
        this.deadEnd = true;
        this.fullyExplored = true;
        this.setCost(Double.POSITIVE_INFINITY);
    }

    void setCost(double r) {
        if (Double.isNaN(r)) {
            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                System.out.println(ste);
            }
            System.exit(0);
        }
        this.cost = r;
    }

    void setFullyExplored() {
        if (JSJshopVars.useFullyExplored)
            this.fullyExplored = true;
    }

    void addChild(MCTSNode ts) {
        this.children.add(ts);
    }

    void setInTree() {
        this.inTree = true;
    }

    boolean isDeadEnd() {
        return this.deadEnd;
    }


    boolean isInTree() {
        return this.inTree;
    }

    String dotTree() {
        String result = "digraph UCT {";
        result += dotNode();
        result += "}";
        return result;

    }

    String dotNode() {
        String color = "";
        String label = this.cost + " " + this.visited;
        if (!this.inTree) {
            label = "";
        }
        if (this.deadEnd) {
            color = " style=filled fillcolor=red";
        } else if (this.fullyExplored) {
            color = " style=filled fillcolor=green";
        } else if (!this.inTree) {
            color = " style=filled fillcolor=grey";
        }

        String result = this.id + " [label=\"" + label + "\"" + color + "];\n";
        for (MCTSNode child : children) {
            /*while (child.children.size() == 1 && child.children.get(0).inTree) {
                child = child.children.get(0);
            }*/

           // if (child.inTree) {
                result += this.id + " -> " + child.id + ";\n";
                result += child.dotNode();
            //}
        }

        return result;
    }

    public boolean isFullyExplored() {
        return fullyExplored;
    }


    public void checkFullyExplored() {
        this.fullyExplored = true;
        this.deadEnd = true;
        for (MCTSNode c : children) {
            if (!c.fullyExplored) {
                this.fullyExplored = false;
                this.deadEnd = false;
                return;
            }
            if (!c.deadEnd) {
                this.deadEnd = false;
            }
        }
    }


    public void setGoal() {

        JSJshopVars.policy.computeCost(this); // sets cost
        this.setFullyExplored();
        if(this.isInTree()) {
            this.incSolvedVisits();
            this.incVisited();
        }
    }

}
