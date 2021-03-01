package umd.cs.shop;

public interface MCTSPolicy {

    MCTSNode randomChild(MCTSNode parent);

    MCTSNode bestChild(MCTSNode parent);

    void updateCostAndVisits(MCTSNode parent, double reward);

    default void recordDeadEndCost(MCTSNode parent, double reward){
        return;
    }

    void computeCost(MCTSNode child);

    public static MCTSPolicy getPolicy(JSJshopVars vars, boolean min, double explor) {
        //if ("uct1".equals(policy)) {
        if(vars.landmarks){
            return new LandmarksPolicy(vars, min, explor);
        } else
            return new UCTPolicy(vars, min, explor);
            //return new LandmarksPolicy(vars, min, explor);
            //case "uct2":
            //    return new UCTVersion2();
        //}
        /*
        System.err.println("Unknown policy name: " + policy);
        System.err.println("Options are: uct1 uct2");
        System.exit(-1);
        return null; */
    }

}
