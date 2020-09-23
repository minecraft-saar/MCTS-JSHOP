package umd.cs.shop;

public class UCTVersion2{// implements UCTPolicy {

    //take negative cost as reward and use best cost as exploration factor
    public double computeChildValue(MCTSNode parent, MCTSNode child, JSJshopVars vars) {
        double exploration = (java.lang.Math.log(parent.visited())) / child.visited();
        exploration = java.lang.Math.sqrt(exploration);
        double factor;
        if (vars.planFound) {
            factor = vars.bestCost;
        } else {
            factor = java.lang.Math.sqrt(2);
        }
        double reward = 0 - child.getCost(vars);
        double childValue = reward + factor * exploration; //middle part is exploration factor
        return childValue;
    }
}
