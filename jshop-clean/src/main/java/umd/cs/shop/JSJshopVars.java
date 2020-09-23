package umd.cs.shop;

import umd.cs.shop.costs.CostFunction;

import java.util.Random;
import java.util.Vector;

public class JSJshopVars {

    /* class variables */


    int VarCounter;
    //static boolean flagParser = false;// if true flags will appear
    // when parsing the file
    //static boolean flagPlanning = false;// if true flags will appear
    // when planning the file
    static int flagLevel = 0; // the higher the more verbose the output
    boolean allPlans = false;

    /* Table for the parser */
    static final int leftPar = 0x0028;  //(
    static final int rightPar = 0x0029; //)
    static final int apostrophe = 0x0027; //'
    static final int colon = 0x003A;      //:
    static final int dot = 0x002E;      //.
    static final int semicolon = 0x003B; //;
    static final int exclamation = 0x0021; //!
    static final int interrogation = 0x003F; //?
    static final int minus = 0x002D; //-
    static final int equalT = 0x003D; //=
    static final int greaterT = 0x003E; //>
    static final int lessT = 0x003C; //<
    static final int coma = 0x002C; //,
    static final int astherisk = 0x002A; //*
    static final int rightBrac = 0x005D; //[
    static final int leftBrac = 0x005B;//]
    static final int verticalL = 0x007C; //|
    static final int plus = 0x002B; //+
    static final int whiteSpace = 0x0020;//
    static final int percent = 0x0025;//
    static final int backquote = 96;//
    static final int slash = 47;//
    static final int lowLine = 0x005F;// _

    //Any addition/change of a character implies a change
    // in the following methods in JSUtil:
    // stringTokenizer/1, initParseTable/1 and printTokenizer/1

    JSJshopVars(boolean bbPruning, boolean approx, boolean random, boolean print, boolean landmarks){
        this.bbPruning = bbPruning;
        this.useApproximatedCostFunction = approx;
        this.random = random;
        this.bestPlans = new Vector<>();
        this.planFound = false;
        this.bestCost = Double.POSITIVE_INFINITY;
        this.treeDepth = 0;
        this.mctsRuns = 1;
        this.VarCounter = 0;
        this.print = print;
        this.landmarks = landmarks;
    }

    long startTime;
    int treeDepth;
    int mctsRuns;
    boolean print;

    double bestCost;

    boolean planFound;
    boolean random;
    boolean useApproximatedCostFunction;
    boolean bbPruning;
    boolean landmarks;

    Vector<JSPlan> bestPlans;
    CostFunction costFunction;
    MCTSPolicy policy;
    MCTSExpand expansionPolicy;
    MCTSSimulation simulationPolicy;
    Registry registry;
    Random randomGenerator;
    JSPlanningDomain domain;

    void foundPlan(JSPlan plan, int depth){
        //Get current best reward if it exists
        Double foundCost = plan.planCost();
        long currentTime = System.currentTimeMillis();
        //bestPlans.addElement(plan);
        //JSUtil.println("Found plan of cost " + foundCost + " in run " + mctsRuns + " after " + (currentTime - startTime) + " ms at depth " + depth);
        if (!planFound) {
            planFound = true;
            if(print)
                JSUtil.println("Found first plan of cost " + foundCost + " in run " + mctsRuns + " after " + (currentTime - startTime) + " ms at depth " + depth);
            bestCost = foundCost;
            this.bestPlans.addElement(plan);
        } else if (foundCost.compareTo(bestCost) < 0) {
            this.bestPlans.addElement(plan);
            bestCost = foundCost;
            if(print)
                JSUtil.println("Found better plan of cost " + foundCost + " in run " + mctsRuns + " after " + (currentTime - startTime) + " ms at depth " + depth);
        }
    }

    public void initRandGen(int randomSeed){
        this.randomGenerator = new Random(randomSeed);
    }

    public static int combineHashCodes(int h1, int h2) {
        return (((h1 << 5) + h1) ^ h2);
    }

    //static void SetAllPlans(boolean val) { allPlans = val;}

    //static void SetFlagLevel(int val) {this.flagLevel = val;}


    public boolean bb_pruning(double planCost) {   return this.bbPruning && planCost >= bestCost;}
}
