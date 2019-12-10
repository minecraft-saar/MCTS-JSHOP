package umd.cs.shop;

import java.util.Random;
import java.util.Vector;

public class UCTPolicy implements MCTSPolicy {

    Random randgen = new Random(21);

    @Override
    public MCTSNode randomChild(MCTSNode parent) {
        //JSUtil.println("RANDOM");
        int rand = this.randgen.nextInt(parent.children.size());
        return parent.children.get(rand);
    }

    @Override
    public MCTSNode bestChild(MCTSNode parent) {
        Double maxValue = Double.NEGATIVE_INFINITY;
        MCTSNode bestChild = null;
        boolean allDeadEnd = true;
        //JSUtil.println(("Number of children to choose from: " + parent.children.size()));
        for (MCTSNode child : parent.children) {
            allDeadEnd = allDeadEnd && child.deadEnd;
            if (child.deadEnd) continue;
            if (child.visited() == 0) {

                //JSUtil.println("NEW CHILD UCT");
                /*JSTaskAtom t = (JSTaskAtom) (child.taskNetwork().get(0));
                t.print();
                JSUtil.println("");*/
                return child;  //You can't divide by 0 and it is common practise to ensure every child gets expanded
            }
            //System.out.println("Possible rewards"+child.reward());
            Double childValue = child.reward() / child.visited();
            double exploration = (2.0 * 1.0 * java.lang.Math.log(parent.visited())) / child.visited();//1.0 is exploration factor
            exploration = java.lang.Math.sqrt(exploration);
            childValue = childValue + exploration;
            //JSUtil.println("Best value: " + maxValue + " child Values: " + child.reward());
            int comp = childValue.compareTo(maxValue);
            if (comp >= 0) {
                if(comp == 0) {
                    int rand = this.randgen.nextInt(2);
                    if (rand == 0 || bestChild == null) {
                        System.out.println("child-Value: " + childValue);
                        System.out.println("maxValue: " + maxValue);
                        bestChild = child;
                        maxValue = childValue;
                    }
                }else {
                        bestChild = child;
                        maxValue = childValue;
                    }

                    //if(rand == 0 || bestChild == null) {
                //}
            }
        }
        if (bestChild == null && !allDeadEnd) {
            System.out.println("NO CHILD SELECTED ");
            System.out.println(maxValue);
            for (MCTSNode child : parent.children) {
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
    public void updateReward(MCTSNode parent, double reward) { //Average over rewards
        double newReward;
        if(Double.isInfinite(parent.reward())){
            newReward = reward;
        } else if(JSJshopVars.updateMaximum){
            newReward = Math.max(parent.reward(), reward);
        } else {

            newReward= (reward + parent.reward()*parent.visited())/(parent.visited() + 1);

        }
        parent.setReward(newReward);
        /*
        int numChildren = 0;
        double sum = 0.0;
        for (JSPairTStateTasks child : parent.children) {
            if (child.visited() > 0) {
                sum = sum + child.reward();
                numChildren++;
            }
        }
        double newReward = sum / numChildren; */
        //System.out.println("In Tree: " + parent.inTree);
        //System.out.println("Updated Reward: " + newReward);
    }

    @Override
    public void computeNewReward(MCTSNode child) {
        double cost = 0.0;
        for (int i = 0; i < child.plan.size(); i++) {
            cost = cost + child.plan.elementCost(i);
        }
        cost = cost * -1.0;
        child.setReward(cost);
    }
}
