package umd.cs.shop;

import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import umd.cs.shop.costs.CostFunction;
import umd.cs.shop.costs.EstimationCost;
import umd.cs.shop.costs.NLGCost;


/*HICAP import nrl.aic.hicap.*;*/

//The main constructor of this class is JSJshop(String nameFile). This
//constructor will open and parse the <file> named nameFile and solves the 
//problem  stated in it. It assumes that the file contains a single problem
// i.e., "make-problem" and a single domain, i.e., "make-domain". If several
// domains and problems are given it will parse all of them and solve the
// last problem for the last domain.

public final class JSJshop implements Runnable {

    @Parameters(index = "0", description = "The domain file")
    String nameDomainFile;

    @Parameters(index = "1", description = "The problem file")
    String nameProblemFile;

    @Option(names = {"-s"}, defaultValue = "false", description = "Enables standard search")
    boolean standardSearch;

    @Option(names = {"--printTree"}, defaultValue = "false", description = "Enable printing of tree")
    boolean printTree;

    @Option(names = {"--noRec"}, defaultValue = "true", description = "Disables collapsing nodes")
    boolean recursive;

    @Option(names = {"--noRandom"}, defaultValue = "true", description = "If present first run will not be random and instead follow the domain ordering")
    boolean random;

    @Option(names = {"-e", "--expansionPolicy"}, defaultValue = "simple", description = "")
    String expansionPolicy;

    @Option(names = {"-m", "--monteCarloRuns"}, defaultValue = "10000", description = "Number of runs for the monte carlo search")
    int mctsruns;

    @Option(names = {"--min"}, defaultValue = "false", description = "take minimum when updating the reward")
    boolean updateMinimum;

    @Option(names = {"--approx"}, defaultValue = "false", description = "HAS NO EFFECT since nlg can not do approximations yet")
    boolean useApproximatedCostFunction;

    //@Option(names = {"-p", "--policy"}, defaultValue = "uct1", description = "can be uct1 or uct2")
    //String policy;

    @Option(names = {"-t", "--timeout"}, defaultValue = "30000", description = "Timeout in milliseconds")
    long timeout;

    @Option(names = {"-c", "--costFunction"}, defaultValue = "BASIC", description = "Which cost function should be used:  ${COMPLETION-CANDIDATES}")
    CostFunction.CostFunctionType costFunctionName;

    @Option(names = {"--level"}, defaultValue = "NONE", description = "Which instruction level should be used:  ${COMPLETION-CANDIDATES}")
    CostFunction.InstructionLevel level;

    @Option(names = {"-d", "--detail"}, defaultValue = "1", description = "Integer from 1 to 10 for more details during standard search")
    int detail;

    @Option(names = {"-a", "--all"}, defaultValue = "false", description = "Find all plans in standard search")
    boolean allPlans;

    @Option(names = {"--duplicateDetection"}, defaultValue = "false", description = "Deprecated  was intended for duplicate Detection")
    boolean duplicate;

    @Option(names = {"--fastSimulation"}, defaultValue = "false", description = "The states generated in simulations are not saved")
    boolean fastSimulation;


    @Option(names = {"-b", "--BBpruning"}, defaultValue = "false", description = "Use branch-and-bound pruning (i.e. prune against best solution found so far)")
    boolean bbPruning;

    @Option(names = {"-bf", "--BBpruningFast"}, defaultValue = "false", description = "Use branch-and-bound pruning (i.e. prune against best solution found so far)")
    boolean bbPruningFast;

    @Option(names = {"-exp", "--explorationFactor"}, defaultValue = "1.41421", description = "exploration factor used by mcts")
    double explorationFactor;

    @Option(names = {"--recursiveSimulation"}, defaultValue = "0", description = "")
    int recursiveSimulationBudget;

    @Option(names = {"--randomSeed"}, defaultValue = "42", description = "use this number as seed for random Generator")
    int randomSeed;

    @Option(names = {"-l", "--landmarks"}, defaultValue = "false", description = "Use landmarks")
    boolean landmarks;

    @Option(names = {"-lf", "--landmarkFile"}, defaultValue = "", description = "Name of Landmark File")
    String landmarkFile;

    @Option(names = {"-wf", "--weightsFile"}, defaultValue = "", description = "Json File for NLG weights")
    String weightsFile;

