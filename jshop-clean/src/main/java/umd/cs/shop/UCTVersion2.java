package umd.cs.shop;

public class UCTVersion2 implements UCTPolicy {

    //take negative cost as reward and use best cost as exploration factor
    @Override
    public double computeChildValue(MCTSNode parent, MCTSNode child) {
        double exploration = (java.lang.Math.log(parent.visited())) / child.visited();
        exploration = java.lang.Math.sqrt(exploration);
        double factor;
        if (JSJshopVars.planFound) {
            factor = JSJshopVars.bestCost;
        } else {
            factor = java.lang.Math.sqrt(2);
        }
        double reward = 0 - child.getCost();
        double childValue = reward + factor * exploration; //middle part is exploration factor
        return childValue;
    }
}
