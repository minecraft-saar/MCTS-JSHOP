package umd.cs.shop;

import java.util.LinkedList;
import java.util.Vector;

public class MCTSExpansionPrimitive implements MCTSExpand {

    boolean recursive;
    JSJshopVars vars;

    MCTSExpansionPrimitive(boolean recursive, JSJshopVars vars) {
        this.recursive = recursive;
        this.vars = vars;
    }

    public Vector<MCTSNode> expand(MCTSNode node) {
        LinkedList<MCTSNode> toExpand = new LinkedList<>();
        Vector<MCTSNode> primitive = new Vector<>();
        toExpand.addAll(findChildren(node,recursive));
        while (!toExpand.isEmpty()){
            MCTSNode next = toExpand.removeFirst();
            if(next.taskNetwork().isEmpty()){
                primitive.add(next);
                continue;
            }
            JSTaskAtom t = (JSTaskAtom) next.taskNetwork().firstElement();
            if(t.isPrimitive){
                primitive.addAll(findChildren(next, false));
                continue;
            }
            toExpand.addAll(findChildren(next,recursive));
        }

        if (recursive && primitive.size() == 1 && !primitive.get(0).taskNetwork().isEmpty()) {
            return expand(primitive.get(0));
        }

        return primitive;
    }

    public Vector<MCTSNode> findChildren(MCTSNode node, boolean applyRecursion) {
        JSPairPlanTState pair;
        JSPlan ans;
        JSTaskAtom t = (JSTaskAtom) node.taskNetwork().firstElement();
        JSTasks rest = node.taskNetwork().cdr();

        Vector<MCTSNode> children = new Vector<>();
        if (node.children.size() == 0) {
            if (t.isPrimitive()) {
                //task is primitive, so find applicable operators
                pair = t.seekSimplePlanCostFunction(node.tState(),vars);
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
                        newTasks.addElements(rest);
                        MCTSNode child = new MCTSNode(node.tState(), newTasks, node.plan);
                        children.add(child);
                    }
                    red = vars.domain.methods().findAllReduction(t, node.tState().state(), red, vars.domain.axioms());
                }
            }
        }
        if (applyRecursion && children.size() == 1) {
            MCTSNode child = children.get(0);
            if (child.taskNetwork().isEmpty()) {
                return children;
            }
            JSTaskAtom newTask = (JSTaskAtom) child.taskNetwork().firstElement();
            if(newTask.isPrimitive){
                return children;
            }
            //JSUtil.println("Doing recursive call with task: " + child.taskNetwork().get(0).toString());
            return this.findChildren(child, applyRecursion);
        }

        return children;
    }

}


