package umd.cs.shop;

public class UCTVersion1 implements UCTPolicy{

    // compute value by dividing bestCost with current child cost

    @Override
    public double computeChildValue(MCTSNode parent, MCTSNode child) {
        double exploration = (java.lang.Math.log(parent.visited())) / child.visited();
        exploration = java.lang.Math.sqrt(exploration);
        double reward;
        if(JSJshopVars.planFound){
            reward = JSJshopVars.bestCost/child.getCost();
        }
        else {
            reward = 0.5;
        }

        double childValue = reward + java.lang.Math.sqrt(2) * exploration; //middle part is exploration factor
        return childValue;
    }
}
