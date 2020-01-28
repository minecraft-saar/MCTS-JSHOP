package umd.cs.shop;

import java.util.Collections;
import java.util.Vector;

public class MCTSSimulationFast implements MCTSSimulation {

    int budget_recursive;
    int available_budget;

    JSJshopVars vars;
    double prune_bound;

    MCTSSimulationFast(int budget_recursive, JSJshopVars vars) {
        this.budget_recursive = budget_recursive;
        this.prune_bound = Double.POSITIVE_INFINITY;
        this.vars = vars;
    }

    MCTSSimulationFast(int budget_recursive, double prune_bound) {
        this.budget_recursive = budget_recursive;
        this.prune_bound = prune_bound;
    }


    @Override
    public double simulation(MCTSNode current, int depth) {
        JSPlan plan ;
        this.available_budget = this.budget_recursive;
        if (vars.random || vars.mctsRuns > 1) {
            plan = this.random_simulation(current.tState(), current.taskNetwork(), depth, current.plan.planCost());
        } else {
            plan = this.deterministic_simulation(current.tState(), current.taskNetwork(), depth, current.plan.planCost());
        }
        if (!plan.isFailure()) {
            JSPlan realPlan = new JSPlan();
            realPlan.addElements(current.plan);
            realPlan.addElementsRev(plan);
            realPlan.setDepth(plan.depth);
            vars.foundPlan(realPlan, realPlan.depth);
            return plan.planCost();
        }

        return Double.POSITIVE_INFINITY;
    }

    /*
    class Plan {
        double cost;
        int depth;
        JSPlan plan;

        Plan(int depth) {
            this.depth = depth;
            this.cost = 0;
            this.plan = new JSPlan();
        }

        //Plan() {this.cost = Double.POSITIVE_INFINITY;}

        boolean isPlan() {
            return this.cost != Double.POSITIVE_INFINITY;
        }

        public void addWithCost(JSTaskAtom action, double cost) {
            plan.addWithCost(action, cost);
            this.cost += cost;
        }
    }*/

    public double action_cost(JSOperator op, JSSubstitution alpha, JSTState currentState) {
        JSTaskAtom head = op.head();
        return vars.costFunction.getCost(currentState, op, head.applySubstitutionTA(alpha), vars.useApproximatedCostFunction);
    }

    public JSPlan deterministic_simulation(JSTState currentState, JSTasks currentTasks, int depth, double plan_cost) {
        if (currentTasks.isEmpty()) {
            JSPlan ret = new JSPlan();
            ret.setDepth(depth);
            return ret;
        }

        JSTaskAtom task = (JSTaskAtom) currentTasks.firstElement();
        JSTasks rest = currentTasks.cdr();

        if (task.isPrimitive()) {
            boolean groundedTask = task.isGround();
            //task is primitive, so find applicable operators
            for (Object obj : vars.domain.operators()) {
                JSOperator op = (JSOperator) obj;
                JSTaskAtom head = op.head();
                if (!groundedTask) {
                    head = head.standarizerTA(vars);
                }
                JSSubstitution alpha = head.matches(task);
                if (!alpha.fail()) {
                    op = op.standarizerOp(vars);
                    alpha = alpha.standarizerSubs(vars);
                    vars.VarCounter++;

                    JSListSubstitution satisfiers = vars.domain.axioms().TheoremProver(op.precondition(), currentState.state, alpha, true);
                    if (!satisfiers.isEmpty()) {
                        JSTState newState = currentState.state.applyOp(op, alpha, currentState.addList(), currentState.deleteList());
                        double action_cost = this.action_cost(op, alpha, currentState);
                        JSPlan result = this.deterministic_simulation(newState, rest, depth + 1, plan_cost + action_cost);
                        if (!result.isFailure()) {
                            result.addWithCost(op.head().applySubstitutionTA(alpha), action_cost);
                            return result;
                        } else {
                            this.available_budget--;
                            if (this.available_budget <= 0) {
                                return result;
                            }
                        }
                    }
                }
            }
        } else {
            //Reduce task to find all applicable methods
            JSAllReduction red = new JSAllReduction();
            red = vars.domain.methods().findAllReduction(task, currentState.state(), red, vars.domain.axioms());
            JSTasks newTasks;
            JSMethod selMet = red.selectedMethod();
            if (red.isDummy()) {
                JSPlan fail = new JSPlan();
                fail.assignFailure();
                return fail;
            }
            while (!red.isDummy()) {
                for (int k = 0; k < red.reductions().size(); k++) {
                    newTasks = (JSTasks) red.reductions().elementAt(k);
                    newTasks.addElements(rest);
                    JSPlan result = this.deterministic_simulation(currentState, newTasks, depth + 1, plan_cost);
                    if (!result.isFailure()) {
                        return result;
                    } else {
                        this.available_budget--;
                        if (this.available_budget <= 0) {
                            JSPlan fail = new JSPlan();
                            fail.assignFailure();
                            return fail;
                        }
                    }
                }
                red = vars.domain.methods().findAllReduction(task, currentState.state(), red, vars.domain.axioms());
            }
        }
        JSPlan fail = new JSPlan();
        fail.assignFailure();
        return fail; //Dead end
    }


