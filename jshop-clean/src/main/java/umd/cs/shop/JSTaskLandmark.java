package umd.cs.shop;

import java.io.StreamTokenizer;

public class JSTaskLandmark {

    //private final boolean positive;
    JSTaskAtom task;

    public JSTaskLandmark(boolean isPositive, JSPredicateForm data) {
        //positive = isPositive;
        task = (JSTaskAtom) data;
    }

    public JSTaskLandmark(StreamTokenizer tokenizer, boolean isPrimitive) {
        task = new JSTaskAtom();
        tokenizer.pushBack();
        String w = JSUtil.readWord(tokenizer, "JSPredicateForm");
        if (w.equals("%%%"))//means that an error occurred 
            throw new JSParserError();
        if(isPrimitive){
            w = "!" + w;
            task.makePrimitive();
        } else {
            task.makeCompound();
        }
        task.addElement(w);
        tokenizer.pushBack();

        if (!JSUtil.expectTokenType(JSJshopVars.leftBrac,
                tokenizer, "expected '['"))
            throw new JSParserError();//  return; // error: expected '('

        JSUtil.readToken(tokenizer, "next TaskLandmark Parameter");

        while (tokenizer.ttype != JSJshopVars.rightBrac) {
            JSTerm tmp = new JSTerm();
            if (tokenizer.ttype == JSJshopVars.coma) {
                JSUtil.readToken(tokenizer, "next TaskLandmark Parameter");
                continue;
            } else if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                tmp.addElement(Double.toString(tokenizer.nval));
                JSUtil.readToken(tokenizer, "next TaskLandmark Parameter");

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
            task.addElement(tmp);
        }
    }


    public JSTaskAtom getTask() {
        return task;
    }

    public boolean compare(JSTaskAtom toCheck) {
        return task.equals(toCheck);
    }

    public String toString(){
        return task.toString();
    }

}
