package umd.cs.shop;

import java.util.*;

import java.io.*;


public class JSListLogicalAtoms {

    /*==== instance variables ====*/

    private String label;
    private boolean varlist;
    private String name = "";
    JSTerm term;
    Vector<JSPredicateForm> predicates;

    JSListLogicalAtoms() {
        label = "";
        varlist = false;
        predicates = new Vector<>();
        term = new JSTerm();
    }


    JSListLogicalAtoms(StreamTokenizer tokenizer) {
        // (<PredicateForm>...<PredicateForm>)
        // Example: ((has-money ?person ?old))

        super();
        JSPredicateForm ta;
        label = "";
        varlist = false;
        predicates = new Vector<>();

        if (!JSUtil.readToken(tokenizer, "ListLogicalAtoms"))
            throw new JSParserError(); //return;
        /*  If this is an empty list "nil"    */
        if ((tokenizer.ttype == StreamTokenizer.TT_WORD) && (tokenizer.sval.equalsIgnoreCase("nil")))
            return;
        /*  If this is a variable list " ?x"    */
        if (tokenizer.ttype == JSJshopVars.interrogation) {
            tokenizer.pushBack();
            JSTerm t = new JSTerm(tokenizer);
            if (t.isEmpty())
                throw new JSParserError(); //return;
            this.term = t;
            varlist = true;
            return;
        }

        /* If this is a regular list of atoms */
        tokenizer.pushBack();

        if (!JSUtil.expectTokenType(JSJshopVars.leftPar, tokenizer,
                "ListLogicalAtoms expecting ("))
            throw new JSParserError(); //return;

        if (!JSUtil.readToken(tokenizer, "ListLogicalAtoms"))
            throw new JSParserError(); //return;

        // Added 11/28/00
        if (tokenizer.ttype == JSJshopVars.colon) {

            if (!JSUtil.readToken(tokenizer, " 'first' expected"))
                throw new JSParserError(); //return;  //Error:  There is nothing after :
            if ((tokenizer.ttype != StreamTokenizer.TT_WORD) || (!tokenizer.sval.equalsIgnoreCase("first"))) {
                JSUtil.println("Line : " + tokenizer.lineno() + " Expecting 'first'");
                throw new JSParserError(); //return;  // Error: After colon there must be a word and it must be "first"
            }
            label = tokenizer.sval;

            if (!JSUtil.readToken(tokenizer, "Expecting list of logical atoms"))
                throw new JSParserError(); //return; // Error: There must be something after "first"
        }
        //End of additions

        while (tokenizer.ttype != JSJshopVars.rightPar) {
            tokenizer.pushBack();
            ta = new JSPredicateForm(tokenizer);
            if (ta.size() != 0) {
                this.predicates.addElement(ta);
            } else {
                JSUtil.print("Line : " + tokenizer.lineno() + " ListLogicalAtoms: unexpected Atom");
                throw new JSParserError(); //return;
            }
            if (!JSUtil.readToken(tokenizer, "Expecting ')' "))
                throw new JSParserError(); //return;
        }
        //parsed successfully
    }

    public void addElements(JSListLogicalAtoms l) {
        for (short i = 0; i < l.predicates.size(); i++) {
            this.predicates.addElement(l.predicates.elementAt(i));
        }

    }


    public void print() {

        if (varlist) {
            this.term.print();
            return;
        }
        JSUtil.print("(");

        JSPredicateForm el;
        Enumeration<JSPredicateForm> e = predicates.elements();
        while (e.hasMoreElements()) {
            el = e.nextElement();
            if (el != null)
                el.print();
        }
        JSUtil.println(")  ");
    }

    public JSListLogicalAtoms standarizerListLogicalAtoms(JSJshopVars vars) {
        JSListLogicalAtoms newTs = new JSListLogicalAtoms();
        JSTerm vart;

        if (varlist) {
            vart = this.term;
            newTs.term = vart.standardizerTerm(vars);
            newTs.varlist = true;
            return newTs;
        }


        JSPredicateForm t;

        for (short i = 0; i < this.predicates.size(); i++) {
            t = this.predicates.elementAt(i);
            newTs.predicates.addElement(t.standarizerPredicateForm(vars));
        }
        return newTs;

    }

