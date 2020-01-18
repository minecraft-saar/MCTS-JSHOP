package umd.cs.shop;

import umd.cs.shop.costs.CostFunction;

import java.util.Vector;

public final class JSJshopVars {

    /* class variables */


    static char LastCharRead;
    static int VarCounter;
    public static String errorMsg = null;
    static boolean flagParser = false;// if true flags will appear
    // when parsing the file
    static boolean flagPlanning = false;// if true flags will appear
    // when planning the file
    static int flagLevel = 0; // the higher the more verbose the output
    static boolean flagExit = true;
    static boolean allPlans = false;
    /* Table for the parser */

    static int leftPar = 0x0028;  //(
    static int rightPar = 0x0029; //)
    static int apostrophe = 0x0027; //'
    static int colon = 0x003A;      //:
    static int dot = 0x002E;      //.
    static int semicolon = 0x003B; //;
    static int exclamation = 0x0021; //!
    static int interrogation = 0x003F; //?
    static int minus = 0x002D; //-
    static int equalT = 0x003D; //=
    static int greaterT = 0x003E; //>
    static int lessT = 0x003C; //<
    static int coma = 0x002C; //,
    static int astherisk = 0x002A; //*
    static int rightBrac = 0x005D; //[
    static int leftBrac = 0x005B;//]
    static int verticalL = 0x007C; //|
    static int plus = 0x002B; //+
    static int whiteSpace = 0x0020;//
    static int percent = 0x0025;//  
    static int backquote = 96;//  
    static int slash = 47;//
    static int lowLine = 0x005F;// _

    //Any addition/change of a character implies a change
    // in the following methods in JSUtil:
    // stringTokenizer/1, initParseTable/1 and printTokenizer/1


    static long startTime;
    static int treeDepth;
    static boolean planFound = false;
    static boolean updateMaximum = false;
    static boolean useApproximatedCostFunction = false;
    static CostFunction costFunction;
    static MCTSPolicy policy;
    //static JSPairTStateTasks stateBestPlan;
    static Vector<JSPlan> bestPlans = new Vector<>();
    static double bestCost = Double.POSITIVE_INFINITY;
    static int approxUses = 0;
    static int realCostUses = 0;
    static boolean random = true;
    static boolean useFullyExplored = true;
    static  MCTSExpand expansionPolicy;
    static MCTSSimulation simulationPolicy;
    static Registry registry;
    static boolean perform_bb_pruning;
    static boolean perform_bb_pruning_fast;
    static double explorationFactor = java.lang.Math.sqrt(2);

    static int mctsRuns = 1;
    static int expansions = 0;


    static JSPlanningDomain domain;

    static void FoundPlan(JSPlan plan, int depth){
        //Get current best reward if it exists
        Double foundCost = plan.planCost();
        long currentTime = System.currentTimeMillis();
        if (!planFound) {
            planFound = true;
            JSUtil.println("Found first plan of cost " + foundCost + " in run " + mctsRuns + " after " + (currentTime - startTime) + " ms at depth " + depth);
            bestCost = foundCost;
        } else if (foundCost.compareTo(bestCost) < 0) {
            bestPlans.addElement(plan);
            bestCost = foundCost;

            JSUtil.println("Found better plan of cost " + foundCost + " in run " + mctsRuns + " after " + (currentTime - startTime) + " ms at depth " + depth);
        }
    }


    public static int combineHashCodes(int h1, int h2) {
        return (((h1 << 5) + h1) ^ h2);
    }

    static void SetAllPlans(boolean val) {
        allPlans = val;
    }

    static void SetFlagExit(boolean val) {
        flagExit = val;
    }

    static void SetFlagLevel(int val) {
        flagLevel = val;
    }


    public static boolean bb_pruning(double planCost) {
        return perform_bb_pruning && planCost >= bestCost;
    }
}
