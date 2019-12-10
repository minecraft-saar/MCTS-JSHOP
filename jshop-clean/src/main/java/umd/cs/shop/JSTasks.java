package umd.cs.shop;

import java.util.*;

import java.io.*;


public class JSTasks extends JSListLogicalAtoms {

    /*==== instance variables ====*/

    private boolean fail; //default false

    JSTasks() {
        super();
    }

    JSTasks(StreamTokenizer tokenizer) {
        super();

        JSTaskAtom ta;
        JSUtil.flagParser("in ListTasks()");

        if (!JSUtil.readToken(tokenizer, "List of tasks"))
            throw new JSParserError(); //return;

        /*  If this is an empty list "nil"    */
        if ((tokenizer.ttype == StreamTokenizer.TT_WORD) && (tokenizer.sval.equalsIgnoreCase("nil"))) {

            return;
        }

        tokenizer.pushBack();

        if (!JSUtil.expectTokenType(JSJshopVars.leftPar, tokenizer,
                " Expecting '(' "))
            throw new JSParserError(); //return;


        if (!JSUtil.readToken(tokenizer, "Expecting list of tasks"))
            throw new JSParserError(); //return;

        while (tokenizer.ttype != JSJshopVars.rightPar) {
            tokenizer.pushBack();
            ta = new JSTaskAtom(tokenizer);
            if (ta.size() != 0) {
                this.addElement(ta);
            } else {
                JSUtil.println("Line: " + tokenizer.lineno() + " parsing list of tasks: unexpected atom");
                throw new JSParserError(); //return;
            }
            if (!JSUtil.readToken(tokenizer, "Expecting ')'"))
                throw new JSParserError(); //return;
        }

        //  JSUtil.flagParser("ListTasks parse succesful");
    }

    public double runMCTS(JSPairTStateTasks tst, JSPlanningDomain dom,  int depth) {

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
            //long currentTime = System.currentTimeMillis();
            //JSUtil.println("Found plan of reward " + tst.reward() +" in run " + dom.mctsRuns +" after " + (currentTime - JSJshopVars.startTime) + " ms at depth " + depth);
            JSJshopVars.FoundPlan();
            return tst.reward();
        }
        JSTaskAtom t = (JSTaskAtom) tst.taskNetwork().firstElement();
        JSTasks rest = tst.taskNetwork().cdr();
        rest.removeElement(t);

