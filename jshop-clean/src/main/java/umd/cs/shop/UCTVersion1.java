package umd.cs.shop;

public class UCTVersion1{ //implements UCTPolicy{

    // compute value by dividing bestCost with current child cost

    double explorationFactor = java.lang.Math.sqrt(2);

    public double computeChildValue(MCTSNode parent, MCTSNode child, JSJshopVars vars) {
        double exploration = (java.lang.Math.log(parent.visited())) / child.visited();
        exploration = java.lang.Math.sqrt(exploration);
        double reward;
        if(vars.planFound){
            reward = vars.bestCost/child.getCost(vars);
        } else {
            reward = 0.5;
        }

        double childValue = reward + this.explorationFactor * exploration; //middle part is exploration factor
        return childValue;
    }
}
