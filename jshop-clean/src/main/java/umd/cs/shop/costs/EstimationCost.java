package umd.cs.shop.costs;

import ai.djl.ndarray.types.Shape;
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
    Translator<Float[], Float> translator;
    Predictor<Float[], Float> predictor;
    DataParser parser;

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
        parser = new DataParser();

        translator = new Translator<Float[], Float>() {  // TODO check if translator works properly
            @Override
            public NDList processInput(TranslatorContext ctx, Float[] input) {
                NDManager manager = ctx.getNDManager();
//                NDArray array = manager.create(new float[] {input});
                Shape shape = new Shape(5, 5, 3, 3);
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
     * Parser for the world state information.
     */
    private class DataParser {
        JSONObject data;
        int[] dim;
        int[] dimMin;
        JSONParser parser;
        float[][][][] worldMatrix;

        public DataParser() {  // TODO implement switches for target, structure etc
            parser = new JSONParser();
            dim = new int[]{5, 3, 3};  // TODO make dimensions more flexible through argument in init
            dimMin = new int[]{6, 66, 6};
        }

        /**
         * Sets a new world state in the parser so that it can be processed further.
         *
         * @param jsonData string containing the new world state in json format
         */
        public void setNewData(String jsonData) {
            try {
                Object jsonObj = parser.parse(jsonData);
                data = (JSONObject) jsonObj;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        /**
         * Should only be used after using convertIntoVector; Returns the finished multi-channel 3D matrix portraying
         * the world state.
         * @return multi-channel 3D matrix
         */
        public float[][][][] getMatrix() {
            return worldMatrix;
        }

        /**
         * Converts the json object of the world state into a (multi-channel) 3D matrix containing information on
         * whether certain blocks or structures exist at certain coordinates or should be built; ignores colored blocks.
         * The result is saved in the parser.
         */
        public void convertIntoVector() {
            // read world state
            ArrayList<int[]> blockCoordinates = new ArrayList<>();
            readFromKey("block", blockCoordinates);

            // read target
            ArrayList<int[]> targetCoordinates = new ArrayList<>();
            readFromKey("target", targetCoordinates);

            // read structures
            ArrayList<ArrayList<int[]>> structuresCoordinates = new ArrayList<>();
            ArrayList<int[]> coordinatesRow = new ArrayList<>();
            ArrayList<int[]> coordinatesFloor = new ArrayList<>();
            ArrayList<int[]> coordinatesRailing = new ArrayList<>();
            structuresCoordinates.add(coordinatesRow);
            structuresCoordinates.add(coordinatesFloor);
            structuresCoordinates.add(coordinatesRailing);
            readSpecialStructures(structuresCoordinates);

            // mark block positions in data list
            markBlockPositions(blockCoordinates, targetCoordinates, structuresCoordinates);
        }

        /**
         * Reads coordinates data from a json object given a json key.
         *
         * @param key         json key such as "target"
         * @param coordinates empty arraylist into which the coordinates should be put
         */
        private void readFromKey(String key, ArrayList<int[]> coordinates) {
            JSONArray blocks = (JSONArray) data.get(key);
            for (int i = 0; i < blocks.size(); i++) {
                JSONArray blockInBrackets = (JSONArray) blocks.get(i);
                String block = (String) blockInBrackets.get(0);
                String[] splitBlock = block.split("-");
//                System.out.println(splitBlock[0]);
                int[] coords = new int[3];
                int[] refCoords = new int[6]; // 1st block index 0-2, 2nd block index 3-5
                switch (splitBlock[0]) {
                    case "Railing":
                        // support blocks
                        int[] coords2 = new int[3];
                        for (int j = 2; j < 5; j++) {
                            coords[j - 2] = Integer.parseInt(splitBlock[j]) - dimMin[j - 2];
                            coords2[j - 2] = Integer.parseInt(splitBlock[j + 4]) - dimMin[j - 2];
                        }
                        coordinates.add(coords);
                        coordinates.add(coords2);

                        // row
                        for (int j = coords[0]; j < coords[0] + 5; j++) { // use coord as reference block
                            coordinates.add(new int[]{j, coords[1] + 1, coords[2]});
                        }
                        break;
                    case "floor":
                        // reference blocks
                        for (int j = 1; j < 4; j++) {
                            refCoords[j - 1] = Integer.parseInt(splitBlock[j]) - dimMin[j - 1];
                            refCoords[j + 3 - 1] = Integer.parseInt(splitBlock[j + 3]) - dimMin[j - 1];
                        }

                        // blocks in between
                        for (int x = refCoords[0]; x < (refCoords[3] + 1); x++) {
                            for (int z = refCoords[2]; z < (refCoords[5] + 1); z++) {
                                coordinates.add(new int[]{x, refCoords[1], z});
                            }
                        }
                        // remove first and last block since those are the reference blocks
                        coordinates.remove(0);
                        coordinates.remove(coordinates.size() - 1);
                        break;
                    case "row": // TODO probably untested; does not ignore colored blocks since python version doesn't either
                        // reference blocks
                        System.out.println("Row: ----------");
                        System.out.println(splitBlock[0]);
                        for (int j = 1; j < 4; j++) {
                            refCoords[j - 1] = Integer.parseInt(splitBlock[j]) - dimMin[j - 1];
                            refCoords[j + 3 - 1] = Integer.parseInt(splitBlock[j + 3]) - dimMin[j - 1];
                        }

                        // blocks in between
                        for (int x = refCoords[0]; x < (refCoords[3] + 1); x++) {
                            coordinates.add(new int[]{x, refCoords[1], refCoords[2]});
                            System.out.println(Arrays.toString(new int[]{x, refCoords[1], refCoords[2]}));
                        }
                        System.out.println("----------");
                        break;
                    case "Block":
                        for (int j = 1; j < 4; j++) {
                            coords[j - 1] = Integer.parseInt(splitBlock[j]) - dimMin[j - 1];
                        }
//                    System.out.println(Arrays.toString(coords));
                        coordinates.add(coords);
                        break;
                    case "row-railing": // TODO maybe find another way to deal with this, like changing the data format
                        // TODO also untested since case happens so rarely
                        // notice:
                        // "row":[["row-railing6-68-6-10-68-6"]]
                        // "row":[["row6-66-6-10-66-6"],["row6-66-8-10-66-8"],["row6-68-6-10-68-6"]]
                        System.out.println("Row-Railing: ----------");
                        System.out.println(splitBlock[0]);

                        for (int j = 2; j < 5; j++) {
                            refCoords[j - 2] = Integer.parseInt(splitBlock[j]) - dimMin[j - 2];
                            refCoords[j + 3 - 2] = Integer.parseInt(splitBlock[j + 3]) - dimMin[j - 2];
                        }

                        // blocks in between
                        for (int x = refCoords[0]; x < (refCoords[3] + 1); x++) {
                            coordinates.add(new int[]{x, refCoords[1], refCoords[2]});
                            System.out.println(Arrays.toString(new int[]{x, refCoords[1], refCoords[2]}));
                        }
                        System.out.println("----------");
                        break;
                }
            }

        }

        /**
         * Reads the coordinates of special structures such as rows.
         *
         * @param coordinates empty arraylist of arraylists into which the coordinates should be put
         */
        private void readSpecialStructures(ArrayList<ArrayList<int[]>> coordinates) {
            if (data.containsKey("row")) {
                readFromKey("row", coordinates.get(0));
            }
            if (data.containsKey("floor")) {
                readFromKey("floor", coordinates.get(1));
            }
            if (data.containsKey("railing")) {
                readFromKey("railing", coordinates.get(2));
            }
        }

        /**
         * Converts the coordinate lists into a multi-channel 3D matrix representing the world state and plan intention.
         * The first channel represents the current blocks in the world, 0 meaning there is none and 1 meaning there is
         * one. The second channel represents what the current instruction plans to build. The last three channels
         * represent whether certain structures, such as railings, have been built before, as this will lower the cost
         * when attempting to build them again.
         * @param worldStateCoords coordinate list for the currently existing blocks
         * @param targetCoords coordinate list for the target blocks of the current instruction
         * @param structureCoords coordinate list made of three sublists, each listing coordinates for their respective
         *                        structure type
         */
        private void markBlockPositions(ArrayList<int[]> worldStateCoords, ArrayList<int[]> targetCoords, ArrayList<ArrayList<int[]>> structureCoords) {
            // int arrays are filled with zeros by default
            worldMatrix = new float[5][5][3][3]; // TODO check if dimensions are correct and if these are really all zeros

            // mark currently present blocks
            for (int[] indices : worldStateCoords) {
                worldMatrix[0][indices[0]][indices[1]][indices[2]] = 1F;
            }

            // mark target blocks
            for (int[] indices : targetCoords) {
                worldMatrix[1][indices[0]][indices[1]][indices[2]] = 1F;
            }

            // mark currently present special structures
            int matrixIdx = 2;
            for (ArrayList<int[]> structureType : structureCoords) {
                if (!structureType.isEmpty()) {
                    for (int[] coords : structureType) {
                        worldMatrix[matrixIdx][coords[0]][coords[1]][coords[2]] = 1F;
                    }
                }
                matrixIdx++;
            }
        }
    }

    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {

        if (groundedOperator.get(0).equals("!place-block-hidden") ||
                groundedOperator.get(0).equals("!remove-it-row") ||
                groundedOperator.get(0).equals("!remove-it-railing") ||
                groundedOperator.get(0).equals("!remove-it-stairs") ||
                groundedOperator.get(0).equals("!remove-it-wall")) {
            return 0.0;
        }
        MinecraftObject currentObject = createCurrentMinecraftObject(op, groundedOperator);
        Set<String> knownObjects = new HashSet<>();
        Pair<Set<MinecraftObject>, Set<MinecraftObject>> pair = createWorldFromState(state, knownObjects, currentObject);
        Set<MinecraftObject> world = pair.getRight(); //
        Set<MinecraftObject> it = pair.getLeft(); //
        if (currentObject instanceof IntroductionMessage intro) {
            if (knownObjects.contains(intro.name)) {
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
//        System.out.println(model);

        // process world state data by using a parser
        parser.setNewData(model);
        parser.convertIntoVector();
        float[][][][] inputDataNN = parser.getMatrix();

        // prepare world state representation for python argparse
        Pattern pattern = Pattern.compile("(\"[a-zA-Z_\\-\\d]+)(\")");
        Matcher matcher = pattern.matcher(model);
        model = matcher.replaceAll("\\\\$1\\\\$2");
        model = "[" + model + "]";
//        System.out.println(model);

        // flatten world state matrix in preparation for NDArray
        // TODO check if all of this works as intended together with translator
        Float[] flattenedInputDataNN = new Float[5 * 5 * 3 * 3];
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
//        System.out.println(Arrays.toString(flattenedInputDataNN));

        // I guess ProcessBuilder init input can be understood as the line you would put into the command line
//        ProcessBuilder pb = new ProcessBuilder("python", "../../cost-estimation/nn/main.py", "-c", "-d " + model, "-l");
//        pb.directory(new File("../../cost-estimation/nn"));
//        pb.redirectErrorStream(true);
//        Process process = null;
        double returnValue = Double.POSITIVE_INFINITY;
        try {
            returnValue = predictor.predict(flattenedInputDataNN);
            // inverse scaling
            returnValue = (returnValue - 0D) / (1D - 0D);
            returnValue = returnValue * (128071.40159593D - 2690.70126898D) + 2690.70126898D;
        } catch (TranslateException e) {
            e.printStackTrace();
        }
//        try {
//            process = pb.start();
//            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String ret;
//            while ((ret = in.readLine()) != null) {
//                System.out.println(ret);
//                returnValue = Double.parseDouble(ret);
//            }
//            int exitCode = process.waitFor();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        return returnValue;
    }

}