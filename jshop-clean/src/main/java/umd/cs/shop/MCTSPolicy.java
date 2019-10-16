package umd.cs.shop;

public interface MCTSPolicy {

    JSPairTStateTasks randomChild(JSPairTStateTasks parent);

    JSPairTStateTasks bestChild(JSPairTStateTasks parent);

    void updateReward(JSPairTStateTasks parent, double reward);

    void computeNewReward(JSPairTStateTasks child);

}