    public JSListLogicalAtoms applySubstitutionListLogicalAtoms(JSSubstitution alpha) {
        JSListLogicalAtoms nt = new JSListLogicalAtoms();
        JSPredicateForm ti;
        JSPredicateForm nti;
        JSTerm t, newt;
    
   /* if (JSJshopVars.flagLevel > 8) {
     JSUtil.print("apply subs:");
    alpha.print();
    JSUtil.print("On JSListLogicalAtoms:");
    this.print();
    JSUtil.flag("stop");
    }*/

        if (varlist) {
            t = this.term;
            newt = alpha.instance(t);

            while (!newt.isEmpty()) {
                if (!((String) newt.elementAt(0)).equals("."))
                    break;
                ///nti=(JSPredicateForm)newt.elementAt(1);
                nt.term.addElement(newt.elementAt(1));
                newt = (JSTerm) newt.elementAt(2);
            }
            return nt;
        }
        for (short i = 0; i < this.predicates.size(); i++) {
            ti = this.predicates.elementAt(i);
            //if (JSJshopVars.flagLevel > 8)
            //    ti.print();
            nti = ti.applySubstitutionPF(alpha);
            //  if (JSJshopVars.flagLevel > 8)
            //       nti.print();
            nt.predicates.addElement(nti);
          /*  if (JSJshopVars.flagLevel > 8){
                  nt.print();
                  JSUtil.flag("<-- applyJSListLogicalAtoms");
            }*/
        }
        
        
    /*if (JSJshopVars.flagLevel > 8){
        nt.print();
        JSUtil.flag("<-- final applyTasks");
    }*/
        return nt;
    }


    public JSListLogicalAtoms Cdr() {
        JSListLogicalAtoms newLA = new JSListLogicalAtoms();
        JSPredicateForm t;
        // added
        newLA.varlist = this.varlist;
        if (this.varlist)
            return newLA;
        // additions end

        for (short i = 1; i < this.predicates.size(); i++) {
            t = (JSPredicateForm) this.predicates.elementAt(i);
            newLA.predicates.addElement(t);
        }

        return newLA;

    }

    public String Label() {
        return label;
    }

    public String Name() {
        return name;
    }

    public void setName(String val) {
        name = new String(val);
    }

    public JSListLogicalAtoms clone(){
        JSListLogicalAtoms ret = new JSListLogicalAtoms();
        ret.label = this.label;
        ret.name = this.name;
        ret.varlist = this.varlist;
        if(this.varlist){
            ret.term = this.term.cloneT();
        } else {
            ret.predicates.addAll(this.predicates);
        }
        return  ret;
    }

    public boolean equals(Object o) {
        if (!(o instanceof JSListLogicalAtoms))
            return false;

        JSListLogicalAtoms otherList = (JSListLogicalAtoms) o;

        if (!label.equals(otherList.label))
            return false;

        if (varlist != otherList.varlist)
            return false;

        if (!name.equals(otherList.name))
            return false;

        if (this.predicates.size() != otherList.predicates.size())
            return false;

        if (varlist) {
            JSTerm term = this.term;
            JSTerm otherTerm = otherList.term;
            return term.equals(otherTerm);
        }

        for (int i = 0; i < this.predicates.size(); i++) {
            JSPredicateForm pred = (JSPredicateForm) this.predicates.elementAt(i);
            JSPredicateForm otherPred = (JSPredicateForm) otherList.predicates.elementAt(i);
            if (!pred.equals(otherPred)) {
                return false;
            }
        }

        return true;
    }

    public int hashCode() {
        if(varlist){
            JSTerm term = this.term;
            return term.hashCode();
        }

        int hash = 1;
        for(JSPredicateForm pred : this.predicates) {
            int predHash = pred.hashCode();
            hash = JSJshopVars.combineHashCodes(hash, predHash);
        }
        return hash;
    }

}

