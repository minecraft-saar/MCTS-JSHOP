package umd.cs.shop;

import java.util.*;

public class JSListMethods extends Vector<Object> {
    /*==== instance variables ====*/
    private String label;

    JSListMethods() {
        super();
    }

    public void print() {

        JSMethod el;
        for (short i = 0; i < this.size(); i++) {
            el = (JSMethod) this.elementAt(i);
            el.print();
        }
    }

    public JSReduction findReduction(JSTaskAtom task, JSState s, JSReduction red, JSListAxioms axioms) {
        JSSubstitution alpha;
        JSMethod met;
        int base;
        JSListIfThenElse list;
        JSTasks newT;
        JSTaskAtom tmet;
        boolean taskIsGround = task.isGround();

        //task.print();
//    JSUtil.flag("task being reduced");
        if (red.isDummy()) {//JSUtil.flag("dummy");
            base = 0;
        } else {//JSUtil.flag("not dummy");
            base = this.indexOf(red.selectedMethod()) + 1;
        }
        for (int i = base; i < this.size(); i++) {
            met = (JSMethod) this.elementAt(i);
            //met.print();
//		JSUtil.flag("<- evaluating method");
            tmet = met.head();
            if (!taskIsGround) {
                tmet = tmet.standarizerTA(axioms.vars);
            }
            alpha = tmet.matches(task);
            if (!alpha.fail()) {
//		    JSUtil.flag("heads match");
                list = met.ifThenElseList();
                if (!taskIsGround) {
                    list = list.standarizerListIfTE(axioms.vars);
                }
                newT = list.evalPrec(s, alpha, axioms);
                if (!newT.fail()) {//JSUtil.flag("method applicable");
                    axioms.vars.VarCounter++;
                    return new JSReduction(met, newT);
                }
                // JSUtil.flag("method not applicable");
            }
        }
        return new JSReduction();
    }


    public JSAllReduction findAllReduction(JSTaskAtom task, JSState s, JSAllReduction red, JSListAxioms axioms) {
        JSSubstitution alpha;
        JSMethod met;
        int base;
        JSListIfThenElse list;
        JSTasks newT;
        JSTaskAtom tmet;
        Vector<JSTasks> allReductions;
        boolean taskIsGround = task.isGround();


        if (red.isDummy()) {
            base = 0;
        } else {
            base = this.indexOf(red.selectedMethod()) + 1;
        }
        for (int i = base; i < this.size(); i++) {
            met = (JSMethod) this.elementAt(i);
            tmet = met.head();
            if (!taskIsGround)
                tmet = tmet.standarizerTA(axioms.vars);

            alpha = tmet.matches(task);
            if (!alpha.fail()) {
                list = met.ifThenElseList();
                if (!taskIsGround) {
                    list = list.standarizerListIfTE(axioms.vars);
                }
                allReductions = list.evalPrecAll(s, alpha, axioms);
                if (!allReductions.isEmpty()) {
                    axioms.vars.VarCounter++;
                    return new JSAllReduction(met, allReductions);
                }

            }
        }
        return new JSAllReduction();
    }
}
