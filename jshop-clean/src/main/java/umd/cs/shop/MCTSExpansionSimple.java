package umd.cs.shop;

import java.util.Vector;

public class MCTSExpansionSimple implements MCTSExpand{

    boolean recursive;
    boolean deadEnd;
    JSJshopVars vars;

    MCTSExpansionSimple(boolean recursive, boolean deadEnd, JSJshopVars vars){
        this.recursive = recursive;
        this.deadEnd = deadEnd;
        this.vars = vars;
    }

    @Override
    public Vector<MCTSNode> expand(MCTSNode node){
        JSPairPlanTState pair;
        JSPlan ans;
        JSTaskAtom t = (JSTaskAtom) node.taskNetwork().firstElement();
        JSTasks rest = node.taskNetwork().cdr();
        //JSUtil.println("current Task Atom: ");
        //t.print();
        //JSUtil.println("");

        Vector<MCTSNode> children = new Vector<>();
        if (node.children.size() == 0) {
            if (t.isPrimitive()) {
                //task is primitive, so find applicable operators
                pair = t.seekSimplePlanCostFunction(node.tState(), vars);
                ans = pair.plan();
                if (ans.isFailure()) {
                    node.plan.assignFailure();
                    //JSUtil.println("New dead end at depth: " + depth);
                    node.setDeadEnd(vars);
                    return children;
                } else {
                    JSPlan pl = new JSPlan();
                    pl.addElements(node.plan);
                    pl.addElements(ans);
                    MCTSNode child = new MCTSNode(pair.tState(), rest, pl);
                    children.add(child);
                    //return children;
                }
            } else {
                //Reduce task to find all applicable methods
                JSAllReduction red = new JSAllReduction();
                red = vars.domain.methods().findAllReduction(t, node.tState().state(), red, vars.domain.axioms());
                JSTasks newTasks;
                JSMethod selMet = red.selectedMethod();
                if (red.isDummy()) {
                    assert (!node.taskNetwork().isEmpty());
                    node.plan.assignFailure();
                    node.setDeadEnd(vars);
                    return children;
                }
                while (!red.isDummy()) {
                    for (int k = 0; k < red.reductions().size(); k++) {
                        newTasks = (JSTasks) red.reductions().elementAt(k);
                        //JSUtil.println("Decomposed Tasks");
                        //newTasks.print();
                        newTasks.addElements(rest);
                        MCTSNode child = new MCTSNode(node.tState(), newTasks, node.plan);
                        children.add(child);
                    }
                    red = vars.domain.methods().findAllReduction(t, node.tState().state(), red, vars.domain.axioms());
                }
            }
        }
        if(recursive && children.size() == 1){
            MCTSNode child = children.get(0);
            if(child.taskNetwork().isEmpty()){
                return children;
            }
            children.remove(child);
            //JSUtil.println("Doing recursive call with task: " + child.taskNetwork().get(0).toString());
            return this.expand(child);
        } else if(deadEnd && children.size() > 1){
            children.removeIf(child -> testDeadEnd(child));
        }
        return children;
    }

    boolean testDeadEnd(MCTSNode node) {
        JSPairPlanTState pair;
        JSPlan ans;
        JSTaskAtom t = (JSTaskAtom) node.taskNetwork().firstElement();
        JSTasks rest = node.taskNetwork().cdr();

        Vector<MCTSNode> children = new Vector<>();
        if (t.isPrimitive()) {
            //task is primitive, so find applicable operators
            pair = t.seekSimplePlanCostFunction(node.tState(),vars);
            ans = pair.plan();
            if (ans.isFailure()) {
                node.plan.assignFailure();
                //JSUtil.println("New dead end at depth: " + depth);
                node.setDeadEnd(vars);
                return true;
            }
            return false;
        }

        JSAllReduction red = new JSAllReduction();
        red = vars.domain.methods().findAllReduction(t, node.tState().state(), red, vars.domain.axioms());
        JSTasks newTasks;
        JSMethod selMet = red.selectedMethod();
        if (red.isDummy()) {
            assert (!node.taskNetwork().isEmpty());
            node.plan.assignFailure();
            node.setDeadEnd(vars);
            return true;
        }
        return false;

    }
}