        if (tst.inTree) {

            if(tst.deadEnd){
                //System.out.println("Returned to dead end at depth: "+ depth ); //t.print(); JSUtil.println("\n");
                tst.incVisited();
                return tst.reward();
            }
            //Should never happen since either the state is a goal or a dead and should not arrive here
            if (tst.children.size() == 0) {
                JSUtil.println("No children depth: " + depth);
                tst.incVisited();
                return tst.reward();
            }
            JSPairTStateTasks child = JSJshopVars.policy.bestChild(tst);
            //in bestchild there is also a check for dead end happening
            if(tst.deadEnd){
                return tst.reward();
            }
            double reward = runMCTS(child, dom, depth+1);
            JSJshopVars.policy.updateReward(tst, reward);
            tst.incVisited();
            if(!child.inTree){
                //JSUtil.println("Adding new node to tree in run " + dom.mctsRuns + "at depth " + depth);
            }
            child.setInTree();
            if(depth > JSJshopVars.treeDepth){
                JSJshopVars.treeDepth = depth;
                long currentTime = System.currentTimeMillis();
                JSUtil.println("Increased tree depth to " + depth +" at run "+ dom.mctsRuns + " after " + (currentTime - JSJshopVars.startTime) + " ms" );
            }
            return tst.reward();
        }
        if (tst.children.size() == 0) {
            //System.out.println("MCTS TREE depth=" + depth);

            if (t.isPrimitive()) {
                //task is primitive, so find applicable operators
                pair = t.seekSimplePlan(dom, tst.tState());
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
                    //listNodes.addElement(new JSJshopNode(save, new Vector<>()));
                    //JSTaskAtom method = (JSTaskAtom) ans.get(0);
                    JSPairTStateTasks child = new JSPairTStateTasks(pair.tState(), rest, pl);
                    tst.addChild(child);
                }
            } else {
                //Reduce task to find all applicable methods
                JSAllReduction red = new JSAllReduction();
                red = dom.methods().findAllReduction(t, tst.tState().state(), red, dom.axioms());
                JSTasks newTasks;

                if (red.isDummy()) {
                    assert (!tst.taskNetwork().isEmpty());
                    tst.plan.assignFailure();
                    tst.setDeadEnd();
                    //System.out.println("no method dead-end at depth: " + depth);
                    tst.setReward(-2000.0); // TODO fix reward for failure
                    return tst.reward();
                }
                while (!red.isDummy()) {
                    //JSMethod selMet = red.selectedMethod();
                    //System.out.println(selMet.head());
                    //red.printReductions();
                    for (int k = 0; k < red.reductions().size(); k++) {
                        //JSMethod selected = red.reductions().se
                        newTasks = (JSTasks) red.reductions().elementAt(k);
                        newTasks.addElements(rest);
                        JSPairTStateTasks child = new JSPairTStateTasks(tst.tState(), newTasks, tst.plan);
                        tst.addChild(child);
                    }
                    red = dom.methods().findAllReduction(t, tst.tState().state(), red, dom.axioms());
                }
            }
        }

        JSPairTStateTasks child = JSJshopVars.policy.randomChild(tst);
        double reward = runMCTS(child, dom,  depth+1);
        //tst.incVisited();
        JSJshopVars.policy.updateReward(tst, reward);
        return tst.reward();


    }


    public JSPairPlanTState seekPlan(JSTState ts, JSPlanningDomain dom, JSPlan pl, Vector<Object> listNodes) {

        JSPlan ans;
        JSPairPlanTState pair;
        //JSPlan sol;

        if (this.isEmpty()) {
            return new JSPairPlanTState(pl, ts);
        }

        JSTaskAtom t = (JSTaskAtom) this.firstElement();
        JSTasks rest = this.cdr();
        rest.removeElement(t);

        if (t.isPrimitive()) {
            pair = t.seekSimplePlan(dom, ts);
            ans = pair.plan();
            if (ans.isFailure()) {
                return pair; // failure
            } else {
                pl.addElements(ans);
                listNodes.addElement(new JSJshopNode(t, new Vector<>()));
                return rest.seekPlan(pair.tState(), dom, pl, listNodes);
            }
        } else {
            JSJshopNode node;
            JSReduction red = new JSReduction();
            red = t.reduce(dom, ts.state(), red); //counter to iterate
            // on all reductions

            JSTasks newTasks;
            JSMethod selMet = red.selectedMethod();
            while (!red.isDummy()) {
                newTasks = red.reduction();
                node = new JSJshopNode(t, newTasks.cloneTasks());

                //  JSUtil.flag("<- tasks");
                newTasks.addElements(rest);
                pair = newTasks.seekPlan(ts, dom, pl, listNodes);
                if (!pair.plan().isFailure()) {
                    //  JSUtil.flag("reduced");
                    listNodes.addElement(node);
                    return pair;
                }
                // JSUtil.flag("iterating");
                red = t.reduce(dom, ts.state(), red);
                selMet = red.selectedMethod();
            }
        }
        ans = new JSPlan();
        ans.assignFailure();
        return new JSPairPlanTState(ans, new JSTState());

    }

    /*   Multi plan generator */
    public JSListPairPlanTStateNodes seekPlanAll(JSPairTStateTasks ts, JSPlanningDomain dom, boolean All) {
        JSListPairPlanTStateNodes results, plans = new JSListPairPlanTStateNodes();
        JSPairPlanTSListNodes ptl;
        JSPlan ans;
        JSPairPlanTState pair;
        JSPlan sol;
        JSJshopNode node;
        Vector listnodes;
        JSTaskAtom ta;
        JSTState tts;

        if (this.isEmpty()) {
            if (JSJshopVars.flagLevel > 1)
                JSUtil.println("Returning successfully from find-plan : No more tasks to plan");

            //pair = new JSPairPlanTState((new JSPlan()), ts.tState());
            pair = new JSPairPlanTState(ts.plan, ts.tState());
            ptl = new JSPairPlanTSListNodes(pair, new Vector<>());
            double cost = ts.plan.planCost();
            long currentTime = System.currentTimeMillis();
            JSUtil.println("Found plan after " + (currentTime - JSJshopVars.startTime) + " ms of cost " + cost);
            plans.addElement(ptl);
            return plans;
        }

        JSTaskAtom t = (JSTaskAtom) this.firstElement();
        if (JSJshopVars.flagLevel > 2) {
            JSUtil.println(" ");
            JSUtil.print("Searching a plan for");
            t.print();
        }
        JSTasks rest = this.cdr();

        if (t.isPrimitive()) {
            pair = t.seekSimplePlan(dom, ts.tState());
            ans = pair.plan();
            if (ans.isFailure()) {
                if (JSJshopVars.flagLevel > 1)
                    JSUtil.println("Returning failure from find-plan: Can not find an operator");
                return plans; // failure - empty list
            }
            JSPairTStateTasks tst = new JSPairTStateTasks(pair.tState(), new JSPlan() );
            tst.plan.addElements(ts.plan);
            tst.plan.addElements(ans);
            results = rest.seekPlanAll(tst, dom, All);

            if (results.isEmpty())
                return plans;

            ta = (JSTaskAtom) ans.elementAt(0);
            node = new JSJshopNode(t, new Vector<>());

            for (int i = 0; i < results.size(); i++) {
                ptl = (JSPairPlanTSListNodes) results.elementAt(i);
                //ptl.planS().plan().insertWithCost(0, ta, ans.elementCost(0));
                ptl.listNodes().insertElementAt(node, 0);
                plans.addElement(ptl);
            }

            return plans;
        }

        JSAllReduction red = new JSAllReduction();
        red = dom.methods().findAllReduction(t, ts.tState().state(), red, dom.axioms());
        JSTasks newTasks;
        JSMethod selMet = red.selectedMethod();
        if (JSJshopVars.flagLevel > 1 && red.isDummy())
            JSUtil.println("Returning failure from find-plan: Can not find an applicable method");
        while (!red.isDummy()) {
            if (JSJshopVars.flagLevel > 4) {
                JSUtil.println("The reductions are: ");
                red.printReductions();
            }

            for (int k = 0; k < red.reductions().size(); k++) {

                newTasks = (JSTasks) red.reductions().elementAt(k);
                node = new JSJshopNode((JSTaskAtom) t.clone(), newTasks.cloneTasks());
                newTasks.addElements(rest);
                JSPlan tmp = new JSPlan();
                tmp.addElements(ts.plan);
                JSPairTStateTasks tst = new JSPairTStateTasks(new JSTState(ts.tState()),tmp );
                results = newTasks.seekPlanAll(tst, dom, All);

                if (results.isEmpty())
                    continue;

                for (int j = 0; j < results.size(); j++) {
                    ptl = (JSPairPlanTSListNodes) results.elementAt(j);
                    ptl.listNodes().addElement(node);
                    plans.addElement(ptl);
                    if (plans.size() >= 1 && !All)
                        return plans;
                }
            }
            red = dom.methods().findAllReduction(t, ts.tState().state(), red, dom.axioms());
            selMet = red.selectedMethod();

        }

        return plans;

    }


    /*****************************************************************/


    public boolean fail() {
        return fail;
    }

    public void makeFail() {
        fail = true;
    }

    public void makeSucceed() {
        fail = false;
    }

    public JSTasks applySubstitutionTasks(JSSubstitution alpha) {
        JSTasks nt = new JSTasks();
        JSTaskAtom ti;
        JSTaskAtom nti;


        for (short i = 0; i < this.size(); i++) {
            ti = (JSTaskAtom) this.elementAt(i);
            //ti.print();
            nti = ti.applySubstitutionTA(alpha);
            //nti.print();
            nt.addElement(nti);
            //nt.print();
            //JSUtil.flag("<-- applyJSTasks");
        }
        
        
    /* nt.print();
    // JSUtil.flag("<-- final applyTasks");*/
        return nt;
    }

    public boolean contains(JSTaskAtom t) {
        JSTaskAtom el;

        for (int i = this.size() - 1; i > -1; i--) {
            el = (JSTaskAtom) this.elementAt(i);
            
            /*JSUtil.print("JSTaskAtom:");
            el.print();
            JSUtil.print(" equals: ");
            t.print();
            JSUtil.flag("?");*/

            if (t.equals(el)) {
                //JSUtil.flag("YES");
                return true;
            }
        }
        //JSUtil.flag("NO");
        return false;
    }

    public JSTasks cloneTasks() {
        JSTasks newTs = new JSTasks();
        JSTaskAtom t;

        for (short i = 0; i < this.size(); i++) {
            t = (JSTaskAtom) this.elementAt(i);
            newTs.addElement(t.cloneTA());
        }
        return newTs;

    }

    public JSTasks cdr() {
        JSTasks newTs = new JSTasks();
        JSTaskAtom t;

        for (short i = 1; i < this.size(); i++) {
            t = (JSTaskAtom) this.elementAt(i);
            newTs.addElement(t);
        }
        return newTs;

    }

    public JSTasks standarizerTasks() {
        JSTasks newTs = new JSTasks();
        JSTaskAtom t;

        for (short i = 0; i < this.size(); i++) {
            t = (JSTaskAtom) this.elementAt(i);
            newTs.addElement(t.standarizerTA());
        }
        return newTs;

    }
}

    
