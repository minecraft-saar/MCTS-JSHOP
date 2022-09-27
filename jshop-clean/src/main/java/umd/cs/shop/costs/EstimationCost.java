package umd.cs.shop.costs;

import ai.djl.ndarray.types.Shape;
import de.saar.basic.Pair;
import de.saar.coli.minecraft.MinecraftRealizer;
import de.saar.coli.minecraft.relationextractor.IntroductionMessage;
import de.saar.coli.minecraft.relationextractor.MinecraftObject;
import de.saar.minecraft.analysis.WeightEstimator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import umd.cs.shop.*;
import umd.cs.shop.DataParser;
import umd.cs.shop.DataParserNeutral;

import java.io.*;
import java.util.*;

import ai.djl.*;
import ai.djl.inference.*;
import ai.djl.ndarray.*;
import ai.djl.translate.*;

import java.nio.file.*;

public class EstimationCost extends NLGCost {

    // related to DJL model loading
    Model nn;
    Translator<Float[], Float> translator;
    Predictor<Float[], Float> predictor;

    // parser-related
    DataParser parser;
    boolean useTarget;
    boolean useStructures;
    int numChannels;
    int[] dim;
    ScenarioType scenarioType;

    // related to cost comparison
    boolean compare;
    MinecraftRealizer nlgSystem;
    public BufferedWriter writerCost;
    Double diffCosts;
    Double avgDiffCosts;
    int countInstr;

    // scaling parameters
    double min;
    double max;

    /**
     * Types of scenarios, so things that are being built (small bridge, fancy bridge)
     */
    public enum ScenarioType {
        SimpleBridge,
        FancyBridge
    }

