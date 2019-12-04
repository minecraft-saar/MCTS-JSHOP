package umd.cs.shop;

import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/*HICAP import nrl.aic.hicap.*;*/

//The main constructor of this class is JSJshop(String nameFile). This
//constructor will open and parse the <file> named nameFile and solves the 
//problem  stated in it. It assumes that the file contains a single problem
// i.e., "make-problem" and a single domain, i.e., "make-domain". If several
// domains and problems are given it will parse all of them and solve the
// last problem for the last domain.

public final class JSJshop implements Runnable {
    /*HICAP*//*==== class variables ====*/
    // /*HICAP*/public static boolean corbaToHicap = false;
    // /*HICAP*/public static JApplet applet;

    /* instance variables */
    private JSPlanningDomain dom;

    @Parameters(index = "0", description = "The domain file")
    String nameDomainFile;

    @Parameters(index = "1", description = "The problem file")
    String nameProblemFile;

    @Option(names = {"-s"}, defaultValue = "false", description = "Enables standard search")
    boolean standardSearch;

    @Option(names = {"-m", "--monteCarloRuns"}, defaultValue = "10000", description = "Number of runs for the monte carlo search")
    int mctsruns;

    @Option(names = {"-x", "--max"}, defaultValue = "false", description = "take maximum when updating the reward")
    boolean updateMaximum;

    @Option(names = {"-r", "--random"}, defaultValue = "false", description = "take random cost function")
    boolean random;

    @Option(names = {"-p", "--policy"}, defaultValue = "UCT", description = "UCT")
    String policy;

    @Option(names = {"-t", "--timeout"}, defaultValue = "10000", description = "Timeout in milliseconds")
    long timeout;

    @Option(names = {"-c", "--costFunction"}, defaultValue = "false", description = "Enables use of cost funtion")
    boolean costFunction;

    @Option(names = {"-d", "--detail"}, defaultValue = "1", description = "Integer from 1 to 10 for more details during standard search")
    int detail;

    @Option(names = {"-a", "--all"}, defaultValue = "false", description = "Find all plans in standard search")
    boolean allPlans;

    @Override
    public void run() {
        JSJshopVars.startTime = System.currentTimeMillis();
        JSUtil.println("Reading file " + nameDomainFile);
        if (!parserFile(nameDomainFile))
            if (JSJshopVars.flagExit)
                System.exit(0);
            else
                return;
        JSUtil.println("Domain file parsed successfully");
        JSUtil.println("Reading file " + nameProblemFile);
        if (!parserFile(nameProblemFile))
            if (JSJshopVars.flagExit)
                System.exit(0);
            else
                return;

        JSUtil.println("Problem file parsed successfully");
        final long parseTime = System.currentTimeMillis();
        JSUtil.println("Parsing Time: " + (parseTime - JSJshopVars.startTime));

        if (standardSearch) {
            standardSearch();
        } else {
            mctsSearch();
        }

    }

    public static void main(String[] args) {
        CommandLine.run(new JSJshop(), args);

    }

    private JSPlanningProblem prob;

    private JSListPlanningProblem probSet = new JSListPlanningProblem();

    private JSPlan sol;

    private JSJshopNode tree = null;

    private JSPairPlanTSListNodes solution;

    /*HICAP:    private NeoEditor aNeoEditor;*/
    /*====  main ====*/

    /* constructors */

    //public
    //  JSJshop()    {    }


    /******** main constructor **********/

    public void standardSearch() {
        JSJshopVars.allPlans = allPlans;
        JSJshopVars.flagLevel = detail;
        JSPairPlanTSListNodes pair;
        JSListPairPlanTStateNodes allPlans;

        for (int k = 0; k < probSet.size(); k++) {

            prob = (JSPlanningProblem) probSet.elementAt(k);
            JSUtil.println("Solving Problem :" + prob.Name());
            allPlans = dom.solveAll(prob, JSJshopVars.allPlans);
            final long totalTime = System.currentTimeMillis();
            JSUtil.println("Total Time: " + (totalTime - JSJshopVars.startTime));
            if (allPlans.isEmpty()) {
                // Return the failing solution to HICAP
                sol = new JSPlan();
                sol.assignFailure();
                solution = new JSPairPlanTSListNodes(new JSPairPlanTState(sol, new JSTState()), new Vector<>());
                JSUtil.println("0 plans found");
            } else {
                // Return the first solution to HICAP
                solution = (JSPairPlanTSListNodes) allPlans.elementAt(0);
                sol = solution.planS().plan();
                //solution.print();
                JSUtil.println(allPlans.size() + " plans found.");
                if (JSJshopVars.flagLevel > 0) {
                    int bestplanIndex = 0;
                    Double bestPlanValue = Double.NEGATIVE_INFINITY;
                    JSUtil.println("********* PLANS *******");
                    for (int i = 0; i < allPlans.size(); i++) {
                        JSUtil.println("Plan # " + (i + 1));
                        pair = (JSPairPlanTSListNodes) allPlans.elementAt(i);
                        double planCost = pair.planS().plan().planCost();
                        JSUtil.println("Plan cost: " + planCost);
                        if (bestPlanValue.compareTo(planCost) > 0) {
                            bestPlanValue = planCost;
                            bestplanIndex = i;
                        }
                        //pair.planS().plan().printPlan();//.print();
                        // pair.print();
                    }
                    JSUtil.println("Best Plan: ");
                    pair = (JSPairPlanTSListNodes) allPlans.elementAt(bestplanIndex);
                    pair.planS().plan().printPlan();
                }
            }
        }
    }

