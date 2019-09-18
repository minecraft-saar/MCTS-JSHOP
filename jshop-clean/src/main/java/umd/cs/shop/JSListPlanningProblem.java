package umd.cs.shop;

import java.util.*;

public class JSListPlanningProblem extends Vector<Object> {

    JSListPlanningProblem() {
        super();
    }


    public void print() {

        JSPlanningProblem el;
        for (short i = 0; i < this.size(); i++) {
            el = (JSPlanningProblem) this.elementAt(i);
            el.print();
        }
    }
}