    @Option(names = {"-p", "--planFile"}, defaultValue = "NoFile.plan", description = "Output File containing Plan")
    String planFile;

    @Option(names = {"-nc", "--numStructs"}, defaultValue = "3", description = "How many special structures the scenario has")
    int numStructs;

    @Option(names = {"-nnp", "--nnPath"}, defaultValue = "src/main/java/umd/cs/shop/costs/models/trained_model.zip", description = "Path to the trained NN model")
    String nnPath;

    @Option(names = {"-cmp", "--compare"}, defaultValue = "false", description = "Whether to compare results between NN and NLG system during an NN run")
    boolean compare;

    @Option(names = {"-tar", "--useTarget"}, defaultValue = "false", description = "Whether to use information of the current target for cost estimation (only for NNs)")
    boolean useTarget;

    @Option(names = {"-str", "--useStructures"}, defaultValue = "false", description = "Whether to use information of existing structures for cost estimation (only for NNs)")
    boolean useStructures;

    @Option(names = {"-st", "--scenarioType"}, defaultValue = "SimpleBridge", description = "What kind of scenario is being used")
    EstimationCost.ScenarioType scenarioType;

    @Override
    public void run() {
        /*JSPlan plan = nlgSearch(mctsruns, timeout);
        if(plan != null){
            return;
        }*/
        JSJshopVars variables = new JSJshopVars(bbPruning, useApproximatedCostFunction, random, true, landmarks, planFile);
        JSUtil.println(variables.planFile);
        variables.startTime = System.currentTimeMillis();
        variables.initRandGen(this.randomSeed);
        JSUtil.println("Reading file " + nameDomainFile);
        if (!parserFile(nameDomainFile))
            System.exit(0);
        domain.axioms.setVars(variables);
        variables.domain = domain;
        JSUtil.println("Domain file parsed successfully");
        JSUtil.println("Reading file " + nameProblemFile);
        if (!parserFile(nameProblemFile))
            System.exit(0);

        JSUtil.println("Problem file parsed successfully");
        final long parseTime = System.currentTimeMillis();
        JSUtil.println("Parsing Time: " + (parseTime - variables.startTime));
        if (level == CostFunction.InstructionLevel.NONE)
            variables.costFunction = CostFunction.getCostFunction(costFunctionName, variables.domain.getName());
        else
            variables.costFunction = CostFunction.getCostFunction(costFunctionName, variables.domain.getName(), level, weightsFile, numStructs, nnPath, compare, useTarget, useStructures, scenarioType);

        if (landmarks) {
            JSUtil.println("Starting landmark parsing");
            if (!parserFileLandmarks(landmarkFile))
                System.exit(0);
            JSUtil.println("Initial Fact Landmarks: ");
            JSUtil.println(initialFactLandmarks.toString());
            JSUtil.println("initial Task Landmarks: ");
            JSUtil.println(initialTaskLandmarks.toString());

        }


        if (standardSearch) {
            standardSearch(variables);
        } else {
            mctsSearch(variables);
        }

    }

    public static void main(String[] args) {
        JSJshop j = new JSJshop();
        CommandLine cmd = new CommandLine(j).setCaseInsensitiveEnumValuesAllowed(true);
        cmd.parseArgs(args);
        j.run();

        //CommandLine.run(new JSJshop(), args);
    }

    public JSPlanningProblem prob;

    JSPlanningDomain domain;

    Set<JSFactLandmark> initialFactLandmarks;
    Set<JSTaskLandmark> initialTaskLandmarks;

    private Vector<JSPlanningProblem> probSet = new Vector<>();

    private JSPlan sol;

    private JSJshopNode tree = null;

    private JSPairPlanTSListNodes solution;

    /*HICAP:    private NeoEditor aNeoEditor;*/
    /*====  main ====*/

    /* constructors */

    //public
    //  JSJshop()    {    }


    /******** main constructor **********/