    //MCTS
    public void mctsSearch() {
        JSJshopVars.policy = new UCTPolicy(); //TODO adapt to parameter
        JSJshopVars.updateMaximum = updateMaximum;
        JSJshopVars.random = random;
        if (costFunction)
            JSJshopVars.costFunction = new BasicCost(); //TODO adapt to parameter
        for (int k = 0; k < probSet.size(); k++) {
            prob = (JSPlanningProblem) probSet.elementAt(k);
            JSUtil.println("Solving Problem :" + prob.Name() + " with mcts");
            dom.solveMCTS(prob, mctsruns, timeout, costFunction);
            final long searchTime = System.currentTimeMillis();
            JSUtil.println("Total Time: " + (searchTime - JSJshopVars.startTime));
            if (random) {
                JSUtil.println("Random cost function uses: " + JSJshopVars.approxUses);
            } else {
                JSUtil.println("Real cost function uses: " + JSJshopVars.realCostUses);
            }
            if (JSJshopVars.bestPlans.lastElement().plan.isFailure()) {
                JSUtil.println("0 plans found");
            } else {
                JSUtil.println("Plan found:");
                JSUtil.println("Solution in Tree: " + JSJshopVars.bestPlans.lastElement().inTree);
                JSUtil.println("Reward for Given Plan: " + JSJshopVars.bestPlans.lastElement().reward());
                JSUtil.println("********* PLAN *******");
                JSJshopVars.bestPlans.lastElement().plan.printPlan();
            }
            //}
        }
    }

    //Calls mcts search but returns the plan insteasd of printing it for use in the NLG System
    public JSPlan nlgSearch(int mctsruns, long timeout) {
        InputStream domain = JSJshop.class.getResourceAsStream("domain.shp");
        InputStream world = JSJshop.class.getResourceAsStream("/de/saar/minecraft/worlds/bridge.csv");

        boolean parseSuccess = parserFile(domain);
        if (!parseSuccess) {
            JSUtil.println("Domain File not parsed correctly");
        }
        InputStream transformedProblem = transformWorld(world);
        parseSuccess = parserFile(transformedProblem);
        if (!parseSuccess) {
            JSUtil.println("Problem File not parsed correctly");
        }
        JSJshopVars.startTime = System.currentTimeMillis();
        this.mctsruns = mctsruns;
        this.timeout = timeout;
        mctsSearch();
        return JSJshopVars.bestPlans.lastElement().plan;
    }

/*HICAP:   
    public
    JSJshop( String nameFile, NeoEditor aNE, JSTaskAtom pred)
    {
      this.aNeoEditor = aNE;
      setFile( nameFile, pred);
      
    }
*/

    //public JSJshop(String nameFile, JSTaskAtom pred) {
    //    setFile(nameFile, pred);

    //}

