package umd.cs.shop;

import java.io.*;

import java.util.*;


public class JSPlanningDomain {

    /*==== instance variables ====*/
    private String name;

    private JSListAxioms axioms = new JSListAxioms();

    private JSListOperators operators = new JSListOperators();

    private JSListMethods methods = new JSListMethods();

    int mctsRuns;


    /*==== constructor ====*/
    public JSPlanningDomain() {
    }

    public JSPlanningDomain(StreamTokenizer tokenizer) {

        String w = JSUtil.readWord(tokenizer, "Reading Domain Definition");
        if (w.equals("%%%"))
            throw new JSParserError(); //return;

        name = w;
        if (!JSUtil.expectTokenType(JSJshopVars.leftPar, tokenizer, "Expecting '( ' for planning domain"))
            throw new JSParserError(); //return;

        parserOpsMethsAxs(tokenizer);

        if (!JSUtil.expectTokenType(JSJshopVars.rightPar, tokenizer, " Expecting ')' for planning domain"))
            throw new JSParserError(); //return;

    }


    public void parserOpsMethsAxs(StreamTokenizer tokenizer) {

        if (!JSUtil.readToken(tokenizer, "Expecting domain definition, operators,methods,axioms"))
            throw new JSParserError(); //return;
        if (tokenizer.ttype != JSJshopVars.leftPar &&
                tokenizer.ttype != JSJshopVars.rightPar) {
            JSUtil.println("parserOpsMethsAxs: expected ( or )");
            throw new JSParserError(); //return;
        }

        String w;
        JSOperator op;
        JSMethod met;
        JSAxiom ax;

        while (tokenizer.ttype != JSJshopVars.rightPar) {
            if (!JSUtil.expectTokenType(JSJshopVars.colon, tokenizer, "Expecting ':' for axiom,method or operator definition"))
                throw new JSParserError(); //return;

            if (!JSUtil.readToken(tokenizer, "Method/axiom/operator definition expected"))
                throw new JSParserError(); //return;

            if (tokenizer.ttype == JSJshopVars.minus) {
                ax = new JSAxiom(tokenizer);
                ax.setName("Axiom_" + (axioms.size() + 1) + "_");
                axioms.addElement(ax);
            } else {
                if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
                    JSUtil.println("Line : " + tokenizer.lineno() + " method/operator expected");
                    throw new JSParserError(); //return;
                } else {
                    if (tokenizer.sval.equalsIgnoreCase("operator")) {
                        op = new JSOperator(tokenizer);
                        operators.addElement(op);
                    } else {
                        if (tokenizer.sval.equalsIgnoreCase("method")) {
                            met = new JSMethod(tokenizer);
                            met.setName("Method_" + (methods.size() + 1) + "_");
                            methods.addElement(met);

                        } else {
                            JSUtil.println("Line : " + tokenizer.lineno() + " Expecting method or operator found text:" + tokenizer.sval);
                            throw new JSParserError(); //return;

                        }
                    }
                }
            }

            if (!JSUtil.expectTokenType(JSJshopVars.rightPar, tokenizer, "Expecting ')' for Domain definition"))//close op/meth/ax
                throw new JSParserError(); //return;

            if (!JSUtil.readToken(tokenizer, "PlanningDomain expected token"))
                throw new JSParserError(); //return;

        }

    }

    public JSPairPlanTSListNodes solve(JSPlanningProblem prob, Vector<Object> listNodes) {
        JSPairPlanTState pair = new JSPairPlanTState();

        if (JSJshopVars.flagLevel > 8 && JSJshopVars.flagPlanning) {
            JSUtil.flag("====== SOLVING A NEW PROBLEM====");
            this.print();
            JSUtil.flag("PROBLEM");
            prob.print();
        }

        JSTasks tasks = prob.tasks();
        pair = tasks.seekPlan(new JSTState(prob.state(), new JSListLogicalAtoms(),
                        new JSListLogicalAtoms()),
                this, new JSPlan(), listNodes);

        return new JSPairPlanTSListNodes(pair, listNodes);
    }

    public void solveMCTS(JSPlanningProblem prob, int runs, long timeout) {
        JSTState ts = new JSTState(prob.state(), new JSListLogicalAtoms(), new JSListLogicalAtoms());
        JSTasks tasks = prob.tasks();
        JSPlan plan = new JSPlan();
        MCTSNode initial = new MCTSNode(ts, tasks, plan);
        JSJshopVars.bestPlans.addElement(initial);
        JSJshopVars.treeDepth = 0;
        this.mctsRuns = 1;
        for (int i = 1; i <= runs; i++) {
            long currentTime = System.currentTimeMillis();
            long runningTime = currentTime - JSJshopVars.startTime;
            if (runningTime >= timeout) {
                JSUtil.println("Timeout");
                break;
            }

            if (initial.fullyExplored) {
                JSUtil.println("Solved optimally");
                break;
            }
            //System.out.println(" !!!!!!! Starting Run number : " + i + " after " + (currentTime - JSJshopVars.startTime) + " ms");
            //if(!JSJshopVars.costFunction.isUnitCost()){
            MCTSAlgorithm.runMCTS(initial, this, 1);
            //}
            initial.setInTree();
            this.mctsRuns++;
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("finalUCT.dot"));
            writer.write(initial.dotTree());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSUtil.println("Found Plan: " + JSJshopVars.planFound);
        if (!JSJshopVars.planFound) {
            JSJshopVars.bestPlans.lastElement().plan.assignFailure();
        }

    }


    public JSListPairPlanTStateNodes solveAll(JSPlanningProblem prob, boolean All) {

        JSListPairPlanTStateNodes allPlans;
        JSTasks tasks = prob.tasks();
        JSTState ts = new JSTState(prob.state(), new JSListLogicalAtoms(), new JSListLogicalAtoms());
        JSPlan plan = new JSPlan();
        MCTSNode initial = new MCTSNode(ts, plan);
        allPlans = tasks.seekPlanAll(initial, this, All);

        return allPlans;
    }

    public void print() {

        JSUtil.print("(");
        JSUtil.print("make-domain ");
        JSUtil.println(name + " ");
        axioms.print();
        operators.print();
        methods.print();
        JSUtil.println(")");
    }

    public JSListMethods methods() {
        return methods;
    }

    public JSListAxioms axioms() {
        return axioms;
    }

    public JSListOperators operators() {
        return operators;
    }


    public String getName() {
        return name;
    }
}

