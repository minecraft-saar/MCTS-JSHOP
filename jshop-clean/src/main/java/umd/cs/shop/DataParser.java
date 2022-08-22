package umd.cs.shop;

import com.google.gson.JsonObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import umd.cs.shop.costs.EstimationCost;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Parser for the world state information. Used to prepare the data so that it can be fed to the NN.
 */
public class DataParser {
    JSONObject data;
    int[] dim;
    int[] dimMin;
    org.json.simple.parser.JSONParser parser;
    float[][][][] worldMatrix;
    Boolean use_target;
    Boolean use_structures;
    int numChannels;
    EstimationCost.NNType nnType;
    EstimationCost.ScenarioType scenarioType;
    int[][] coloredBlocks;

    /**
     * @param use_target     boolean, whether target information should be in data
     * @param use_structures boolean, whether information on existing structures should be in data
     * @param numChannels    int, number of channels the data should have, depending on NN type and method
     * @param nnType         type of the NN that the data is parser for (Simple/CNN)
     * @param scenarioType   type of the used scenario (SimpleBridge/FancyBridge)
     */
    public DataParser(Boolean use_target, Boolean use_structures, int numChannels, EstimationCost.NNType nnType, EstimationCost.ScenarioType scenarioType) {
        this.parser = new org.json.simple.parser.JSONParser();
        if (scenarioType == EstimationCost.ScenarioType.SimpleBridge) {
            this.dim = new int[]{5, 3, 3}; // for simple bridge
        } else if (scenarioType == EstimationCost.ScenarioType.FancyBridge) {
            this.dim = new int[]{3, 5, 9}; // for fancy bridge {8, 70, 14}
        }
        this.dimMin = new int[]{6, 66, 6};
        this.use_target = use_target;
        this.use_structures = use_structures;
        this.numChannels = numChannels;
        this.nnType = nnType;
        this.scenarioType = scenarioType;
        this.coloredBlocks = new int[][]{{0, 0, 0}, {4, 0, 2}};
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
     *
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
        if (use_target) {
            readFromKey("target", targetCoordinates);
        }

        // read structures
        ArrayList<ArrayList<int[]>> structuresCoordinates = new ArrayList<>();
        ArrayList<int[]> coordinatesRow = new ArrayList<>();
        ArrayList<int[]> coordinatesFloor = new ArrayList<>();
        ArrayList<int[]> coordinatesRailing = new ArrayList<>();
        structuresCoordinates.add(coordinatesRow);
        structuresCoordinates.add(coordinatesFloor);
        structuresCoordinates.add(coordinatesRailing);
        if (use_structures) {
            readSpecialStructures(structuresCoordinates);
        }

        // mark block positions in data list
        markBlockPositions(blockCoordinates, targetCoordinates, structuresCoordinates);
    }

