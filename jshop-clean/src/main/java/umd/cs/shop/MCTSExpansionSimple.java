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
    public Vector<MCTSNode> expand(MCTSNode node){
        JSJshopVars.expansions ++;
        JSPairPlanTState pair;
        JSPlan ans;
        JSTaskAtom t = (JSTaskAtom) node.taskNetwork().firstElement();
        JSTasks rest = node.taskNetwork().cdr();

        Vector<MCTSNode> children = new Vector<>();
        if (node.children.size() == 0) {
            if (t.isPrimitive()) {
                //task is primitive, so find applicable operators
                pair = t.seekSimplePlanCostFunction(node.tState());
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
                red = JSJshopVars.domain.methods().findAllReduction(t, node.tState().state(), red, JSJshopVars.domain.axioms());
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
                    red = JSJshopVars.domain.methods().findAllReduction(t, node.tState().state(), red, JSJshopVars.domain.axioms());
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
            pair = t.seekSimplePlanCostFunction(node.tState());
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
        red = JSJshopVars.domain.methods().findAllReduction(t, node.tState().state(), red, JSJshopVars.domain.axioms());
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


