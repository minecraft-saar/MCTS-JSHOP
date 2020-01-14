package umd.cs.shop;

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

public class MCTSSimulationFast implements MCTSSimulation {

    int budget_recursive;

    MCTSSimulationFast(int budget_recursive) {
        this.budget_recursive = budget_recursive;
    }

    @Override
    public double simulation(MCTSNode current, int depth ) {
        Plan plan;
        if(JSJshopVars.random || JSJshopVars.mctsRuns > 1) {
            plan = this.random_simulation(current.tState(), current.taskNetwork(), depth, budget_recursive);
        } else {
            plan = this.deterministic_simulation(current.tState(), current.taskNetwork(), depth, budget_recursive);
        }
        if (plan.isPlan()) {
            JSPlan realPlan = new JSPlan();
            realPlan.addElements(current.plan);
            realPlan.addElements(plan.plan);

            JSJshopVars.FoundPlan(realPlan, plan.depth);
            return  plan.cost;
        }

        return Double.POSITIVE_INFINITY;
    }

    class Plan {
        double cost;
        int depth;
        JSPlan plan;

        Plan(int depth) {
            this.depth = depth;
            this.cost = 0;
            this.plan = new JSPlan();
        }

        Plan() {
            this.cost = Double.POSITIVE_INFINITY;
        }

        boolean isPlan() {
            return this.cost != Double.POSITIVE_INFINITY;
        }

        public void addWithCost(JSTaskAtom action, double cost) {
            plan.addWithCost(action, cost);
            this.cost += cost;
        }
    }

    public Plan deterministic_simulation(JSTState currentState, JSTasks currentTasks, int depth, Integer budget) {
        if (currentTasks.isEmpty()) {
            return new Plan(depth);
        }

        JSTaskAtom task = (JSTaskAtom) currentTasks.firstElement();
        JSTasks rest = currentTasks.cdr();

        if (task.isPrimitive()) {
            boolean groundedTask = task.isGround();
            //task is primitive, so find applicable operators
            for (Object obj : JSJshopVars.domain.operators()) {
                JSOperator op = (JSOperator) obj;
                JSTaskAtom head = op.head();
                if (!groundedTask) {
                    head = head.standarizerTA();
                }
                JSSubstitution alpha = head.matches(task);
                if (!alpha.fail()) {
                    op = op.standarizerOp();
                    alpha = alpha.standarizerSubs();
                    JSJshopVars.VarCounter++;

                    JSListSubstitution satisfiers = JSJshopVars.domain.axioms().TheoremProver(op.precondition(), currentState.state, alpha, true);
                    if (!satisfiers.isEmpty()) {
                        JSTState newState = currentState.state.applyOp(op, alpha, currentState.addList(), currentState.deleteList());

                        Plan result = this.deterministic_simulation(newState, rest, depth+1, budget);
                        if (result.isPlan()) {
                            head = op.head();
                            double cost;
                            if(JSJshopVars.useApproximatedCostFunction){
                                cost = JSJshopVars.costFunction.approximate(currentState, op, head.applySubstitutionTA(alpha));
                                JSJshopVars.approxUses++;
                            } else {
                                cost = JSJshopVars.costFunction.realCost(currentState, op, head.applySubstitutionTA(alpha));
                                JSJshopVars.realCostUses++;
                            }
                            result.addWithCost(head.applySubstitutionTA(alpha), cost);
                            return result;
                        } else {
                            budget--;
                            if (budget == 0) {
                                return new Plan();
                            }
                        }
                    }
                }
            }
        } else {
            //Reduce task to find all applicable methods
            JSAllReduction red = new JSAllReduction();
            red = JSJshopVars.domain.methods().findAllReduction(task, currentState.state(), red, JSJshopVars.domain.axioms());
            JSTasks newTasks;
            JSMethod selMet = red.selectedMethod();
            if (red.isDummy()) {
                return new Plan();
            }
            while (!red.isDummy()) {
                for (int k = 0; k < red.reductions().size(); k++) {
                    newTasks = (JSTasks) red.reductions().elementAt(k);
                    newTasks.addElements(rest);
                    Plan result = this.deterministic_simulation(currentState, newTasks, depth + 1, budget);
                    if (result.isPlan()) {
                        return result;
                    } else {
                        budget--;
                        if (budget == 0) {
                            return new Plan();
                        }
                    }
                }
                red = JSJshopVars.domain.methods().findAllReduction(task, currentState.state(), red, JSJshopVars.domain.axioms());
            }
        }

        return new Plan(); //Dead end
    }


    public Plan random_simulation(JSTState currentState, JSTasks currentTasks, int depth, Integer budget) {
        if (currentTasks.isEmpty()) {
            return new Plan(depth);
        }

        JSTaskAtom task = (JSTaskAtom) currentTasks.firstElement();
        JSTasks rest = currentTasks.cdr();

        if (task.isPrimitive()) {
            boolean groundedTask = task.isGround();
            //task is primitive, so find applicable operators
            //NOTE: We do not randomize the order because typically there is a single operator available
            for (Object obj : JSJshopVars.domain.operators()) {
                JSOperator op = (JSOperator) obj;
                JSTaskAtom head = op.head();
                if (!groundedTask) {
                    head = head.standarizerTA();
                }
                JSSubstitution alpha = head.matches(task);
                if (!alpha.fail()) {

                    op = op.standarizerOp();
                    alpha = alpha.standarizerSubs();
                    JSJshopVars.VarCounter++;

                    JSListSubstitution satisfiers = JSJshopVars.domain.axioms().TheoremProver(op.precondition(), currentState.state, alpha, true);
                    if (!satisfiers.isEmpty()) {
                        JSTState newState = currentState.state.applyOp(op, alpha, currentState.addList(), currentState.deleteList());

                        Plan result = this.random_simulation(newState, rest, depth+1, budget);
                        if (result.isPlan()) {
                            head = op.head();
                            double cost;
                            if(JSJshopVars.useApproximatedCostFunction){
                                cost = JSJshopVars.costFunction.approximate(currentState, op, head.applySubstitutionTA(alpha));
                                JSJshopVars.approxUses++;
                            } else {
                                cost = JSJshopVars.costFunction.realCost(currentState, op, head.applySubstitutionTA(alpha));
                                JSJshopVars.realCostUses++;
                            }
                            result.addWithCost(head.applySubstitutionTA(alpha), cost);
                            return result;
                        } else {
                            if (budget-- == 0) {
                                return new Plan();
                            }
                        }
                    }
                }
            }
        } else {
            //Reduce task to find all applicable methods
            Vector<JSTasks> reductions = new Vector<JSTasks>();
            JSAllReduction red = JSJshopVars.domain.methods().findAllReduction(task, currentState.state(), new JSAllReduction(), JSJshopVars.domain.axioms());
            while (!red.isDummy()) {
                reductions.addAll(red.reductions());
                red = JSJshopVars.domain.methods().findAllReduction(task, currentState.state(), red, JSJshopVars.domain.axioms());
            }
            Collections.shuffle(reductions);
            JSMethod selMet = red.selectedMethod();
            for (JSTasks newTasks : reductions) {
               newTasks.addElements(rest);
               Plan result = this.random_simulation(currentState, newTasks, depth + 1, budget);
               if (result.isPlan()) {
                   return result;
               } else {
                   if (budget-- == 0) {
                       return new Plan();
                   }
               }
            }
        }

        return new Plan(); //Dead end
    }
}






