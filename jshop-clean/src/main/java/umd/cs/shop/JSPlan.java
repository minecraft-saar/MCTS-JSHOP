package umd.cs.shop;

import java.util.*;


public class JSPlan extends JSTasks {

    /*==== instance variables ====*/

    private boolean isFailure;
    Vector<Object> costs;
    double planCost;
    int depth;

    JSPlan() {

        super();

        isFailure = false;
        costs = new Vector<>();
        planCost = 0;

    }

    public void setDepth(int depth) { this.depth = depth;}

    public void assignFailure() {
        isFailure = true;
    }

    public boolean isFailure() {
        return isFailure;
    }

    public void addElements(JSPlan pl) {
        for (short i = 0; i < pl.predicates.size(); i++) {
            this.predicates.addElement(pl.predicates.elementAt(i));
            // Added in May 2
            costs.addElement(pl.costs.elementAt(i));
        }
        planCost += pl.planCost;
    }

    public void addElementsRev(JSPlan pl) {
        for (int i = pl.predicates.size()-1; i >= 0; i--) {
            this.predicates.addElement(pl.predicates.elementAt(i));
            // Added in May 2
            costs.addElement(pl.costs.elementAt(i));
        }
        planCost += pl.planCost;
    }

    public void printPlan() {
        JSTaskAtom t;
        JSUtil.print(" ( ");
        for (short i = 0; i < this.predicates.size(); i++) {
            t = (JSTaskAtom) this.predicates.elementAt(i);
            t.print();
            // Added in May 2
            JSUtil.print(" " + (String) this.costs.elementAt(i) + " ");
        }
        JSUtil.println(" ) ");
    }

    public void addWithCost(JSTaskAtom t, double cost) {

        this.predicates.addElement(t);
        this.costs.addElement(String.valueOf(cost));
        planCost += cost;
    }

    public void insertWithCost(int place, JSTaskAtom t, double cost) {
        this.predicates.insertElementAt(t, place);
        costs.insertElementAt(String.valueOf(cost), place);
        planCost += cost;
    }

    public double elementCost(int i) {
        return Double.parseDouble((String) costs.elementAt(i));
    }

    public double planCost() {
        return planCost;
    }
}

