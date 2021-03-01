package umd.cs.shop;

import java.util.*;

import java.io.*;


public class JSAxiom {
    /*==== instance variables ====*/

    private JSPredicateForm head;

    private JSListConjuncts tail;

    private boolean notDummy;


    JSAxiom(StreamTokenizer tokenizer) {
        head = new JSPredicateForm(tokenizer);

        tail = new JSListConjuncts(tokenizer);

        notDummy = true;

    }

    JSAxiom() {
        super();
        notDummy = false;

    }

    public boolean notDummy() {
        return notDummy;
    }

    public void print() {
        JSUtil.print("(");
        JSUtil.print(":- ");
        head.print();
        tail.print();
        JSUtil.println(")");

    }

    public JSPredicateForm head() {
        return head;
    }

    public JSListConjuncts tail() {
        return tail;
    }

    public JSAxiom standarizerAxiom(JSJshopVars vars) {
        JSAxiom newAx = new JSAxiom();
        JSPredicateForm ta = this.head();
        JSListConjuncts listPs = this.tail();

        newAx.notDummy = this.notDummy();
        newAx.head = ta.standarizerPredicateForm(vars);
        newAx.tail = listPs.standarizerListConjuncts(vars);

        return newAx;
    }

    public void setName(String mName) {
        JSListLogicalAtoms listAtom;
        for (int i = 0; i < tail.listLogicalAtomsVector.size(); i++) {
            listAtom = (JSListLogicalAtoms) tail.listLogicalAtomsVector.elementAt(i);
            listAtom.setName(mName + listAtom.Name());
        }
    }
}

