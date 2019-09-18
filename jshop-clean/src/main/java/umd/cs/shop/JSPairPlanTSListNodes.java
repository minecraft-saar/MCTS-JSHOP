package umd.cs.shop;

import java.util.*;

/*
planS contains the plan and the final state for given problem
listNodes contains the decomposition tree
 */
public class JSPairPlanTSListNodes {
    private JSPairPlanTState planS;
    private Vector<Object> listNodes;

    JSPairPlanTSListNodes() {
        super();
    }

    JSPairPlanTSListNodes(JSPairPlanTState pS, Vector<Object> l) {
        super();
        planS = pS;
        listNodes = l;
    }

    public JSPairPlanTState planS() {
        return planS;
    }

    public Vector<Object> listNodes() {
        return listNodes;
    }


    public void print() {
        JSPairPlanTState pS = this.planS();
        Vector l = this.listNodes();
        JSJshopNode n;
        pS.print();
        JSUtil.println(" ");
        JSUtil.println("Here starts the tree");
        for (short i = 0; i < l.size(); i++) {
            n = (JSJshopNode) l.elementAt(i);
            n.print();
        }

    }


}