    /**
     * Reads coordinates data from a json object given a json key.
     *
     * @param key         json key such as "target"
     * @param coordinates empty arraylist into which the coordinates should be put
     */
    public void readFromKey(String key, ArrayList<int[]> coordinates) {
        JSONArray blocks = (JSONArray) data.get(key); // get list of json objects belonging to given key
        // iterate through all json objects in the list
        for (int i = 0; i < blocks.size(); i++) {
            JSONArray blockInBrackets = (JSONArray) blocks.get(i);
            String block = (String) blockInBrackets.get(0); // get type of current object
            // skip this object if it is also the current target
//            if (target.equals(block)) {
//                continue;
//            }
            // split object in order to find coordinates
            String[] splitBlock = block.split("-");
            int[] coords = new int[3];
            int[] refCoords = new int[6]; // 1st block index 0-2, 2nd block index 3-5
            // find reference coordinates and calculate the rest based on which type the object is
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
                    if (this.scenarioType == EstimationCost.ScenarioType.SimpleBridge) {
                        for (int x = coords[0]; x < coords[0] + 5; x++) { // use coord as reference block
                            coordinates.add(new int[]{x, coords[1] + 1, coords[2]});
                        } // simple bridge
                    } else if (this.scenarioType == EstimationCost.ScenarioType.FancyBridge) {
                        for (int z = coords[2]; z < coords[2] + 5; z++) { // use coord as reference block
                        coordinates.add(new int[]{coords[0], coords[1] + 1, z});
                        } // fancy bridge
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

                    // remove first and last block since those are the reference blocks TODO use filters instead
//                    coordinates.remove(0);
//                    coordinates.remove(coordinates.size() - 1);
                    break;
                case "Stairs": // untested, TODO filter colored block
                    System.out.println(splitBlock);
                    int[] refCoordsStairs = new int[18];

                    // read reference blocks into list and normalize
                    for (int j = 3; j < 6; j++) {
                        // row
                        refCoordsStairs[j - 3] = Integer.parseInt(splitBlock[j]) - dimMin[j - 3];
                        refCoordsStairs[j + 3 - 3] = Integer.parseInt(splitBlock[j + 3]) - dimMin[j - 3];
                        // lower wall
                        refCoordsStairs[j + 6 - 3] = Integer.parseInt(splitBlock[j + 8]) - dimMin[j - 3];
                        refCoordsStairs[j + 9 - 3] = Integer.parseInt(splitBlock[j + 11]) - dimMin[j - 3];
                        // higher wall
                        refCoordsStairs[j + 12 - 3] = Integer.parseInt(splitBlock[j + 16]) - dimMin[j - 3];
                        refCoordsStairs[j + 15 - 3] = Integer.parseInt(splitBlock[j + 19]) - dimMin[j - 3];
                    }
                    System.out.println(refCoordsStairs);

                    // calculate blocks
                    for (int n = refCoordsStairs[0]; n < refCoordsStairs[3] + 1; n++) { // row
                        coordinates.add(new int[]{n, refCoordsStairs[1], refCoordsStairs[2]});
                    }
                    for (int x = refCoordsStairs[6]; x < refCoordsStairs[9] + 1; x++) { // lower wall
                        for (int y = refCoordsStairs[7]; y < refCoordsStairs[10] + 1; y++) {
                            coordinates.add(new int[]{x, y, refCoordsStairs[8]});
                        }
                    }
                    for (int x = refCoordsStairs[12]; x < refCoordsStairs[15] + 1; x++) { // higher wall
                        for (int y = refCoordsStairs[13]; y < refCoordsStairs[16] + 1; y++) {
                            coordinates.add(new int[]{x, y, refCoordsStairs[14]});
                        }
                    }
                    System.out.println(coordinates);
                    break;
                case "wall": // untested, TODO filter colored block
                    System.out.println(splitBlock);
                    // get reference blocks
                    for (int j = 1; j < 4; j++) {
                        refCoords[j - 1] = Integer.parseInt(splitBlock[j]) - dimMin[j - 1];
                        refCoords[j + 3 - 1] = Integer.parseInt(splitBlock[j + 3]) - dimMin[j - 1];
                    }
                    System.out.println(refCoords);

                    // blocks in between
                    for (int x = refCoords[0]; x < (refCoords[3] + 1); x++) {
                        for (int y = refCoords[1]; y < (refCoords[4] + 1); y++) {
                            coordinates.add(new int[]{x, y, refCoords[2]});
                        }
                    }
                    System.out.println(coordinates);
                    break;
                case "row":
                    // reference blocks
                    for (int j = 1; j < 4; j++) {
                        refCoords[j - 1] = Integer.parseInt(splitBlock[j]) - dimMin[j - 1];
                        refCoords[j + 3 - 1] = Integer.parseInt(splitBlock[j + 3]) - dimMin[j - 1];
                    }

                    // blocks in between
                    if (this.scenarioType == EstimationCost.ScenarioType.SimpleBridge) {
                        for (int x = refCoords[0]; x < (refCoords[3] + 1); x++) {
                            coordinates.add(new int[]{x, refCoords[1], refCoords[2]});
                        } // simple bridge
                    } else if (this.scenarioType == EstimationCost.ScenarioType.FancyBridge) {
                        for (int z = refCoords[2]; z < (refCoords[5] + 1); z++) { // TODO untested, filter colored blocks
                            coordinates.add(new int[]{refCoords[0], refCoords[1], z});
                        } // fancy bridge
                    }
                    break;
                case "Block":
                    for (int j = 1; j < 4; j++) {
                        coords[j - 1] = Integer.parseInt(splitBlock[j]) - dimMin[j - 1];
                    }
                    coordinates.add(coords);
                    break;
                case "row-railing": // TODO is this an issue for fancy bridge scenario?
                    // notice:
                    // "row":[["row-railing6-68-6-10-68-6"]]
                    // "row":[["row6-66-6-10-66-6"],["row6-66-8-10-66-8"],["row6-68-6-10-68-6"]]
                    for (int j = 2; j < 5; j++) {
                        refCoords[j - 2] = Integer.parseInt(splitBlock[j]) - dimMin[j - 2];
                        refCoords[j + 3 - 2] = Integer.parseInt(splitBlock[j + 3]) - dimMin[j - 2];
                    }

                    // blocks in between
                    for (int x = refCoords[0]; x < (refCoords[3] + 1); x++) {
                        coordinates.add(new int[]{x, refCoords[1], refCoords[2]});
                        System.out.println(Arrays.toString(new int[]{x, refCoords[1], refCoords[2]}));
                    }
                    break;
            }
        }

    }