    /**
     * A CostFunction that uses an NN instead of the NLG System to estimate costs instead of calculating them.
     * Inherits from NLGCost and thus, only the initialization and getCost() are changed.
     *
     * @param ins instruction level, needed by NLGSystem base
     * @param weightFile weight file, needed by NLGSystem base
     * @param numStructs number of special structures that appear in this scenario
     * @param nnPath path to the pre-trained model
     * @param compare whether the NN results should be directly compared to NLG system results
     * @param useTarget whether target information is used for the NN
     * @param useStructures whether information on structures is used for the NN
     * @param scenarioType type of scenario that is the target of the instructions
     * @param scalerPath string path to a file containing min and max values for the scaler
     */
    public EstimationCost(CostFunction.InstructionLevel ins, String weightFile, int numStructs, String nnPath, boolean compare, boolean useTarget, boolean useStructures, ScenarioType scenarioType, String scalerPath) {
        super(ins, weightFile);
        this.compare = compare;
        this.useTarget = useTarget;
        this.useStructures = useStructures;
        this.scenarioType = scenarioType;

        // prepare NN and dimensions for this scenario
        if (this.scenarioType == ScenarioType.SimpleBridge) { // model for simple bridge scenario
            nn = Model.newInstance("simpleBridge");
            this.dim = new int[]{5, 3, 3};
        } else if (this.scenarioType == ScenarioType.FancyBridge) { // model for fancy bridge scenario
            nn = Model.newInstance("fancyBridge");
            this.dim = new int[]{3, 5, 9};
        } else {
            System.out.println("Careful - Invalid scenario type!");
        }
        // load pre-trained model
        Path nnDir = Paths.get(nnPath);
        try {
            nn.load(nnDir);
        } catch (IOException | MalformedModelException e) {
            e.printStackTrace();
        }

        // read scaler parameters from file
        try {
            JSONParser scalerParser = new JSONParser();
            Object obj = scalerParser.parse(new FileReader(scalerPath));
            JSONObject scalerObject = (JSONObject)obj;
            this.min = (double)scalerObject.get("min");
            this.max = (double)scalerObject.get("max");
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        // prepare everything needed for the comparison between NLG system and NN
        if (this.compare) {
            nlgSystem = MinecraftRealizer.createRealizer();
            // output comparisons into a txt file
            try {
                writerCost = new BufferedWriter(new FileWriter("cost_comparison.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            // prepare NLG system
            lowestCost = 10.0;
            if (weightFile.equals("")) {
                weightsPresent = false;
            } else if (weightFile.equals("random")) {
                nlgSystem.randomizeExpectedDurations();
            } else {
                weightsPresent = true;
                try {
                    weights = WeightEstimator.WeightResult.fromJson(
                            Files.readString(Paths.get(weightFile)));
                    nlgSystem.setExpectedDurations(weights.weights, false);
                } catch (IOException e) {
                    throw new RuntimeException("could not read weights file: " + weightFile);
                }
            }

            // initialize variables that are used to track differences between NN and NLG system
            diffCosts = 0D;
            avgDiffCosts = 0D;
            countInstr = 0;
        }

        // based on given options, determine how many channels are needed for the data
        if (useStructures) {
            numChannels = 2 + numStructs;
        } else if (useTarget) {
            numChannels = 2;
        } else {
            numChannels = 1;
        }

        parser = new DataParserNeutral(useTarget, useStructures, numChannels, scenarioType);
//        parser = new DataParser(useTarget, useStructures, numChannels, scenarioType);

        // translator needed for loading torchscript model
        // modeled after: https://docs.djl.ai/jupyter/load_pytorch_model.html
        translator = new Translator<Float[], Float>() {
            @Override
            public NDList processInput(TranslatorContext ctx, Float[] input) {
                NDManager manager = ctx.getNDManager();
                Shape shape = new Shape(numChannels, dim[0], dim[1], dim[2]);
                float[] primitiveFloatArr = new float[input.length];
                for (int i = 0; i < input.length; i++) {
                    primitiveFloatArr[i] = input[i].floatValue();
                }
                NDArray array = manager.create(primitiveFloatArr, shape);
                return new NDList(array);
            }

            @Override
            public Float processOutput(TranslatorContext ctx, NDList list) {
                NDArray temp_arr = list.get(0);
                return temp_arr.getFloat();
            }

            @Override
            public Batchifier getBatchifier() {
                // The Batchifier describes how to combine a batch together
                // Stacking, the most common batchifier, takes N [X1, X2, ...] arrays to a single [N, X1, X2, ...] array
                return Batchifier.STACK;
            }
        };

        predictor = nn.newPredictor(translator);
    }

    /**
     * Estimate the cost for a specific world state and instruction.
     *
     * @param state ???
     * @param op ???
     * @param groundedOperator ???
     * @param approx ???
     * @return estimated cost
     */
    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {
        if (groundedOperator.get(0).equals("!place-block-hidden") ||
                groundedOperator.get(0).equals("!remove-it-row") ||
                groundedOperator.get(0).equals("!remove-it-railing") ||
                groundedOperator.get(0).equals("!remove-it-stairs") ||
                groundedOperator.get(0).equals("!remove-it-wall")) {
            return 0.0;
        }
        MinecraftObject currentObject = createCurrentMinecraftObject(groundedOperator);  // op, groundedOperator
        Set<String> knownObjects = new HashSet<>();
        Pair<Set<MinecraftObject>, Set<MinecraftObject>> pair = createWorldFromState(state, knownObjects, currentObject);
        if (currentObject instanceof IntroductionMessage intro) {
            if (knownObjects.contains(intro.name)) {
                //make dead end
                return 30000.0; // why is this number different from the one in NLGCost? when did that happen?
            } else {
                return 0.000001;
            }
        }

        // NLG Model for comparison
        double returnValueNLG = 0D;
        if (this.compare) {
            Set<MinecraftObject> world = pair.getRight();
            Set<MinecraftObject> it = pair.getLeft();

            countInstr++;
            String currentObjectType = currentObject.getClass().getSimpleName().toLowerCase();
            boolean objectFirstOccurence = !knownObjects.contains(currentObjectType);
            if (objectFirstOccurence && weights != null) {
                // temporarily set the weight to the first occurence one
                // ... if we have an estimate for the first occurence
                if (weights.firstOccurenceWeights.containsKey("i" + currentObjectType)) {
                    nlgSystem.setExpectedDurations(
                            Map.of("i" + currentObjectType, weights.firstOccurenceWeights.get("i" + currentObjectType)),
                            false);
                }
            }
            // remove the two lines below to fix the structures bug of the NLG system
//            world.addAll(currentObject.getChildren());
//            world.add(currentObject);
            returnValueNLG = nlgSystem.estimateCostForPlanningSystem(world, currentObject, it);
        }

        // process world state data by using a parser
        parser.setNewData(this.model);
        parser.convertIntoVector();
        float[][][][] inputDataNN = parser.getMatrix();

        // flatten world state matrix in preparation for NDArray
        Float[] flattenedInputDataNN = new Float[numChannels * dim[0] * dim[1] * dim[2]];
        int currIdx = 0;
        for (float[][][] l1 : inputDataNN) {
            for (float[][] l2 : l1) {
                for (float[] l3 : l2) {
                    for (float i : l3) {
                        flattenedInputDataNN[currIdx] = i;
                        currIdx++;
                    }
                }
            }
        }

        // estimate cost
        double returnValue = Double.POSITIVE_INFINITY;
        try {
            returnValue = predictor.predict(flattenedInputDataNN);
        } catch (TranslateException e) {
            System.out.println("An error occurred while estimating the costs using the NN.");
            e.printStackTrace();
        }
        // reverse scaling
        returnValue = (returnValue - 0D) / (1D - 0D);
        returnValue = returnValue * (this.max - this.min) + this.min;

//        System.out.printf("Cost NN: %f%n", returnValue);
        // do comparisons between NLG system and NN and print them into file
        try {
            if (this.compare) {
                writerCost.write("world: " + this.model + '\n');
                writerCost.write("Cost NLG: " + returnValueNLG + '\n');
                writerCost.write("Cost NN: " + returnValue + '\n');
                if (returnValueNLG < 1000000000000.0D) { // calculate and print out average cost differences
                    diffCosts += Math.abs(returnValue - returnValueNLG);
                    avgDiffCosts = diffCosts / countInstr;
                    writerCost.write("AVG COST DIFF: " + avgDiffCosts + '\n');
                } else {  // ignore comparision if NLG prediction is infinity
                    countInstr--;
                }
                writerCost.write("------------" + '\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnValue;
    }

}