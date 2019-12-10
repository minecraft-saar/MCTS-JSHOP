package umd.cs.shop;

import java.util.Vector;


public class MCTSNode {
    private static int NEXT_ID = 0;
    private int id;

    private JSTState tState;
    private JSTasks taskNetwork;
    private int visited;
    private double reward;
    Vector<MCTSNode> children;
    boolean inTree  = false;
    JSPlan plan;
    boolean deadEnd = false;
    //private JSTaskAtom primitiveAction; // method that generated this state
    //boolean primitive = false;

    MCTSNode(JSTState state, JSTasks tasks, JSPlan plan) {
        this.tState = state;
        this.taskNetwork = tasks;
        this.visited = 0;
        this.reward = Double.NEGATIVE_INFINITY;
        this.plan = new JSPlan();
        this.plan.addElements(plan);
        this.children = new Vector<MCTSNode>();
        this.id = NEXT_ID++;
    }

    MCTSNode(JSTState state , JSPlan plan) {
        this.tState = state;
        this.plan = new JSPlan();
        this.plan.addElements(plan);
        this.reward = Double.NEGATIVE_INFINITY;
        this.visited = 0;
        this.id = NEXT_ID++;
    }

    JSTState tState(){
        return this.tState;
    }

    JSTasks taskNetwork(){
        return this.taskNetwork;
    }

    void incVisited(){
        this.visited += 1;
    }

    double reward(){
        return this.reward;
    }

    int visited(){
        return this.visited;
    }

    void setDeadEnd() {this.deadEnd = true;}

    void setReward(double r) {
        if(Double.isNaN(r)) {


            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                System.out.println(ste);
            }


            System.exit(0);
        }
        this.reward = r;
    }

    void addChild(MCTSNode ts){
        this.children.add(ts);
    }

    void setInTree(){
        this.inTree = true;
    }



    String dotTree() {
        String result = "digraph UCT {";
        result += dotNode();
        result += "}";
        return result;

    }

    String dotNode() {
        if(!this.inTree) return "";
        String result = this.id + " [label=\"" + this.reward + " " + this.visited + "\"];\n";
            for (MCTSNode child : children) {
                //while (child.children.size() == 1) {
                //    child = child.children.get(0);
                //}
                result += this.id + " -> " + child.id + ";\n";
                result += child.dotNode();
            }


        return result;
    }



}