    //MCTS
    public void mctsSearch(JSJshopVars vars) {
        if (explorationFactor == 1.41421) {
            explorationFactor = java.lang.Math.sqrt(2);
        }
        vars.policy = MCTSPolicy.getPolicy(vars, updateMinimum, explorationFactor);
        vars.expansionPolicy = MCTSExpand.getPolicy(expansionPolicy, recursive, vars);
        vars.simulationPolicy = MCTSSimulation.getPolicy(!fastSimulation, recursiveSimulationBudget, bbPruning, bbPruningFast, vars);

        if (duplicate) {
            vars.registry = new Registry();
        }

        for (int k = 0; k < probSet.size(); k++) {
            prob = (JSPlanningProblem) probSet.elementAt(k);
            if (vars.print) {
                JSUtil.println("Solving Problem :" + prob.Name() + " with mcts");
                JSUtil.println("time till timeout: " + timeout);
            }
            if (landmarks) {
                prob.state().factLandmarks = initialFactLandmarks;
                for (JSPredicateForm pred : prob.state().atoms) {
                    prob.state().factLandmarks.removeIf(landmark -> landmark.compare(pred, true));
                }
                prob.state().taskLandmarks = initialTaskLandmarks;
            }
            vars.domain.solveMCTS(prob, mctsruns, timeout, printTree, vars);
            if (!vars.print) {
                return;
            }
            final long searchTime = System.currentTimeMillis();
            JSUtil.println("Total Time: " + (searchTime - vars.startTime));
            if(vars.costFunction instanceof NLGCost costfunction && (!(vars.costFunction instanceof EstimationCost))){
                if(costfunction.writeNNData){

                    try {
                        JSUtil.println("Closing NN Data");
                        costfunction.NNData.write("}");
                        costfunction.closeFile();
                    } catch (IOException e){
                        System.out.println("An error occurred while writing NN-data file");
                        e.printStackTrace();
                    }
                }
            }
            JSUtil.println("Number of Nodes generated: " + MCTSNode.NEXT_ID);
            if (duplicate) {
                JSUtil.println("Number of Insertion into Registry " + vars.registry.numStates);
            }
            if (!vars.planFound) {
                JSUtil.println("0 plans found");
            } else {
                JSUtil.println("Plan found:");
                //JSUtil.println("Solution in Tree: " + JSJshopVars.bestPlans.lastElement().isInTree());
                JSUtil.println("Reward for Given Plan: " + vars.bestPlans.lastElement().planCost());
                JSUtil.println("********* PLAN *******");
                if (vars.planFile.equals("NoFile.plan")) {
                    vars.bestPlans.lastElement().printPlan(vars.planWriter);
                } else {
                    vars.bestPlans.lastElement().printPlanToFile(vars.planFile);
                }

                //for (JSPlan plan : vars.bestPlans) {
                //    plan.printPlan();
                //}
                //JSJshopVars.bestPlans.lastElement().printPlan();
            }
            //}
        }
    }

    public void standardSearch(JSJshopVars vars) {
        vars.allPlans = allPlans;
        JSJshopVars.flagLevel = detail;
        JSPairPlanTSListNodes pair;
        Vector<JSPairPlanTSListNodes> allPlans;

        for (int k = 0; k < probSet.size(); k++) {

            prob = probSet.elementAt(k);
            JSUtil.println("Solving Problem :" + prob.Name());
            allPlans = vars.domain.solveAll(prob, vars.allPlans, vars);
            final long totalTime = System.currentTimeMillis();
            JSUtil.println("Total Time: " + (totalTime - vars.startTime));
            if (allPlans.isEmpty()) {
                // Return the failing solution to HICAP
                sol = new JSPlan();
                sol.assignFailure();
                solution = new JSPairPlanTSListNodes(new JSPairPlanTState(sol, new JSTState()), new Vector<>());
                JSUtil.println("0 plans found");
            } else {
                // Return the first solution to HICAP
                solution = allPlans.elementAt(0);
                sol = solution.planS().plan();
                //solution.print();
                JSUtil.println(allPlans.size() + " plans found.");
                if (JSJshopVars.flagLevel > 0) {
                    int bestplanIndex = 0;
                    Double bestPlanValue = Double.POSITIVE_INFINITY;
                    JSUtil.println("********* PLANS *******");
                    for (int i = 0; i < allPlans.size(); i++) {
                        //JSUtil.println("Plan # " + (i + 1));
                        pair = allPlans.elementAt(i);
                        double planCost = pair.planS().plan().planCost();
                        JSUtil.println("Plan cost: " + planCost);
                        if (bestPlanValue.compareTo(planCost) > 0) {
                            bestPlanValue = planCost;
                            bestplanIndex = i;
                        }
                        //pair.planS().plan().printPlan();//.print();
                        // pair.print();
                    }
                    pair = (JSPairPlanTSListNodes) allPlans.elementAt(bestplanIndex);
                    JSUtil.println("Best Plan with cost " + bestPlanValue + ": ");
                    pair.planS().plan().printPlan(vars.planWriter);
                }
            }
        }
    }

