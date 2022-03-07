package umd.cs.shop.costs;

import de.saar.basic.Pair;
import de.saar.coli.minecraft.MinecraftRealizer;
import de.saar.coli.minecraft.relationextractor.IntroductionMessage;
import de.saar.coli.minecraft.relationextractor.MinecraftObject;
import umd.cs.shop.*;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.djl.*;
import ai.djl.inference.*;
import ai.djl.ndarray.*;
import ai.djl.translate.*;
import java.nio.file.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class EstimationCost extends NLGCost {

    MinecraftRealizer nlgSystem; //
    Model nn;
    Translator<Float, Float> translator;
    Predictor<Float, Float> predictor;

    public EstimationCost(CostFunction.InstructionLevel ins, String weightFile) {
        super(ins, weightFile);
         nlgSystem = MinecraftRealizer.createRealizer(); //
        Path nnDir = Paths.get("../../cost-estimation/nn/");
        nn = Model.newInstance("trained_model.zip");
        try {
            nn.load(nnDir);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MalformedModelException e) {
            e.printStackTrace();
        }

        translator = new Translator<Float, Float>(){  // TODO check if the types are correct
            @Override
            public NDList processInput(TranslatorContext ctx, Float input) {
                NDManager manager = ctx.getNDManager();
                NDArray array = manager.create(new float[] {input});
                return new NDList (array);
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

    private class DataParser { // TODO add documentation in code
        JSONObject data;
        int[] dim;
        int[] dimMin;
        JSONParser parser;

        public DataParser(String jsonData) {  // TODO implement switches for target, structure etc
            parser = new JSONParser();
            try {

                Object jsonObj = parser.parse(jsonData);
                data = (JSONObject) jsonObj;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            dim = new int[]{5, 3, 3};  // TODO make dimensions more flexible through argument in init
            dimMin = new int[]{6, 66, 6};
        }

        public void convertIntoVector() {
            // read world state
            ArrayList<int[]> coordinates = new ArrayList<>();
            JSONArray blocks = (JSONArray) data.get("block");
            for (int i = 0; i < blocks.size(); i++) {
                JSONArray blockInBrackets = (JSONArray) blocks.get(i);
                String block = (String) blockInBrackets.get(0);
                String[] splitBlock = block.split("-");
                if (splitBlock.length == 4) {
                    int[] coords = new int[3];
                    for (int j = 1; j < 4; j++) {
                        coords[j-1] = Integer.parseInt(splitBlock[j]) - dimMin[j-1];
                    }
                    System.out.println(Arrays.toString(coords));
                    coordinates.add(coords);
                }
            }

            // read target

            // read structures

            // add fake costs

            // mark block positions in data list

        }

        // TODO read from key method

        // TODO read special structures method

        // TODO mark blocks method
    }

    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {

        if(groundedOperator.get(0).equals("!place-block-hidden")||
                groundedOperator.get(0).equals("!remove-it-row") ||
                groundedOperator.get(0).equals("!remove-it-railing") ||
                groundedOperator.get(0).equals("!remove-it-stairs") ||
                groundedOperator.get(0).equals("!remove-it-wall")) {
            return 0.0;
        }
        MinecraftObject currentObject = createCurrentMinecraftObject(op, groundedOperator);
        Set<String> knownObjects = new HashSet<>();
        Pair<Set<MinecraftObject>,Set<MinecraftObject>> pair = createWorldFromState(state, knownObjects, currentObject);
        Set<MinecraftObject> world = pair.getRight(); //
        Set<MinecraftObject> it = pair.getLeft(); //
        if(currentObject instanceof IntroductionMessage intro){
            if(knownObjects.contains(intro.name)){
                //make dead end
                return 30000.0;
            } else {
                return 0.000001;
            }
        }
        // call NN python script here
        //calling nlgsysstem for model:
        String model = nlgSystem.getModelforNN(world, currentObject, it); //
        //new way
//        String model = this.model;  //
        // add missing \" to string
//        System.out.println(model);

        DataParser parser = new DataParser(model);
        parser.convertIntoVector();

        Pattern pattern = Pattern.compile("(\"[a-zA-Z_\\-\\d]+)(\")");
        Matcher matcher = pattern.matcher(model);
        model = matcher.replaceAll("\\\\$1\\\\$2");
        model = "[" + model + "]";
//        System.out.println(model);

        // I guess ProcessBuilder init input can be understood as the line you would put into the command line
        ProcessBuilder pb = new ProcessBuilder("python", "../../cost-estimation/nn/main.py", "-c", "-d " + model);
        pb.directory(new File("../../cost-estimation/nn"));
        pb.redirectErrorStream(true);
        Process process = null;
        System.out.println("----------");
//        try {
//            predictor.predict(model);  // TODO create a json parser for the model in java
//        } catch (TranslateException e) {
//            e.printStackTrace();
//        }
        System.out.println("----------");
        double returnValue = Double.POSITIVE_INFINITY;
        try {
            process = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String ret;
            while ((ret = in.readLine()) != null) {
                System.out.println(ret);
                returnValue = Double.parseDouble(ret);
            }
            int exitCode = process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return returnValue;
    }

}