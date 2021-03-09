package umd.cs.shop;

import java.util.*;

import java.io.*;


public class JSListConjuncts {

    Vector<JSListLogicalAtoms> listLogicalAtomsVector;
    JSListConjuncts() {
        listLogicalAtomsVector = new Vector<>();
    }

    JSListConjuncts(Vector<JSListLogicalAtoms> inputVec){
        listLogicalAtomsVector = inputVec;
    }


    JSListConjuncts(StreamTokenizer tokenizer) {
        super();
        listLogicalAtomsVector = new Vector<>();
        JSListLogicalAtoms listAtoms;
        String name;
        int index = 1;
        if (!JSUtil.readToken(tokenizer, "Expecting list of conjuncts as tail of the axiom "))
            throw new JSParserError(); // return;
        while (tokenizer.ttype != JSJshopVars.rightPar) {
            tokenizer.pushBack();
            if (!JSUtil.readToken(tokenizer, "Expecting axiom tail definition"))
                throw new JSParserError();//return;
            if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
                tokenizer.pushBack();
                name = "Part_" + index;
            } else {
                tokenizer.pushBack();
                name = JSUtil.readWord(tokenizer, "Expecting name for the method if/then pair.");
                if (name.equals("%%%")) {
                    JSUtil.println("Invalid name for an axiom tail.");
                    throw new JSParserError();//return;  
                }
            }
            listAtoms = new JSListLogicalAtoms(tokenizer);
            listAtoms.setName(name);
            listLogicalAtomsVector.addElement(listAtoms);
            if (!JSUtil.readToken(tokenizer, "Expecting ) for axiom definition"))
                throw new JSParserError();//return;
            index++;
        }
        tokenizer.pushBack();
    }

    public void print() {

        JSListLogicalAtoms el;
        for (short i = 0; i < listLogicalAtomsVector.size(); i++) {
            el = listLogicalAtomsVector.elementAt(i);
            JSUtil.println(el.Name());
            el.print();
        }
    }

    public JSListConjuncts standarizerListConjuncts(JSJshopVars vars) {
        JSListConjuncts newList = new JSListConjuncts();
        JSListLogicalAtoms listAtoms;

        for (short i = 0; i < listLogicalAtomsVector.size(); i++) {
            listAtoms = listLogicalAtomsVector.elementAt(i);
            newList.listLogicalAtomsVector.addElement(listAtoms.standarizerListLogicalAtoms(vars));
        }
        return newList;
    }

}

