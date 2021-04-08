package umd.cs.shop;

import java.util.*;

import java.io.*;


public class JSTasks extends JSListLogicalAtoms {

    /*==== instance variables ====*/

    //private boolean fail; //default false

    JSTasks() {
        super();
    }

    JSTasks(Vector<JSPredicateForm> tasks){
        super();
        predicates.addAll(tasks);
    }

    JSTasks(StreamTokenizer tokenizer) {
        super();

        JSTaskAtom ta;
        //JSUtil.flagParser("in ListTasks()");

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
                this.predicates.addElement(ta);
            } else {
                JSUtil.println("Line: " + tokenizer.lineno() + " parsing list of tasks: unexpected atom");
                throw new JSParserError(); //return;
            }
            if (!JSUtil.readToken(tokenizer, "Expecting ')'"))
                throw new JSParserError(); //return;
        }

        //  JSUtil.flagParser("ListTasks parse succesful");
    }

    /*   Multi plan generator */
    public Vector<JSPairPlanTSListNodes> seekPlanAll(MCTSNode ts, boolean All, JSJshopVars vars) {

        Vector<JSPairPlanTSListNodes> results, plans = new Vector<>();
        JSPairPlanTSListNodes ptl;
        JSPlan ans;
        JSPairPlanTState pair;
        //JSPlan sol;
        JSJshopNode node;
        //Vector listnodes;
        JSTaskAtom ta;
        //JSTState tts;

        if (vars.bb_pruning(ts.plan.planCost())){
            return plans;
        }

        if (this.predicates.isEmpty()) {
            if (JSJshopVars.flagLevel > 1)
                JSUtil.println("Returning successfully from find-plan : No more tasks to plan");

            //pair = new JSPairPlanTState((new JSPlan()), ts.tState());
            pair = new JSPairPlanTState(ts.plan, ts.tState());
            ptl = new JSPairPlanTSListNodes(pair, new Vector<>());
            //double cost = ts.plan.planCost();
            //long currentTime = System.currentTimeMillis();
            vars.foundPlan(ts.plan, 0);
            //JSUtil.println("Found plan after " + (currentTime - JSJshopVars.startTime) + " ms of cost " + cost);
            plans.addElement(ptl);
            return plans;
        }

        JSTaskAtom t = (JSTaskAtom) this.predicates.firstElement();
        if (JSJshopVars.flagLevel > 2) {
            JSUtil.println(" ");
            JSUtil.print("Searching a plan for");
            t.print();
        }
        JSTasks rest = this.cdr();

        if (t.isPrimitive()) {
            pair = t.seekSimplePlanCostFunction(ts.tState(), vars);
            ans = pair.plan();
            if (ans.isFailure()) {
                if (JSJshopVars.flagLevel > 1)
                    JSUtil.println("Returning failure from find-plan: Can not find an operator");
                return plans; // failure - empty list
            }
            MCTSNode tst = new MCTSNode(pair.tState(), new JSPlan() );
            tst.plan.addElements(ts.plan);
            tst.plan.addElements(ans);
            results = rest.seekPlanAll(tst, All, vars);

            if (results.isEmpty())
                return plans;

            ta = (JSTaskAtom) ans.predicates.elementAt(0);
            node = new JSJshopNode(t, new JSTasks());

            for (int i = 0; i < results.size(); i++) {
                ptl = results.elementAt(i);
                //ptl.planS().plan().insertWithCost(0, ta, ans.elementCost(0));
                ptl.listNodes().insertElementAt(node, 0);
                plans.addElement(ptl);
            }

            return plans;
        }

        JSAllReduction red = new JSAllReduction();
        red = vars.domain.methods().findAllReduction(t, ts.tState().state(), red, vars.domain.axioms());
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

                newTasks = red.reductions().elementAt(k);
                node = new JSJshopNode((JSTaskAtom) t.clone(), newTasks.cloneTasks());
                newTasks.addElements(rest);
                JSPlan tmp = new JSPlan();
                tmp.addElements(ts.plan);
                MCTSNode tst = new MCTSNode(new JSTState(ts.tState()),tmp );
                results = newTasks.seekPlanAll(tst, All,vars);

                if (results.isEmpty())
                    continue;

                for (int j = 0; j < results.size(); j++) {
                    ptl = results.elementAt(j);
                    ptl.listNodes().addElement(node);
                    plans.addElement(ptl);
                    if (plans.size() >= 1 && !All)
                        return plans;
                }
            }
            red = vars.domain.methods().findAllReduction(t, ts.tState().state(), red, vars.domain.axioms());
            selMet = red.selectedMethod();
        }

        return plans;

    }


    /*****************************************************************/


    /*public boolean fail() {
        return fail;
    }

    public void makeFail() {
        fail = true;
    }

    public void makeSucceed() {
        fail = false;
    }*/

    public JSTasks applySubstitutionTasks(JSSubstitution alpha) {
        JSTasks nt = new JSTasks();
        JSTaskAtom ti;
        JSTaskAtom nti;


        for (short i = 0; i < this.predicates.size(); i++) {
            ti = (JSTaskAtom) this.predicates.elementAt(i);
            //ti.print();
            nti = ti.applySubstitutionTA(alpha);
            //nti.print();
            nt.predicates.addElement(nti);
            //nt.print();
            //JSUtil.flag("<-- applyJSTasks");
        }
        
        
    /* nt.print();
    // JSUtil.flag("<-- final applyTasks");*/
        return nt;
    }

    public boolean contains(JSTaskAtom t) {
        JSTaskAtom el;

        for (int i = this.predicates.size() - 1; i > -1; i--) {
            el = (JSTaskAtom) this.predicates.elementAt(i);
            
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

        for (short i = 0; i < this.predicates.size(); i++) {
            t = (JSTaskAtom) this.predicates.elementAt(i);
            newTs.predicates.addElement(t.cloneTA());
        }
        return newTs;

    }

    public JSTasks cdr() {
        JSTasks newTs = new JSTasks();
        JSTaskAtom t;

        for (short i = 1; i < this.predicates.size(); i++) {
            t = (JSTaskAtom) this.predicates.elementAt(i);
            newTs.predicates.addElement(t);
        }
        return newTs;

    }

    public JSTasks standarizerTasks(JSJshopVars vars) {
        JSTasks newTs = new JSTasks();
        JSTaskAtom t;

        for (short i = 0; i < this.predicates.size(); i++) {
            t = (JSTaskAtom) this.predicates.elementAt(i);
            newTs.predicates.addElement(t.standarizerTA(vars));
        }
        return newTs;

    }
}

    
