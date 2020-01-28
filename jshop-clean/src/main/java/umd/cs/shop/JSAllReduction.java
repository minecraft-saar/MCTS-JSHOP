package umd.cs.shop;

import java.util.*;


public class JSAllReduction {
    private JSMethod selectedMethod;
    private Vector<JSTasks> reduction;


    JSAllReduction() {
        super();
        selectedMethod = new JSMethod();
        reduction = new Vector<JSTasks>();
    }

    JSAllReduction(JSMethod met, Vector<JSTasks> red) {
        super();
        selectedMethod = met;
        reduction = red;
    }

    public JSMethod selectedMethod() {
        return selectedMethod;
    }


    public Vector<JSTasks> reductions() {
        return reduction;
    }

    public boolean isDummy() {
        JSMethod met = this.selectedMethod();
        return !met.notDummy();
    }

    public void print() {
        selectedMethod.print();
        for (int i = 0; i < reduction.size(); i++)
            (reduction.elementAt(i)).print();

    }

    public void printReductions() {
        for (int i = 0; i < reduction.size(); i++) {
            JSUtil.print("#" + (i + 1) + "  ");
            (reduction.elementAt(i)).print();
        }

    }

}
