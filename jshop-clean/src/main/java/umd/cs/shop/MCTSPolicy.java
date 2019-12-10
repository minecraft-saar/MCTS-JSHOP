package umd.cs.shop;

public interface MCTSPolicy {

    MCTSNode randomChild(MCTSNode parent);

    MCTSNode bestChild(MCTSNode parent);

    void updateReward(MCTSNode parent, double reward);

    void computeNewReward(MCTSNode child);

}
