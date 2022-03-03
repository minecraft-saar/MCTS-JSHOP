package umd.cs.shop;


import umd.cs.shop.costs.NLGCost;

import java.io.*;
import java.util.HashSet;
import java.util.Set;


public class JSState {

    Set<JSPredicateForm> atoms;
    public boolean wallBuilt = false;
    public boolean railingBuilt = false;
    public boolean stairsBuilt = false;
    // This is a Vector over JSPredicateForm (call terms count as JSPredicateForm but should not end up in the state)
    public Set<JSTaskLandmark> taskLandmarks;
    Set<JSFactLandmark> factLandmarks;


    /*==== instance variables ====*/

    public JSState(Set<JSPredicateForm> atoms) {
        this.atoms = new HashSet<>();
        this.atoms.addAll(atoms);
    }

    public JSState(Set<JSPredicateForm> atoms, Set<JSFactLandmark> facts, Set<JSTaskLandmark> tasks) {
        this.atoms = new HashSet<>();
        this.atoms.addAll(atoms);
        this.factLandmarks = new HashSet<>();
        this.factLandmarks.addAll(facts);
        //for(JSPredicateForm pred : this.atoms){
        //    this.factLandmarks.removeIf(landmark -> landmark.compare(pred, true));
        //}
        this.taskLandmarks = new HashSet<>();
        this.taskLandmarks.addAll(tasks);

    }

    public JSState(StreamTokenizer tokenizer) {
        JSPredicateForm ta;
        //label = "";
        //varlist = false;
        this.atoms = new HashSet<>();

        if (!JSUtil.readToken(tokenizer, "ListLogicalAtoms"))
            throw new JSParserError(); //return;
        /*  If this is an empty list "nil"    */
        if ((tokenizer.ttype == StreamTokenizer.TT_WORD) && (tokenizer.sval.equalsIgnoreCase("nil")))
            return;
        /*  If this is a variable list " ?x"    */
        if (tokenizer.ttype == JSJshopVars.interrogation) {
            System.err.println("Unexpected term while parsing state!");
            throw new JSParserError();
        }

        /* If this is a regular list of atoms */
        tokenizer.pushBack();

        if (!JSUtil.expectTokenType(JSJshopVars.leftPar, tokenizer,
                "ListLogicalAtoms expecting ("))
            throw new JSParserError(); //return;

        if (!JSUtil.readToken(tokenizer, "State"))
            throw new JSParserError(); //return;

        if (tokenizer.ttype == JSJshopVars.colon) {

            if (!JSUtil.readToken(tokenizer, " 'first' expected"))
                throw new JSParserError(); //return;  //Error:  There is nothing after :
            if ((tokenizer.ttype != StreamTokenizer.TT_WORD) || (!tokenizer.sval.equalsIgnoreCase("first"))) {
                JSUtil.println("Line : " + tokenizer.lineno() + " Expecting 'first'");
                throw new JSParserError(); //return;  // Error: After colon there must be a word and it must be "first"
            }
            //label = tokenizer.sval;

            if (!JSUtil.readToken(tokenizer, "Expecting list of logical atoms"))
                throw new JSParserError(); //return; // Error: There must be something after "first"
        }

        while (tokenizer.ttype != JSJshopVars.rightPar) {
            tokenizer.pushBack();
            ta = new JSPredicateForm(tokenizer);
            if (ta.size() != 0) {
                atoms.add(ta);
            } else {
                JSUtil.println("Line : " + tokenizer.lineno() + " ListLogicalAtoms: unexpected Atom");
                throw new JSParserError(); //return;
            }
            if (!JSUtil.readToken(tokenizer, "Expecting ')' "))
                throw new JSParserError(); //return;
        }
        //parsed successfully
    }

