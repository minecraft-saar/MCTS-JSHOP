package umd.cs.shop;

import java.util.Vector;


public class MCTSNode {
    private static int NEXT_ID = 0;
    private int id;

    private JSTState tState;
    private JSTasks taskNetwork;
    private int visited;
    private double cost;
    Vector<MCTSNode> children;
    boolean inTree = false;
    JSPlan plan;
    boolean deadEnd = false;

    boolean fullyExplored = false;

    //private JSTaskAtom primitiveAction; // method that generated this state
    //boolean primitive = false;

    MCTSNode(JSTState state, JSTasks tasks, JSPlan plan) {
        this.tState = state;
        this.taskNetwork = tasks;
        this.visited = 0;
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

    double getCost() {
        return this.cost;
    }

    int visited() {
        return this.visited;
    }

    void setDeadEnd() {
        this.deadEnd = true;
        this.fullyExplored = true;
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

    void addChild(MCTSNode ts) {
        this.children.add(ts);
    }

    void setInTree() {
        this.inTree = true;
    }


    String dotTree() {
        String result = "digraph UCT {";
        result += dotNode();
        result += "}";
        return result;

    }

    String dotNode() {
        if (!this.inTree) return "";
        String result = this.id + " [label=\"" + this.cost + " " + this.visited + "\"];\n";
        for (MCTSNode child : children) {
            //while (child.children.size() == 1) {
            //    child = child.children.get(0);
            //}
            result += this.id + " -> " + child.id + ";\n";
            result += child.dotNode();
        }

        return result;
    }

    public boolean isFullyExplored() {
        return fullyExplored;
    }


    public void checkFullyExplored() {
        this.fullyExplored = true;
        for (MCTSNode c : children) {
            if(!c.fullyExplored) {
                this.fullyExplored = false;
                return;
            }
        }
    }



}
