package umd.cs.shop;

public  class MCTSAlgorithm {

    public static double runMCTS(MCTSNode tst, JSPlanningDomain dom, int depth) {

        JSPlan ans;
        JSPairPlanTState pair;

        if (tst.taskNetwork().isEmpty()) {
            //compute reward for found goal or increase count
            if(tst.inTree) {
                tst.incVisited();
            }
            if (tst.visited() == 1 || !tst.inTree) {
                JSJshopVars.policy.computeNewReward(tst);
            }

            //Get current best reward if it exist
            Double currentReward = Double.NEGATIVE_INFINITY;
            if(JSJshopVars.planFound){
                currentReward = JSJshopVars.bestPlans.lastElement().reward();
            } else {
                long currentTime = System.currentTimeMillis();
                JSUtil.println("Found first plan of reward " + tst.reward() +" in run " + dom.mctsRuns +" after " + (currentTime - JSJshopVars.startTime) + " ms at depth " + depth);
            }

            Double foundReward = tst.reward();
            if(foundReward.compareTo(currentReward) > 0){
                JSJshopVars.bestPlans.addElement(tst);
                if(JSJshopVars.planFound) {
                    long currentTime = System.currentTimeMillis();
                    JSUtil.println("Found better plan of reward " + tst.reward() + " in run " + dom.mctsRuns + " after " + (currentTime - JSJshopVars.startTime) + " ms at depth " + depth);
                }
            }

            JSJshopVars.FoundPlan();
            return tst.reward();
        }
        JSTaskAtom t = (JSTaskAtom) tst.taskNetwork().firstElement();
        JSTasks rest = tst.taskNetwork().cdr();
        rest.removeElement(t);

        if (tst.inTree) {
            if(tst.deadEnd){
                System.out.println("Returned to dead end at depth: " + depth ); //t.print(); JSUtil.println("\n");
                tst.incVisited();
                return tst.reward();
            }
            //Should never happen since either the state is a goal or a dead and should not arrive here
            if (tst.children.size() == 0) {
                //JSUtil.println("No children depth: " + depth);
                tst.incVisited();
                return tst.reward();
            }
            MCTSNode child = JSJshopVars.policy.bestChild(tst);
            if(tst.deadEnd){
                return tst.reward();
            }
            double reward = runMCTS(child, dom, depth+1);
            tst.incVisited();
            JSJshopVars.policy.updateReward(tst, reward);
            if(!child.inTree){
                //JSUtil.println("Adding new node to tree in run " + dom.mctsRuns + "at depth " + depth);
                child.incVisited();
            }
            child.setInTree();
            if(depth > JSJshopVars.treeDepth){
                JSJshopVars.treeDepth = depth;
                long currentTime = System.currentTimeMillis();
                JSUtil.println("Increased tree depth to " + depth + " at run " + dom.mctsRuns + " after " + (currentTime - JSJshopVars.startTime) + " ms" );
            }
            return tst.reward();
        }
        if (tst.children.size() == 0) {
            if (t.isPrimitive()) {
                //task is primitive, so find applicable operators
                pair = t.seekSimplePlanCostFunction(dom, tst.tState(), false);
                ans = pair.plan();
                if (ans.isFailure()) {
                    tst.plan.assignFailure();
                    //JSUtil.println("New dead end at depth: " + depth);
                    //t.print();
                    tst.setDeadEnd();
                    tst.setReward(-2000.0); // TODO fix reward for failure
                    return tst.reward();
                } else {
                    JSPlan pl = new JSPlan();
                    pl.addElements(tst.plan);
                    pl.addElements(ans);
                    JSTaskAtom save = t.cloneTA();
                    //JSTaskAtom method = (JSTaskAtom) ans.get(0);
                    MCTSNode child = new MCTSNode(pair.tState(), rest, pl);
                    tst.addChild(child);
                }
            } else {
                //Reduce task to find all applicable methods
                JSAllReduction red = new JSAllReduction();
                red = dom.methods().findAllReduction(t, tst.tState().state(), red, dom.axioms());
                JSTasks newTasks;
                JSMethod selMet = red.selectedMethod();
                if (red.isDummy()) {
                    assert (!tst.taskNetwork().isEmpty());
                    tst.plan.assignFailure();
                    tst.setDeadEnd();
                    //System.out.println("NO METHOD APPLICABLE, ASSIGNING FAILURE!!!");
                    tst.setReward(-2000.0); // TODO fix reward for failure
                    return tst.reward();
                }
                while (!red.isDummy()) {
                    for (int k = 0; k < red.reductions().size(); k++) {
                        newTasks = (JSTasks) red.reductions().elementAt(k);
                        newTasks.addElements(rest);
                        MCTSNode child = new MCTSNode(tst.tState(), newTasks, tst.plan);
                        tst.addChild(child);
                    }
                    red = dom.methods().findAllReduction(t, tst.tState().state(), red, dom.axioms());
                }
            }
        }

        MCTSNode child = JSJshopVars.policy.randomChild(tst);
        double reward = runMCTS(child, dom, depth+1);
        //tst.incVisited();
        JSJshopVars.policy.updateReward(tst, reward);
        return tst.reward();


    }

}
