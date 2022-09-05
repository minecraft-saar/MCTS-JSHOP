package umd.cs.shop;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import umd.cs.shop.costs.EstimationCost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class DataParserNeutral extends DataParser {

    String[] specialStructures;
    int numStructures;

    /**
     * @param use_target     boolean, whether target information should be in data
     * @param use_structures boolean, whether information on existing structures should be in data
     * @param numChannels    int, number of channels the data should have, depending on NN type and method
     * @param nnType         type of the NN that the data is parser for (Simple/CNN)
     * @param scenarioType   type of the used scenario (SimpleBridge/FancyBridge)
     */
    public DataParserNeutral(Boolean use_target, Boolean use_structures, int numChannels, EstimationCost.NNType nnType, EstimationCost.ScenarioType scenarioType) {
        super(use_target, use_structures, numChannels, nnType, scenarioType);

        // structures and colored blocks for different scenario types, add new ones here
        if (scenarioType == EstimationCost.ScenarioType.SimpleBridge) {
            this.specialStructures = new String[]{"row", "floor", "railing"};
            this.coloredBlocks = new int[][]{{0, 0, 0}, {4, 0, 2}};
            this.dimMin = new int[]{6, 66, 6};
        } else if (scenarioType == EstimationCost.ScenarioType.FancyBridge) {
            this.specialStructures = new String[]{"row", "floor", "railing", "wall", "stairs"};
            this.coloredBlocks = new int[][]{{}};  // TODO
//            this.dimMin = new int[]{}; TODO
        } else {
            this.specialStructures = new String[]{};
        }

        // get number of structures
        this.numStructures = this.specialStructures.length;
    }

    /**
     * Converts the json object of the world state into a (multi-channel) 3D matrix containing information on
     * whether certain blocks or structures exist at certain coordinates or should be built; ignores colored blocks.
     * The result is saved in the parser.
     */
    @Override
    public void convertIntoVector() {
        // read world state
        ArrayList<int[]> blockCoordinates = new ArrayList<>();
        JSONArray blocks = (JSONArray) data.get("block"); // get list of blocks
        readBlocks(blocks, blockCoordinates);

        // read target
        ArrayList<int[]> targetCoordinates = new ArrayList<>();
        if (use_target) {
            readFromKey("target", targetCoordinates);
        }

        // read structures
        ArrayList<ArrayList<int[]>> structuresCoordinates = new ArrayList<>();
//        ArrayList<int[]> coordinatesRow = new ArrayList<>();
//        ArrayList<int[]> coordinatesFloor = new ArrayList<>();
//        ArrayList<int[]> coordinatesRailing = new ArrayList<>();
//        structuresCoordinates.add(coordinatesRow);
//        structuresCoordinates.add(coordinatesFloor);
//        structuresCoordinates.add(coordinatesRailing);
        for (int i = 0; i < this.numStructures; i++) {
            structuresCoordinates.add(new ArrayList<>());
        }
        if (use_structures) {
            readSpecialStructures(structuresCoordinates);
        }

        // mark block positions in data list
        markBlockPositions(blockCoordinates, targetCoordinates, structuresCoordinates);
    }

    /**
     * Reads the coordinates of special structures such as rows.
     *
     * @param coordinates empty arraylist of arraylists into which the coordinates should be put
     */
    @Override
    public void readSpecialStructures(ArrayList<ArrayList<int[]>> coordinates) {
        // parse coordinates of different structures
//        if (data.containsKey("row")) {
//            readFromKey("row", coordinates.get(0));
//        }
//        if (data.containsKey("floor")) {
//            readFromKey("floor", coordinates.get(1));
//        }
//        if (data.containsKey("railing")) {
//            readFromKey("railing", coordinates.get(2));
//        }
        for (int idx = 0; idx < this.numStructures; idx++) {
            String key = this.specialStructures[idx];
            if (data.containsKey(key)) {
                readFromKey(key, coordinates.get(idx));
            }
        }
    }

    /**
     * Reads coordinates data from a json object given a json key.
     *
     * @param key         json key such as "target"
     * @param coordinates empty arraylist into which the coordinates should be put
     */
    @Override
    public void readFromKey(String key, ArrayList<int[]> coordinates) {
        JSONArray blocks = (JSONArray) data.get(key); // get list of json objects belonging to given key
        JSONObject children = (JSONObject) ((JSONArray) blocks.get(0)).get(0);
        blocks = (JSONArray) children.get("children");

        readBlocks(blocks, coordinates);
    }

    public void readBlocks(JSONArray blocks, ArrayList<int[]> coordinates) {
        // iterate through all json objects in the list
        for (int i = 0; i < blocks.size(); i++) {
            JSONArray blockInBrackets = (JSONArray) blocks.get(i);
            String block = (String) blockInBrackets.get(0);

            // split object
            String[] splitBlock = block.split("-");
            if (!Objects.equals(splitBlock[0], "Block")) { // ignore colored blocks
                continue;
            }

            // convert to int to get coordinates
            int x = Integer.parseInt(splitBlock[1]);
            int y = Integer.parseInt(splitBlock[2]);
            int z = Integer.parseInt(splitBlock[3]);
            int[] coords = new int[]{x, y, z};

            // normalize coordinates
            for (int j = 0; j < 3; j++) {
                coords[j] = coords[j] - dimMin[j];
            }
            coordinates.add(coords);
        }
    }


}