    /**
     * Reads the coordinates of special structures such as rows.
     *
     * @param coordinates empty arraylist of arraylists into which the coordinates should be put
     */
    public void readSpecialStructures(ArrayList<ArrayList<int[]>> coordinates) {
        // parse coordinates of different structures
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
     *
     * @param worldStateCoords coordinate list for the currently existing blocks
     * @param targetCoords     coordinate list for the target blocks of the current instruction
     * @param structureCoords  coordinate list made of three sublists, each listing coordinates for their respective
     *                         structure type
     */
    public void markBlockPositions(ArrayList<int[]> worldStateCoords, ArrayList<int[]> targetCoords, ArrayList<ArrayList<int[]>> structureCoords) {
        // int arrays are filled with zeros by default
        worldMatrix = new float[numChannels][dim[0]][dim[1]][dim[2]];

        // filter out colored blocks
        for (int[] cb : this.coloredBlocks) {
            // define necessary predicates for current colored block
            Predicate<int[]> filter_pred = ints -> !Arrays.equals(cb, ints);

            // use streams to filter colored blocks
            worldStateCoords = worldStateCoords.stream().filter(filter_pred).collect(Collectors.toCollection(ArrayList::new));
            targetCoords = targetCoords.stream().filter(filter_pred).collect(Collectors.toCollection(ArrayList::new));
            structureCoords.set(0, structureCoords.get(0).stream().filter(filter_pred).collect(Collectors.toCollection(ArrayList::new)));
            structureCoords.set(1, structureCoords.get(1).stream().filter(filter_pred).collect(Collectors.toCollection(ArrayList::new)));
            structureCoords.set(2, structureCoords.get(2).stream().filter(filter_pred).collect(Collectors.toCollection(ArrayList::new)));
        }

        // mark currently present blocks
        for (int[] indices : worldStateCoords) {
            worldMatrix[0][indices[0]][indices[1]][indices[2]] = 1F;
        }

        // mark target blocks
        ArrayList<Integer> test_idx = new ArrayList<>();
        try {
            if (use_target) {
                for (int[] indices : targetCoords) {
                    test_idx.add(indices[0]);
                    test_idx.add(indices[1]);
                    test_idx.add(indices[2]);
                    worldMatrix[1][indices[0]][indices[1]][indices[2]] = 1F;
                    test_idx.clear();
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("first: " + test_idx.get(0) + ", second: " + test_idx.get(1) + ", third: " + test_idx.get(2));
            e.printStackTrace();
        }

        // mark currently present special structures
        if (use_structures) {
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
}