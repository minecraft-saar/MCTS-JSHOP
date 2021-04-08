package umd.cs.shop;

import java.util.*;

import java.io.*;


public class JSListIfThenElse {

    Vector<JSPairIfThen> ifThenVector;

    JSListIfThenElse() {
        ifThenVector = new Vector<>();
    }


    JSListIfThenElse(StreamTokenizer tokenizer) {
        ifThenVector = new Vector<>();
        String name;
        JSPairIfThen pair;
        int index = 1;

        if (!JSUtil.readToken(tokenizer, "Expecting method's IfThenElse list"))
            throw new JSParserError(); //return;
        while (tokenizer.ttype != JSJshopVars.rightPar) {
            tokenizer.pushBack();
            if (!JSUtil.readToken(tokenizer, "Expecting method definition"))
                throw new JSParserError(); //return;
            if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
                tokenizer.pushBack();
                name = "Part_" + index;
            } else {
                tokenizer.pushBack();
                name = JSUtil.readWord(tokenizer, "Expecting name for the method if/then pair.");
                if (name.equals("%%%")) {
                    JSUtil.println("Line : " + tokenizer.lineno() + " Invalid name for a method if/then pair.");
                    throw new JSParserError(); //return;  
                }
            }
            pair = new JSPairIfThen(tokenizer);
            pair.setName(name);
            ifThenVector.addElement(pair);
            if (!JSUtil.readToken(tokenizer, "Expecting ')'"))
                throw new JSParserError(); //return;
            index++;
        }
        tokenizer.pushBack();
    }

    public void print() {

        for (short i = 0; i < ifThenVector.size(); i++) {
            JSPairIfThen el = ifThenVector.elementAt(i);
            el.print();
        }
    }
/*
    JSTasks evalPrec(JSState s, JSSubstitution alpha, JSListAxioms axioms) {
        JSPairIfThen pair;
        JSTasks then;
        JSSubstitution beta;

        for (short i = 0; i < ifThenVector.size(); i++) {
            pair = ifThenVector.elementAt(i);
            beta = s.satisfies(pair.ifPart(), alpha, axioms);
            if (!beta.fail()) {
                //JSUtil.flag("****Success*****");
                //beta.print();
                then = pair.thenPart();
                return then.applySubstitutionTasks(beta);
            }
        }
        then = new JSTasks();
        then.makeFail();
        //JSUtil.flag("Failure");
        return then;

    }*/

    Vector<JSTasks> evalPrecAll(JSState s, JSSubstitution alpha, JSListAxioms axioms) {

        Vector<JSTasks> allReductions = new Vector<>();

        for (short i = 0; i < ifThenVector.size(); i++) {
            JSPairIfThen pair = ifThenVector.elementAt(i);

            JSListSubstitution beta = s.satisfiesAll(pair.ifPart(), alpha, axioms);

            if (!beta.fail()) {
                if (JSJshopVars.flagLevel > 4) {
                    JSUtil.print("Found an applicable method : ");
                    JSUtil.println(pair.Name());
                }
                JSTasks then =  pair.thenPart().cloneTasks();
                for (int k = 0; k < beta.substitutionVector.size(); k++)
                    allReductions.addElement(then.applySubstitutionTasks((JSSubstitution) beta.substitutionVector.elementAt(k)));
                return allReductions;
            }
        }

        return allReductions;

    }


    public JSListIfThenElse standarizerListIfTE(JSJshopVars vars) {
        JSListIfThenElse newList = new JSListIfThenElse();

        for (short i = 0; i < ifThenVector.size(); i++) {
            JSPairIfThen pair = ifThenVector.elementAt(i);
            newList.ifThenVector.addElement(pair.standarizerPIT(vars));
        }
        return newList;
    }

}