    public void transformWorldForArchitect(InputStream world, String problem, InputStream
        domain){
            //InputStream domain = JSJshop.class.getResourceAsStream("domain.shp");
            //InputStream world = JSJshop.class.getResourceAsStream("/de/saar/minecraft/worlds/artengis.csv");

            boolean parseSuccess = parserFile(domain);
            if (!parseSuccess) {
                JSUtil.println("Domain File not parsed correctly");
            }
            //creating Problem file from world info
            InputStream transformedProblem = transformWorld(world, problem);
            //parseFile initializes probSet, a list of all problems, prob.elementAt(0).state() is initial state
            parseSuccess = parserFile(transformedProblem);
            if (!parseSuccess) {
                JSUtil.println("Problem File not parsed correctly");
            }
        }


        //Calls mcts search but returns the plan insteasd of printing it for use in the NLG System
        public JSPlan nlgSearch ( int mctsruns, long timeout, InputStream world, String problem, InputStream
        domain, CostFunction.InstructionLevel level){
            //InputStream domain = JSJshop.class.getResourceAsStream("domain.shp");
            //InputStream world = JSJshop.class.getResourceAsStream("/de/saar/minecraft/worlds/artengis.csv");

            boolean parseSuccess = parserFile(domain);
            if (!parseSuccess) {
                JSUtil.println("Domain File not parsed correctly");
            }
            //creating Problem file from world info
            InputStream transformedProblem = transformWorld(world, problem);
            //parseFile initializes probSet, a list of all problems, prob.elementAt(0).state() is initial state
            parseSuccess = parserFile(transformedProblem);
            if (!parseSuccess) {
                JSUtil.println("Problem File not parsed correctly");
            }
            JSJshopVars vars = new JSJshopVars(false, true, false, false, false, "");
            vars.initRandGen(42);
            this.domain.axioms.setVars(vars);
            vars.domain = this.domain;
            vars.startTime = System.currentTimeMillis();
            this.mctsruns = mctsruns;
            this.timeout = timeout;
            this.updateMinimum = true;
            this.explorationFactor = java.lang.Math.sqrt(2);
            this.expansionPolicy = "simple";
            this.recursive = true;
            this.fastSimulation = false;
            this.bbPruningFast = true;
            this.recursiveSimulationBudget = 0;
            this.weightsFile = "";
            vars.costFunction = CostFunction.getCostFunction(CostFunction.CostFunctionType.STATEDEPENDENT, "house", level, weightsFile, numStructs, nnPath, compare, useTarget, useStructures, scenarioType);

            mctsSearch(vars);
            if (!vars.planFound) {
                return new JSPlan();
            }
            return vars.bestPlans.lastElement();
        }

