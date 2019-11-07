package umd.cs.shop;

import java.util.Random;
import java.util.Vector;

public class UCTPolicy implements MCTSPolicy {

    Random randgen = new Random(42);

    @Override
    public JSPairTStateTasks randomChild(JSPairTStateTasks parent) {
        //JSUtil.println("RANDOM");
        int rand = this.randgen.nextInt(parent.children.size());
        return parent.children.get(rand);
    }

    @Override
    public JSPairTStateTasks bestChild(JSPairTStateTasks parent) {
        Double maxValue = Double.NEGATIVE_INFINITY;
        JSPairTStateTasks bestChild = null;
        boolean allDeadEnd = true;
        //JSUtil.println(("Number of children to choose from: " + parent.children.size()));
        for (JSPairTStateTasks child : parent.children) {
            allDeadEnd = allDeadEnd && child.deadEnd;
            if (child.deadEnd) continue;
            if (child.visited() == 0) {
                /*
                JSUtil.println("NEW CHILD UCT");
                JSTaskAtom t = (JSTaskAtom) (child.taskNetwork().get(0));
                t.print();
                JSUtil.println("");*/
                return child;  //You can't divide by 0 and it is common practise to ensure every child gets expanded
            }
            //System.out.println("Possible rewards"+child.reward());
            Double childValue = child.reward() / child.visited();
            double exploration = (2.0 * 1.0 * java.lang.Math.log(parent.visited())) / child.visited();//1.0 is exploration factor
            exploration = java.lang.Math.sqrt(exploration);
            childValue = childValue + exploration;
            if (childValue.compareTo(maxValue) > 0) {
                //System.out.println("child-Value: " +childValue);
                //System.out.println("maxValue: " +maxValue);
                bestChild = child;
                maxValue = childValue;
            }
        }
        if (bestChild == null && !allDeadEnd) {
            System.out.println("NO CHILD SELECTED ");
            System.out.println(maxValue);
            for (JSPairTStateTasks child : parent.children) {
                System.out.println(child.deadEnd);
                System.out.println(child.reward());
            }
        }

        if (parent.children.size() == 0) {
            System.out.println("CHILDREN LIST EMPTY \n");
        }
        //System.out.println("Chosen reward: " + bestChild.reward());
        if (allDeadEnd){
            parent.setDeadEnd();
            parent.children = new Vector<>();
            return parent;
        }
        /*
        JSUtil.println("UCT");
        JSTaskAtom t = (JSTaskAtom) (bestChild.taskNetwork().get(0));
        t.print();
        JSUtil.println("");*/

        return bestChild;
    }

    @Override
    public void updateReward(JSPairTStateTasks parent, double reward) { //Average over rewards
        int numChildren = 0;
        double sum = 0.0;
        for (JSPairTStateTasks child : parent.children) {
            if (child.visited() > 0) {
                sum = sum + child.reward();
                numChildren++;
            }
        }
        double newReward = sum / numChildren;
        //System.out.println("In Tree: " + parent.inTree);
        //System.out.println("Updated Reward: " + newReward);
        parent.setReward(newReward);
    }

    @Override
    public void computeNewReward(JSPairTStateTasks child) {
        double cost = 0.0;
        for (int i = 0; i < child.plan.size(); i++) {
            cost = cost + child.plan.elementCost(i);
        }
        cost = cost * -1.0;
        //System.out.println("New Reward : "+ cost);
        child.setReward(cost);
    }
}
