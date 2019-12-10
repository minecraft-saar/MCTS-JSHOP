package umd.cs.shop;

import javax.naming.event.ObjectChangeListener;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

public class JSPairTStateTasks {
    private JSTState tState;
    private JSTasks taskNetwork;
    private int visited;
    private double reward;
    Vector<JSPairTStateTasks> children;
    boolean inTree  = false;
    JSPlan plan;
    boolean deadEnd = false;
    //private JSTaskAtom primitiveAction; // method that generated this state
    //boolean primitive = false;

    JSPairTStateTasks(JSTState state, JSTasks tasks, JSPlan plan) {
        this.tState = state;
        this.taskNetwork = tasks;
        this.visited = 0;
        this.reward = Double.NEGATIVE_INFINITY;
        this.plan = new JSPlan();
        this.plan.addElements(plan);
        this.children = new Vector<JSPairTStateTasks>();
    }

    JSPairTStateTasks(JSTState state , JSPlan plan) {
        this.tState = state;
        this.plan = new JSPlan();
        this.plan.addElements(plan);
        this.reward = Double.NEGATIVE_INFINITY;
        this.visited = 0;
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

    void addChild(JSPairTStateTasks ts){
        this.children.add(ts);
    }

    void setInTree(){
        this.inTree = true;
    }


}
