package umd.cs.shop;

import java.util.Vector;

public class MCTSExpansionSimple implements MCTSExpand{

    boolean recursive;
    boolean deadEnd;

    MCTSExpansionSimple(boolean recursive, boolean deadEnd){
        this.recursive = recursive;
        this.deadEnd = deadEnd;
    }

    @Override
    public Vector<MCTSNode> expand(MCTSNode node, JSPlanningDomain dom){
        JSPairPlanTState pair;
        JSPlan ans;
        JSTaskAtom t = (JSTaskAtom) node.taskNetwork().firstElement();
        JSTasks rest = node.taskNetwork().cdr();
        rest.removeElement(t);
        Vector<MCTSNode> children = new Vector<>();
        if (node.children.size() == 0) {
            if (t.isPrimitive()) {
                //task is primitive, so find applicable operators
                pair = t.seekSimplePlanCostFunction(dom, node.tState(), false);
                ans = pair.plan();
                if (ans.isFailure()) {
                    node.plan.assignFailure();
                    //JSUtil.println("New dead end at depth: " + depth);
                    node.setDeadEnd();
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
                red = dom.methods().findAllReduction(t, node.tState().state(), red, dom.axioms());
                JSTasks newTasks;
                JSMethod selMet = red.selectedMethod();
                if (red.isDummy()) {
                    assert (!node.taskNetwork().isEmpty());
                    node.plan.assignFailure();
                    node.setDeadEnd();
                    return children;
                }
                while (!red.isDummy()) {
                    for (int k = 0; k < red.reductions().size(); k++) {
                        newTasks = (JSTasks) red.reductions().elementAt(k);
                        newTasks.addElements(rest);
                        MCTSNode child = new MCTSNode(node.tState(), newTasks, node.plan);
                        children.add(child);
                    }
                    red = dom.methods().findAllReduction(t, node.tState().state(), red, dom.axioms());
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
            return this.expand(child, dom);
        } else if(deadEnd && children.size() > 1){
            children.removeIf(child -> testDeadEnd(child, dom));
            if(children.isEmpty()){

                JSUtil.println("ALL CHILDREN REMOVED");
            }
        }
        return children;
    }

    boolean testDeadEnd(MCTSNode node, JSPlanningDomain dom) {
        JSPairPlanTState pair;
        JSPlan ans;
        JSTaskAtom t = (JSTaskAtom) node.taskNetwork().firstElement();
        JSTasks rest = node.taskNetwork().cdr();
        rest.removeElement(t);
        Vector<MCTSNode> children = new Vector<>();
        if (t.isPrimitive()) {
            //task is primitive, so find applicable operators
            pair = t.seekSimplePlanCostFunction(dom, node.tState(), false);
            ans = pair.plan();
            if (ans.isFailure()) {
                node.plan.assignFailure();
                //JSUtil.println("New dead end at depth: " + depth);
                node.setDeadEnd();
                return true;
            }
            return false;
        }

        JSAllReduction red = new JSAllReduction();
        red = dom.methods().findAllReduction(t, node.tState().state(), red, dom.axioms());
        JSTasks newTasks;
        JSMethod selMet = red.selectedMethod();
        if (red.isDummy()) {
            assert (!node.taskNetwork().isEmpty());
            node.plan.assignFailure();
            node.setDeadEnd();
            return true;
        }
        return false;

    }
}

