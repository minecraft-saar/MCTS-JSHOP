package umd.cs.shop;

import java.io.StreamTokenizer;

public class JSFactLandmark {
    private final boolean positive;
    JSPredicateForm predicate;

    public JSFactLandmark(boolean isPositive, JSPredicateForm data) {
        positive = isPositive;
        predicate = (JSPredicateForm) data;
    }

    public JSFactLandmark(StreamTokenizer tokenizer) {
        if (tokenizer == null) throw new JSParserError();//return;
        // JSUtil.flagParser("in PredicateForm()");
        if (tokenizer.ttype == JSJshopVars.plus) {
            positive = true;
        } else if (tokenizer.ttype == JSJshopVars.minus) {
            positive = false;
        } else {
            throw new JSParserError();
        }
        predicate = new JSPredicateForm();

        String w = JSUtil.readWord(tokenizer, "JSPredicateForm");
        if (w.equals("%%%"))//means that an error occur
            throw new JSParserError();
        predicate.addElement(w);
        tokenizer.pushBack();

        if (!JSUtil.expectTokenType(JSJshopVars.leftBrac,
                tokenizer, "expected '['"))
            throw new JSParserError();//  return; // error: expected '('

        JSUtil.readToken(tokenizer, "next FactLandmark Parameter");

        while (tokenizer.ttype != JSJshopVars.rightBrac) {
            JSTerm tmp = new JSTerm();
            if (tokenizer.ttype == JSJshopVars.coma) {
                JSUtil.readToken(tokenizer, "next FactLandmark Parameter");
                continue;
            } else if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                tmp.addElement(new Double(tokenizer.nval).toString());
                JSUtil.readToken(tokenizer, "next FactLandmark Parameter");
            } else if (tokenizer.ttype == StreamTokenizer.TT_WORD) {
                tokenizer.pushBack();
                String varname = JSUtil.readWord(tokenizer, "Expecting constant symbol as term");
                tmp.addElement(varname);
                //readWord already advances tokenizer
            } else {
                JSUtil.println("Line : " + tokenizer.lineno() + " Term expected");
                throw new JSParserError(); //return;
            }
            tmp.makeConstant();
            predicate.addElement(tmp);
        }

    }


    public JSPredicateForm getPredicate() {
        return predicate;
    }

    public boolean compare(JSPredicateForm toCheck, boolean add) {
        if (positive != add) {
            return false;
        }
        return predicate.equals(toCheck);

    }
    public String toString(){
        String s = "";
        if(positive){
            s = s+"+";
        } else {
            s = s + "-";
        }
        s = s + predicate.toString();
        return s;
    }


}
