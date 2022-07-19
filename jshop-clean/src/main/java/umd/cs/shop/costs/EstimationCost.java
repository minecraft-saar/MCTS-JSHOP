package umd.cs.shop.costs;

import ai.djl.ndarray.types.Shape;
import de.saar.basic.Pair;
import de.saar.coli.minecraft.MinecraftRealizer;
import de.saar.coli.minecraft.relationextractor.IntroductionMessage;
import de.saar.coli.minecraft.relationextractor.MinecraftObject;
import de.saar.minecraft.analysis.WeightEstimator;
import umd.cs.shop.*;
import umd.cs.shop.DataParser;

import java.io.*;
import java.util.*;

import ai.djl.*;
import ai.djl.inference.*;
import ai.djl.ndarray.*;
import ai.djl.translate.*;

import java.nio.file.*;

public class EstimationCost extends NLGCost {

    Model nn;
    Translator<Float[], Float> translator;
    Predictor<Float[], Float> predictor;
    DataParser parser;
    boolean useTarget;
    boolean useStructures;
    int numChannels;
    NNType nnType;
    int[] dim;
    ScenarioType scenarioType;

    // things needed for cost comparison
    boolean compare;
    MinecraftRealizer nlgSystem;
    public BufferedWriter writerCost;
    Double diffCosts;
    Double avgDiffCosts;
    int countInstr;

    // scaling parameters
    double min = 2690.70126898D;
    double max = 128071.40159593D;

    public enum NNType {
        Simple,
        CNN
    }

    public enum ScenarioType {
        SimpleBridge,
        FancyBridge
    }

    public EstimationCost(CostFunction.InstructionLevel ins, String weightFile, NNType nnType, String nnPath, boolean compare, boolean useTarget, boolean useStructures, ScenarioType scenarioType) {
        super(ins, weightFile);
        this.nnType = nnType;
        this.compare = compare;
        this.useTarget = useTarget;
        this.useStructures = useStructures;
        this.scenarioType = scenarioType;

        // load pre-trained NN
        if (this.scenarioType == ScenarioType.SimpleBridge) {
            nn = Model.newInstance("trained_model.zip");
            this.dim = new int[]{5, 3, 3};
        } else if (this.scenarioType == ScenarioType.FancyBridge) {
            nn = Model.newInstance("trained_model_fancy.zip");
            this.dim = new int[]{3, 5, 9};
        } else {
            System.out.println("Careful - Invalid scenario type!");
        }
        Path nnDir = Paths.get(nnPath);
        try {
            nn.load(nnDir);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MalformedModelException e) {
            e.printStackTrace();
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

            diffCosts = 0D;
            avgDiffCosts = 0D;
            countInstr = 0;
        }

        // use options to determine correct data structure for NN
//        if (useStructures) {
//            numChannels = 5;
//        } else if (useTarget && (nnType == NNType.CNN)) {
//            numChannels = 2;
//        } else if (nnType == NNType.Simple || (nnType == NNType.CNN)) {
//            numChannels = 1;
//        }
        if (useStructures) {
            numChannels = 5;
        } else if (useTarget) {
            numChannels = 2;
        } else if (nnType == NNType.Simple || (nnType == NNType.CNN)) {
            numChannels = 1;
        }

        parser = new DataParser(useTarget, useStructures, numChannels, nnType, scenarioType);

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

    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {
        System.out.println("--------");
        long startTime = System.currentTimeMillis();
        long checkTime1 = System.nanoTime();
        if (groundedOperator.get(0).equals("!place-block-hidden") ||
                groundedOperator.get(0).equals("!remove-it-row") ||
                groundedOperator.get(0).equals("!remove-it-railing") ||
                groundedOperator.get(0).equals("!remove-it-stairs") ||
                groundedOperator.get(0).equals("!remove-it-wall")) {
            return 0.0;
        }
        long endCheck1 = System.nanoTime();
        // -------------------------------------------------------------------------------------------------------------
        long checkTime2 = System.nanoTime();
        MinecraftObject currentObject = createCurrentMinecraftObject(groundedOperator);  // op, groundedOperator
        long endCheck2 = System.nanoTime();
        // -------------------------------------------------------------------------------------------------------------
        long checkTime3 = System.nanoTime();
        Set<String> knownObjects = new HashSet<>();
        Pair<Set<MinecraftObject>, Set<MinecraftObject>> pair = createWorldFromState(state, knownObjects, currentObject);
        if (currentObject instanceof IntroductionMessage intro) {
            if (knownObjects.contains(intro.name)) {
                //make dead end
                return 30000.0;
            } else {
                return 0.000001;
            }
        }
        long endCheck3 = System.nanoTime();
        // -------------------------------------------------------------------------------------------------------------

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
//            world.addAll(currentObject.getChildren());
//            world.add(currentObject);
            returnValueNLG = nlgSystem.estimateCostForPlanningSystem(world, currentObject, it);
            System.out.printf("Cost NLG: %f%n", returnValueNLG);
            try {
                writerCost.write("world: " + this.model + '\n');
                writerCost.write("Cost NLG: " + returnValueNLG + '\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // process world state data by using a parser
        long checkTime4 = System.nanoTime();
        parser.setNewData(this.model);
        parser.convertIntoVector();
        float[][][][] inputDataNN = parser.getMatrix();
        long endCheck4 = System.nanoTime();
        // -------------------------------------------------------------------------------------------------------------

        // flatten world state matrix in preparation for NDArray
        long checkTime5 = System.nanoTime();
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
        long endCheck5 = System.nanoTime();
        // -------------------------------------------------------------------------------------------------------------

        // estimate cost and reverse scaling
        long checkTime6 = System.nanoTime();
        double returnValue = Double.POSITIVE_INFINITY;
        try {
            returnValue = predictor.predict(flattenedInputDataNN);
            // inverse scaling
            returnValue = (returnValue - 0D) / (1D - 0D);
            returnValue = returnValue * (this.max - this.min) + this.min;
        } catch (TranslateException e) {
            System.out.println("An error occurred while estimating the costs using the NN.");
            e.printStackTrace();
        }
        long endCheck6 = System.nanoTime();
        // -------------------------------------------------------------------------------------------------------------

//        System.out.printf("Cost NN: %f%n", returnValue);
        // do comparisons between NLG system and NN
        if (this.compare) {
            try {
                writerCost.write("Cost NN: " + returnValue + '\n');
                writerCost.write("------------" + '\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(returnValue);
            System.out.println(returnValueNLG);
            if (returnValueNLG < 1000000000000.0D) {
                diffCosts += Math.abs(returnValue - returnValueNLG);
                avgDiffCosts = diffCosts / countInstr;
                System.out.println("AVG COST DIFF: " + avgDiffCosts);
            } else {  // ignore comparision if NLG prediction is infinity
                countInstr--;
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.printf("Duration Check1: %d%n", (endCheck1 - checkTime1));
        System.out.printf("Duration Check2: %d%n", (endCheck2 - checkTime2));
        System.out.printf("Duration Check3: %d%n", (endCheck3 - checkTime3));
        System.out.printf("Duration Check4: %d%n", (endCheck4 - checkTime4));
        System.out.printf("Duration Check5: %d%n", (endCheck5 - checkTime5));
        System.out.printf("Duration Check6: %d%n", (endCheck6 - checkTime6));
        System.out.printf("Duration getCost: %d%n", (endTime - startTime));
        System.out.println("--------");

        return returnValue;
    }

}