    public JSPlan random_simulation(JSTState currentState, JSTasks currentTasks, int depth, double plan_cost) {
        if (currentTasks.isEmpty()) {
            JSPlan success = new JSPlan();
            success.setDepth(depth);
            return success;
        }
        if (plan_cost >= this.prune_bound) {
            JSPlan fail = new JSPlan();
            fail.assignFailure();
            return fail;
        }


        JSTaskAtom task = (JSTaskAtom) currentTasks.firstElement();
        JSTasks rest = currentTasks.cdr();

        if (task.isPrimitive()) {
            boolean groundedTask = task.isGround();
            //task is primitive, so find applicable operators
            //NOTE: We do not randomize the order because typically there is a single operator available
            for (Object obj : vars.domain.operators()) {
                JSOperator op = (JSOperator) obj;
                JSTaskAtom head = op.head();
                if (!groundedTask) {
                    head = head.standarizerTA(vars);
                }
                JSSubstitution alpha = head.matches(task);
                if (!alpha.fail()) {

                    op = op.standarizerOp(vars);
                    alpha = alpha.standarizerSubs(vars);
                    vars.VarCounter++;

                    JSListSubstitution satisfiers = vars.domain.axioms().TheoremProver(op.precondition(), currentState.state, alpha, true);
                    if (!satisfiers.isEmpty()) {
                        JSTState newState = currentState.state.applyOp(op, alpha, currentState.addList(), currentState.deleteList());
                        double action_cost = this.action_cost(op, alpha, currentState);
                        JSPlan result = this.random_simulation(newState, rest, depth + 1, plan_cost + action_cost);
                        if (!result.isFailure()) {
                            head = op.head();
                            result.addWithCost(head.applySubstitutionTA(alpha), action_cost);
                            return result;
                        } else {
                            this.available_budget--;
                            if (this.available_budget <= 0) {
                                JSPlan fail = new JSPlan();
                                fail.assignFailure();
                                return fail;
                            }
                        }
                    }
                }
            }
        } else {
            //Reduce task to find all applicable methods
            Vector<JSTasks> reductions = new Vector<JSTasks>();
            JSAllReduction red = vars.domain.methods().findAllReduction(task, currentState.state(), new JSAllReduction(), vars.domain.axioms());
            while (!red.isDummy()) {
                reductions.addAll(red.reductions());
                red = vars.domain.methods().findAllReduction(task, currentState.state(), red, vars.domain.axioms());
            }
            Collections.shuffle(reductions, vars.randomGenerator);
            JSMethod selMet = red.selectedMethod();
            for (JSTasks newTasks : reductions) {
                newTasks.addElements(rest);
                JSPlan result = this.random_simulation(currentState, newTasks, depth + 1, plan_cost);
                if (!result.isFailure()) {
                    return result;
                } else {
                    this.available_budget--;
                    if (this.available_budget <= 0) {
                        JSPlan fail = new JSPlan();
                        fail.assignFailure();
                        return fail;
                    }
                }
            }
        }

        JSPlan fail = new JSPlan();
        fail.assignFailure();
        return fail; //Dead end
    }
}