        public InputStream transformWorld (InputStream world, String problem){

            BufferedReader reader = new BufferedReader(new InputStreamReader(world));
            String line = "";
            String result = "(defproblem problem-house build-house ( (last-placed 150 150 150) ";
            try {
                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    int z = Integer.parseInt(data[2]);
                    if (x < 0 || y < 0 || z < 0) {
                        continue;
                    }
                    String blockAt = "(block-at ";
                    //append block type
                    blockAt = blockAt.concat(data[3]).concat(" ");
                    //append x,y,z coordinates
                    blockAt = blockAt.concat(data[0]).concat(" ").concat(data[1]).concat(" ").concat(data[2]).concat(") ");
                    result = result.concat(blockAt);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            result = result.concat(") ((").concat(problem).concat(")) )");
            return new ByteArrayInputStream(result.getBytes());
        }

        public void testParser () {
            try {

                FileReader fr = new FileReader("farp.shp");
                StreamTokenizer tokenizer = new StreamTokenizer(fr);
                JSUtil.initParseTable(tokenizer);
                while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
                    if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                        System.err.print(tokenizer.nval + " ");
                    }
                    if (tokenizer.ttype == JSJshopVars.leftPar) {
                        System.err.print("( ");
                    }
                    if (tokenizer.ttype == JSJshopVars.rightPar) {
                        System.err.print(") ");
                    }
                    if (tokenizer.ttype == JSJshopVars.colon) {
                        System.err.print(": ");
                    }
                    if (tokenizer.ttype == JSJshopVars.semicolon) {
                        System.err.print("; ");
                    }
                    if (tokenizer.ttype == JSJshopVars.apostrophe) {
                        System.err.print("' ");
                    }
                    if (tokenizer.ttype == JSJshopVars.exclamation) {
                        System.err.print("! ");
                    }
                    if (tokenizer.ttype == JSJshopVars.interrogation) {
                        System.err.print("? ");
                    }
                    if (tokenizer.ttype == JSJshopVars.minus) {
                        System.err.print("- ");
                    }
                    if (tokenizer.ttype == JSJshopVars.lessT) {
                        System.err.print("< ");
                    }
                    if (tokenizer.ttype == JSJshopVars.equalT) {
                        System.err.print("= ");
                    }
                    if (tokenizer.ttype == JSJshopVars.greaterT) {
                        System.err.print("> ");
                    }
                    if (tokenizer.ttype == JSJshopVars.plus) {
                        System.err.print("+ ");
                    }

                    if (tokenizer.ttype == JSJshopVars.backquote) {
                        System.err.print("` ");
                    }

                    if (tokenizer.ttype == JSJshopVars.slash) {
                        System.err.print("/ ");
                    }
                    if (tokenizer.ttype == JSJshopVars.coma) {
                        System.err.print(", ");
                    }
                    if (tokenizer.ttype == JSJshopVars.astherisk) {
                        System.err.print("* ");
                    }
                    if (tokenizer.ttype == JSJshopVars.rightBrac) {
                        System.err.print("] ");
                    }
                    if (tokenizer.ttype == JSJshopVars.leftBrac) {
                        System.err.print("[ ");
                    }
                    if (tokenizer.ttype == JSJshopVars.verticalL) {
                        System.err.print("| ");
                    }
                    if (tokenizer.ttype == StreamTokenizer.TT_WORD) {
                        System.err.print(tokenizer.sval + " ");
                    }

                }
                fr.close();
                //JSUtil.flag("End of parsing");

            } catch (Exception e) {
                JSUtil.println("Error reading control parameters: " + e);
                System.exit(1);
            }

        }

        public boolean parserFile (InputStream input){
            String libraryDirectory = ".";

            try {
                Reader fr = new InputStreamReader(input);
                StreamTokenizer tokenizer = new StreamTokenizer(fr);
                tokenizer.lowerCaseMode(true);
                JSUtil.initParseTable(tokenizer);
                while (tokenizer.nextToken() != StreamTokenizer.TT_EOF)
                    processToken(tokenizer);
                fr.close();
            } catch (IOException e) {
                System.out.println("Error in readFile() : " + e);
                return false;
            } catch (JSParserError parserError) {
                System.out.println("Error in parsing file");
                return false;
            }
            return true;
        }

        public boolean parserFile (String fileName){
            String libraryDirectory = ".";
            try {
                FileReader fr = new FileReader(fileName);
                StreamTokenizer tokenizer = new StreamTokenizer(fr);
                tokenizer.lowerCaseMode(true);
                JSUtil.initParseTable(tokenizer);
                while (tokenizer.nextToken() != StreamTokenizer.TT_EOF)
                    processToken(tokenizer);
                fr.close();
            }catch (FileNotFoundException exp){
                JSUtil.println("Can not open file : " + fileName);
                return false;
            } catch (IOException e) {
                System.out.println("Error in readFile() : " + e);
                return false;
            } catch (JSParserError parserError) {
                System.out.println("Error in parsing file");
                return false;
            }
            return true;
        }

        //public BufferedReader getBufferedReader (String dir, String file){ return getBufferedReader(dir, file);}

        public void processToken (StreamTokenizer tokenizer){
            if (tokenizer.ttype == JSJshopVars.leftPar) {
                if (!JSUtil.expectTokenType(StreamTokenizer.TT_WORD, tokenizer,
                        "Expected 'defdomain or defproblem' "))
                    throw new JSParserError(); //return;

                tokenizer.pushBack();

                String w = JSUtil.readWord(tokenizer, "JSJshop>>processToken");
                if (w.equals("%%%"))
                    throw new JSParserError(); //return;

                if (w.equalsIgnoreCase("defdomain")) {
                    domain = new JSPlanningDomain(tokenizer);
                    return;
                } else {
                    if (w.equalsIgnoreCase("defproblem")) {
                        prob = new JSPlanningProblem(tokenizer);
                        probSet.addElement(prob);
                        return;
                    }
                }

                System.err.println("Line : " + tokenizer.lineno() + " Expecting defdomain or defproblem");
                throw new JSParserError(); //return;
            } else {
                System.err.println("Line : " + tokenizer.lineno() + " Expected '('");
                throw new JSParserError(); //return;
            }
        }


        public boolean parserFileLandmarks (String libraryFile){
            String libraryDirectory = ".";

            try {
                FileReader fr = new FileReader(libraryFile);
                StreamTokenizer tokenizer = new StreamTokenizer(fr);
                tokenizer.lowerCaseMode(true);
                //JSUtil.initParseTable(tokenizer);
                if (fr == null) {
                    JSUtil.println("Can not open file : " + libraryFile);
                    return false;
                }
                if (tokenizer.nextToken() != StreamTokenizer.TT_EOF)
                    processTokenLandmarks(tokenizer);
                fr.close();
            } catch (IOException e) {
                System.out.println("Error in readFile() : " + e);
                return false;
            } catch (JSParserError parserError) {
                System.out.println("Error in parsing file");
                return false;
            }
            return true;
        }


        public void processTokenLandmarks (StreamTokenizer tokenizer){
            initialFactLandmarks = new HashSet<JSFactLandmark>();
            initialTaskLandmarks = new HashSet<>();
            while (tokenizer.ttype != StreamTokenizer.TT_EOF) {
                if (tokenizer.ttype == JSJshopVars.leftBrac) {
                    if (!JSUtil.expectTokenType(StreamTokenizer.TT_WORD, tokenizer,
                            "Expected landmark description "))
                        throw new JSParserError(); //return;

                    tokenizer.pushBack();

                    String w = JSUtil.readWord(tokenizer, "JSJshop>>processToken");
                    if (w.equals("%%%"))
                        throw new JSParserError(); //return;

                    if (w.equalsIgnoreCase("factlm")) {
                        JSUtil.readToken(tokenizer, "rightBrac");
                        JSFactLandmark tmp = new JSFactLandmark(tokenizer);
                        initialFactLandmarks.add(tmp);
                    } else if (w.equalsIgnoreCase("tasklm")) {
                        JSUtil.readToken(tokenizer, "rightBrac");
                        JSTaskLandmark tmp = new JSTaskLandmark(tokenizer, false);
                        initialTaskLandmarks.add(tmp);
                    } else if (w.equalsIgnoreCase("actionlm")) {
                        JSUtil.readToken(tokenizer, "rightBrac");
                        JSTaskLandmark tmp = new JSTaskLandmark(tokenizer, true);
                        initialTaskLandmarks.add(tmp);
                    } else if (w.equalsIgnoreCase("methodlm")) {
                        while (tokenizer.ttype != StreamTokenizer.TT_EOL && tokenizer.ttype != StreamTokenizer.TT_EOF && tokenizer.ttype != JSJshopVars.leftBrac)
                            JSUtil.readToken(tokenizer, "reading methodLMs");
                        tokenizer.pushBack();
                    } else {
                        System.err.println("Line : " + tokenizer.lineno() + " Expected '('");
                        throw new JSParserError(); //return;
                    }

                } else {
                    System.err.println("Line : " + tokenizer.lineno() + " Expected '('");
                    throw new JSParserError(); //return;
                }

                JSUtil.readToken(tokenizer, "nextToken");
                //tokenizer.pushBack();

            }
        }


        public JSPlanningProblem prob () {
            return prob;
        }

        public JSPlan sol () {
            return sol;
        }

        // public JSJshopNode tree () { return tree;}


    }

            

                    

                    

    