    public InputStream transformWorld(InputStream world) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(world));
        String line = "";
        String result = "(defproblem problem-house build-house ( (last-placed dummy dummy dummy) ";
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

        result = result.concat(") ((build-house 0 0 2 3 3 3)) )");
        JSUtil.println(result);
        return new ByteArrayInputStream(result.getBytes());
    }

    public JSJshopNode getTree() {
        return tree;
    }

    public JSPairPlanTSListNodes getSolution() {
        return solution;
    }

    public JSListLogicalAtoms getAddList() {
        if (tree == null) return null;
        return solution.planS().tState().addList();
    }

    public JSListLogicalAtoms getDeleteList() {
        if (tree == null) return null;
        return solution.planS().tState().deleteList();
    }

    /*==== solves planning problem for input file ====*/
    public JSPairPlanTSListNodes setFile(String nameFile,
                                         JSTaskAtom pred) {
        JSPairPlanTSListNodes pair;
        Vector<Object> listNodes;

        JSJshopVars.VarCounter = 0;

        //JSUtil.flag2(nameFile+" will be parsered");
        parserFile(nameFile);

        /*HICAP: prob.assignState(aNeoEditor.translateState());*/
        /*HICAP: prob.makeTask(pred);*/

        JSUtil.flag("File parsed");
        dom.print();
        JSUtil.flag("<- domain");
        prob.print();
        JSUtil.flag("<- problem");

        JSJshopVars.allPlans = false;
        JSListPairPlanTStateNodes listPairs = dom.solveAll(prob, JSJshopVars.allPlans);
        if (listPairs.size() == 0)
            return null;
        else
            pair = (JSPairPlanTSListNodes) listPairs.elementAt(0);

        JSUtil.flag("**After planning");

        sol = pair.planS().plan();
        if (sol.isFailure()) {
            JSUtil.print("*NO* SOLUTION FOUND");
            return null;
        } else {
            JSUtil.print("SOLUTION FOUND");
            pair.planS().tState().print();
            JSUtil.println("***************FINAL SOLUTION*******");
            sol.print();

            JSUtil.println("********* list-tree FINAL SOLUTION*******");


            JSTaskAtom ta;
            Vector listT;
            JSJshopNode node;

            listNodes = pair.listNodes();
            for (int i = 0; i < listNodes.size(); i++) {
                node = (JSJshopNode) listNodes.elementAt(i);
                node.print2();
            }
            // we assume that there is only a single task
            // at the top level. If more than 1 task
            // must be reduced at the top level, listNodes
            // contain the list of trees (one for each task).
            // Whereas this does not affect SHOP, HICAP assumes
            // that it receives a single task at the top level
            //tree.print();
            //listNodes = pair.listNodes();
            //pair.print();
            //JSJshopNode node;
            node = (JSJshopNode) listNodes.elementAt(listNodes.size() - 1);
            // last element must be the root
            listNodes.removeElement(node);
            tree = new JSJshopNode(node, listNodes);
            JSUtil.println("********* tree FINAL SOLUTION*******");

            tree.print();
            solution = pair;
            return pair;
        } // solution found
    } // setFile


    public void testParser() {
        try {

            FileReader fr = new FileReader("farp.shp");
            StreamTokenizer tokenizer = new StreamTokenizer(fr);
            JSUtil.initParseTable(tokenizer);
            while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
                if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                    System.err.print(new
                            Double(tokenizer.nval).toString() + " ");
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

    public boolean parserFile(InputStream input) {
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

    public boolean parserFile(String libraryFile) {
        String libraryDirectory = ".";

        try {
            FileReader fr = new FileReader(libraryFile);
            StreamTokenizer tokenizer = new StreamTokenizer(fr);
            tokenizer.lowerCaseMode(true);
            JSUtil.initParseTable(tokenizer);
            if (fr == null) {
                JSUtil.println("Can not open file : " + libraryFile);
                return false;
            }
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

    public BufferedReader getBufferedReader(String dir, String file) {
        return getBufferedReader(dir, file);
    }

    public BufferedReader getBufferedReader(String dir, String file,
                                            JApplet applet) {
        if (file == null) return null;
        BufferedReader br = null;
        FileInputStream libraryFileInputStream = null;
        InputStream conn = null;
        String line;
        try {
            if (applet != null) {
                URL url = getAppletURL(file, applet);
                if (url == null) {
                    System.err.println("Util.getBufferedReader() error: cannot get URL");
                    return null;
                } else {
                    conn = url.openStream();
                    if (conn == null) {
                        System.err.println("Util.getBufferedReader() error: cannot open URL");
                        return null;
                    }
                }
            } // is applet
            else  // is application
            {
                libraryFileInputStream =
                        new FileInputStream(dir + File.separator + file);
            }
        } catch (IOException e) {
            System.err.println("Error 1 in Util.getBufferedReader : " + e);
            return null;
        }

        if (applet != null) {
            br =
                    new BufferedReader(new InputStreamReader(conn));
        } else  // application
        {
            try {
                br =
                        new BufferedReader(new InputStreamReader(libraryFileInputStream,
                                System.getProperty("file.encoding")));
            } catch (UnsupportedEncodingException e) {
                System.err.println("Error 2 in Util.getBufferedReader : " + e);
                return null;
            }
        }
        return br;
    } // getBufferedReader

    public URL getAppletURL(String file, JApplet applet) {
        try {
            return (new URL(applet.getCodeBase() + file));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public void processToken(StreamTokenizer tokenizer) {
        if (tokenizer.ttype == JSJshopVars.leftPar) {
            if (!JSUtil.expectTokenType(StreamTokenizer.TT_WORD, tokenizer,
                    "Expected 'defdomain or defproblem' "))
                throw new JSParserError(); //return;

            tokenizer.pushBack();

            String w = JSUtil.readWord(tokenizer, "JSJshop>>processToken");
            if (w.equals("%%%"))
                throw new JSParserError(); //return;

            if (w.equalsIgnoreCase("defdomain")) {
                dom = new JSPlanningDomain(tokenizer);
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

    public JSPlanningDomain dom() {
        return dom;
    }

    public JSPlanningProblem prob() {
        return prob;
    }

    public JSPlan sol() {
        return sol;
    }

    public JSJshopNode tree() {
        return tree;
    }


}

            

                    

                    

    

