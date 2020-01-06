package umd.cs.shop;


import java.io.*;
import java.util.HashSet;
import java.util.Set;


public class JSState {

    Set<JSPredicateForm> atoms;

    // This is a Vector over JSPredicateForm (call terms count as JSPredicateForm but should not end up in the state)

    /*==== instance variables ====*/

    public JSState(Set<JSPredicateForm> atoms) {
        this.atoms = new HashSet<JSPredicateForm>();
        this.atoms.addAll(atoms);
    }

    public JSState(StreamTokenizer tokenizer) {
        JSPredicateForm ta;
        //label = "";
        //varlist = false;
        this.atoms = new HashSet<JSPredicateForm>();

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
                JSUtil.flag("Line : " + tokenizer.lineno() + " ListLogicalAtoms: unexpected Atom");
                throw new JSParserError(); //return;
            }
            if (!JSUtil.readToken(tokenizer, "Expecting ')' "))
                throw new JSParserError(); //return;
        }
        //parsed successfully
    }

    JSTState applyOp(JSOperator op, JSSubstitution alpha, JSListLogicalAtoms addL,
                     JSListLogicalAtoms delL) {
        JSListLogicalAtoms add = op.addList();
        JSListLogicalAtoms del = op.deleteList();
        JSListLogicalAtoms opAddL = add.applySubstitutionListLogicalAtoms(alpha);
        JSListLogicalAtoms opDelL = del.applySubstitutionListLogicalAtoms(alpha);
        JSListLogicalAtoms nAddL = new JSListLogicalAtoms();
        JSListLogicalAtoms nDelL = new JSListLogicalAtoms();
        JSPredicateForm el;

        if (JSJshopVars.flagLevel > 3) {

            JSUtil.print(" Matching Operator : (");
            JSUtil.print(":Operator ");
            op.head().applySubstitutionTA(alpha).print();
            JSUtil.print("-: ");
            opDelL.print();
            JSUtil.print("+: ");
            opAddL.print();
            JSUtil.println(")");
        }
        //  JSUtil.flagPlanning("<-- ndelete list");

        JSState ns = new JSState(this.atoms);

        for (Object o : opAddL) {
            ns.atoms.add((JSPredicateForm) o);
        }

        for (Object o : opDelL){
            ns.atoms.remove(o);
        }

        for (short i = 0; i < addL.size(); i++)//creates a new add list
        {
            el = (JSPredicateForm) addL.elementAt(i);
            if (!opDelL.contains(el)) {
                nAddL.addElement(el);
            }
        }
        nAddL.addElements(opAddL);


        for (short i = 0; i < delL.size(); i++)//creates a new del list
        {
            el = (JSPredicateForm) delL.elementAt(i);
            if (!opAddL.contains(el)) {
                nDelL.addElement(el);
            }
        }
        nDelL.addElements(opDelL);

        if (JSJshopVars.flagLevel > 3) {
            JSUtil.println("The resulting state :");
            ns.print();
            JSUtil.println("-----------");
        }
        return new JSTState(ns, nAddL, nDelL);

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
            answer.addElements((JSSubstitution) satisfiers.elementAt(0));
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
        for (i = 0; i < satisfiers.size(); i++) {

            beta = (JSSubstitution) ((JSSubstitution) satisfiers.elementAt(i)).clone();
            beta.addElements((JSSubstitution) alpha.clone());
            answers.addElement(beta);

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
        if (JSJshopVars.flagLevel > 7)
            System.out.println(" ");
        for (JSPredicateForm el : this.atoms) {
            //int i = 0; i < atoms.size(); i++) {
            //el = (JSPredicateForm) atoms.elementAt(i);
            subs = t.matches(el, alpha);
            if (!subs.fail()) {
                if (JSJshopVars.flagLevel > 7) {
                    JSUtil.println(" Goal matches atom: ");
                    el.print();
                }
                answers.addElement(subs);
            }

        }
//        JSUtil.flagPlanning(" NO!");
        if (JSJshopVars.flagLevel > 7 && answers.isEmpty())
            JSUtil.println(" Goal does not match any atom.");
        return answers;
    }

    public Set<JSPredicateForm> atoms(){
        return this.atoms;
    }

    public void print(){
        for(JSPredicateForm pred : this.atoms){
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