    JSTState applyOp(JSOperator op, JSSubstitution alpha, JSListLogicalAtoms addL,
                     JSListLogicalAtoms delL, JSJshopVars vars) {
        JSListLogicalAtoms add = op.addList();
        JSListLogicalAtoms del = op.deleteList();
        JSListLogicalAtoms opAddL = add.applySubstitutionListLogicalAtoms(alpha);
        JSListLogicalAtoms opDelL = del.applySubstitutionListLogicalAtoms(alpha);
        JSListLogicalAtoms nAddL = new JSListLogicalAtoms();
        JSListLogicalAtoms nDelL = new JSListLogicalAtoms();
        JSPredicateForm el;

        /*if (JSJshopVars.flagLevel > 3) {

            JSUtil.print(" Matching Operator : (");
            JSUtil.print(":Operator ");
            op.head().applySubstitutionTA(alpha).print();
            JSUtil.print("-: ");
            opDelL.print();
            JSUtil.print("+: ");
            opAddL.print();
            JSUtil.println(")");
        } */
        //  JSUtil.flagPlanning("<-- ndelete list");
        JSState ns;
        if (vars.landmarks)
            ns = new JSState(this.atoms, this.factLandmarks, this.taskLandmarks);
        else
            ns = new JSState(this.atoms);

        for (JSPredicateForm pred : opDelL.predicates) {
            ns.atoms.remove(pred);
            if (vars.landmarks)
                ns.factLandmarks.removeIf(landmark -> landmark.compare(pred, false));
        }


        for (JSPredicateForm pred : opAddL.predicates) {
            ns.atoms.add(pred);
            if (vars.landmarks)
                ns.factLandmarks.removeIf(landmark -> landmark.compare(pred, true));
        }


        for (short i = 0; i < addL.predicates.size(); i++)//creates a new add list
        {
            el = addL.predicates.elementAt(i);
            if (!opDelL.predicates.contains(el)) {
                nAddL.predicates.addElement(el);
            }
            if (vars.landmarks) {
                JSPredicateForm pred = addL.predicates.elementAt(i);
                ns.factLandmarks.removeIf(landmark -> landmark.compare(pred, true));
            }

        }
        nAddL.addElements(opAddL);
        if(! (vars.costFunction instanceof NLGCost)){
            ns.updateBoolFlags(this, add);
        }


        for (short i = 0; i < delL.predicates.size(); i++)//creates a new del list
        {
            el = delL.predicates.elementAt(i);
            if (!opAddL.predicates.contains(el)) {
                nDelL.predicates.addElement(el);
            }
            if (vars.landmarks) {
                JSPredicateForm pred = (JSPredicateForm) delL.predicates.elementAt(i);
                ns.factLandmarks.removeIf(landmark -> landmark.compare(pred, false));
            }
        }
        nDelL.addElements(opDelL);

        /*if (JSJshopVars.flagLevel > 3) {
            JSUtil.println("The resulting state :");
            ns.print();
            JSUtil.println("-----------");
        }*/
        return new JSTState(ns, nAddL, nDelL);

    }

    public void updateBoolFlags(JSState oldState, JSListLogicalAtoms addList) {
        this.wallBuilt = oldState.wallBuilt;
        this.railingBuilt = oldState.railingBuilt;
        this.stairsBuilt = oldState.stairsBuilt;
        boolean alltrue = railingBuilt && stairsBuilt && wallBuilt;
        if (!alltrue) {
            for (int i = 0; i < addList.predicates.size(); i++) {
                JSPredicateForm pred = (JSPredicateForm) addList.predicates.elementAt(i);
                if (pred.elementAt(0).equals("wall-at")) {
                    this.wallBuilt = true;
                    alltrue = railingBuilt && stairsBuilt && wallBuilt;
                    if (alltrue) {
                        break;
                    }
                }
                if (pred.elementAt(0).equals("railing-at")) {
                    this.railingBuilt = true;
                    alltrue = railingBuilt && stairsBuilt && wallBuilt;
                    if (alltrue) {
                        break;
                    }
                }
                if (pred.elementAt(0).equals("stairs-at")) {
                    this.stairsBuilt = true;
                    alltrue = railingBuilt && stairsBuilt && wallBuilt;
                    if (alltrue) {
                        break;
                    }
                }
            }
        }

    }

    public JSSubstitution satisfies(JSListLogicalAtoms conds, JSSubstitution alpha,
                                    JSListAxioms axioms)
    //Tests if  conds can be infered from this (the current state) and axioms
    //modulo the substitution alpha.
    //
    //If conds can be infered, it returns the first matching substitution
    //
    //else it returns the failure substitution (i.e., calling the method fail()
    //to the returned substitution succeeds)
    {

        //Calls Fusun's inferencing method with parameter all set to "false"

        JSListSubstitution satisfiers = axioms.TheoremProver(conds, this, alpha, false);
        JSSubstitution answer;
        if (satisfiers.fail()) {
            answer = new JSSubstitution();
            answer.assignFailure();

        } else {
            answer = (JSSubstitution) alpha.clone();
            answer.addElements((JSSubstitution) satisfiers.substitutionVector.elementAt(0));
       /*  if (conds.size() >0){
            this.print();
             }*/
        }
        return answer;
    }

    public JSListSubstitution satisfiesAll(JSListLogicalAtoms conds, JSSubstitution alpha,
                                           JSListAxioms axioms)
    //Tests if  conds can be inferred from this (the current state) and axioms
    //modulo the substitution alpha.
    //
    //If conds can be inferred, it returns all of the matching substitutions
    //
    //else it returns an empty list
    {

        //Calls Fusun's inferencing method with parameter all set to "true"
        int i;
        String first = conds.Label();
        boolean findfirst = !first.equalsIgnoreCase("first");


        JSSubstitution beta;
        JSListSubstitution answers = new JSListSubstitution();
        JSListSubstitution satisfiers = axioms.TheoremProver(conds, this, alpha, findfirst);
        for (i = 0; i < satisfiers.substitutionVector.size(); i++) {

            beta = (JSSubstitution) (satisfiers.substitutionVector.elementAt(i)).clone();
            beta.addElements((JSSubstitution) alpha.clone());
            answers.substitutionVector.addElement(beta);

        }

        return answers;

    }

    public JSListSubstitution satisfiesTAm(JSPredicateForm t, JSSubstitution alpha)

    // It searches all atoms in this matching t modulo the substitution alpha.
    //
    // If such an atoms are found, the matching substitutions are returned.
    //
    // Otherwise: returns an empty list
    {
        //JSPredicateForm el;
        JSSubstitution subs;
        JSListSubstitution answers = new JSListSubstitution();
        //if (JSJshopVars.flagLevel > 7)
        //    System.out.println(" ");
        for (JSPredicateForm el : this.atoms) {
            //int i = 0; i < atoms.size(); i++) {
            //el = (JSPredicateForm) atoms.elementAt(i);
            subs = t.matches(el, alpha);
            if (!subs.fail()) {
                /*if (JSJshopVars.flagLevel > 7) {
                    JSUtil.println(" Goal matches atom: ");
                    el.print();
                }*/
                answers.substitutionVector.addElement(subs);
            }

        }
//        JSUtil.flagPlanning(" NO!");
        /*if (JSJshopVars.flagLevel > 7 && answers.isEmpty())
            JSUtil.println(" Goal does not match any atom.");*/
        return answers;
    }

    public Set<JSPredicateForm> atoms() {
        return this.atoms;
    }

    public void print() {
        for (JSPredicateForm pred : this.atoms) {
            pred.print();
        }
    }


    public boolean equals(Object o) {
        if (!(o instanceof JSState))
            return false;
        JSState state = (JSState) o;
        if (state.atoms.size() != this.atoms.size())
            return false;

        return state.atoms.equals(this.atoms);
    }

    public int hashCode() {
        return this.atoms.hashCode();
    }

}
