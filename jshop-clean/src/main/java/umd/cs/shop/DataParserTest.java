package umd.cs.shop;

import org.junit.jupiter.api.Test;
import umd.cs.shop.costs.EstimationCost;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

class DataParserTest {

    DataParserNeutral neutralParserToTest;
    String neutralData;
    DataParser parserToTest;
    String data;

    /**
     * Test for DataParserNeutral, although it can easily be reconfigured for testing DataParser.
     */
    public DataParserTest() {
        // parser arguments
        Boolean use_target = true;
        Boolean use_structures = true;
        int numChannels = 5;
        EstimationCost.NNType nnType = EstimationCost.NNType.CNN;
        EstimationCost.ScenarioType scenarioType = EstimationCost.ScenarioType.SimpleBridge;

        // neutral parser
        this.neutralParserToTest = new DataParserNeutral(use_target, use_structures, numChannels, nnType, scenarioType);
        try {
            this.neutralData = Files.readString(Path.of("test_neutral.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.neutralParserToTest.setNewData(this.neutralData);

        // parser
        this.parserToTest = new DataParser(use_target, use_structures, numChannels, nnType, scenarioType);
        try {
            this.data = Files.readString(Path.of("test_old.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.parserToTest.setNewData(this.data);
    }

    /**
     * Visualizes the world read from test_neutral.json so that a human can easily verify whether the neutral parser works as intended.
     * Keep in mind that the matrix does not have the same order as the minecraft world itself, so imagine flipping the matrix horizontally (I'm sorry.)
     *
     * The output means the following:
     * B - normal block
     * T - target block
     * W - row block
     * F - floor block
     * R - railing block
     */
    @Test
    void testConvertIntoVectorNeutral() {
        String worldState = visualizeParser(this.neutralParserToTest);
        System.out.println("NEUTRAL PARSER:");
        System.out.println(worldState);
    }

    /**
     * Visualizes the world read from test_old.json so that a human can easily verify whether the old parser works as intended.
     * Keep in mind that the matrix does not have the same order as the minecraft world itself, so imagine flipping the matrix horizontally (I'm sorry.)
     *
     * The output means the following:
     * B - normal block
     * T - target block
     * W - row block
     * F - floor block
     * R - railing block
     */
    @Test
    void testConvertIntoVector() {
        String worldState = visualizeParser(this.parserToTest);
        System.out.println("OLD PARSER:");
        System.out.println(worldState);
    }

    /**
     * Compares the output of the two parsers and only outputs their outputs if they differ.
     *
     * The output means the following:
     * B - normal block
     * T - target block
     * W - row block
     * F - floor block
     * R - railing block
     */
    @Test
    void testCompareParsers() {
        try {
            // prepare file and readers
            File oldFile = new File("test_old_complete.json");
            File newFile = new File("test_neutral_complete.json");
            FileReader oldReader = new FileReader(oldFile);
            FileReader newReader = new FileReader(newFile);
            BufferedReader oldBufferedReader = new BufferedReader(oldReader);
            BufferedReader newBufferedReader = new BufferedReader(newReader);
            String oldLine;
            String newLine;

            // read every line separately, assumes files are of equal length (if they weren't this comparison would be pointless anyway)
            while((oldLine = oldBufferedReader.readLine()) != null) {
                newLine = newBufferedReader.readLine();

                // remove comma at end of line
                oldLine = oldLine.substring(0, oldLine.length() - 1);
                newLine = newLine.substring(0, newLine.length() - 1);

                this.parserToTest.setNewData(oldLine);
                this.neutralParserToTest.setNewData(newLine);

                // get visualization
                String worldStateOld = visualizeParser(this.parserToTest);
                String worldStateNew = visualizeParser(this.neutralParserToTest);

                // compare the two
                assert worldStateOld.equals(worldStateNew);
            }

            oldReader.close();
            newReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Visualizes the parsed world state of a given parser.
     * Keep in mind that the matrix does not have the same order as the minecraft world itself, so imagine flipping the matrix horizontally (I'm sorry.)
     *
     * @param parser, either DataParser or NewDataParser, parser that is visualized
     * @return String depicting the world state in a somewhat readable form
     */
    String visualizeParser(DataParser parser) {
        parser.convertIntoVector();
        float[][][][] worldMatrix = parser.getMatrix();

        // convert worldMatrix into something that works as a representation
        String[][][] world = new String[5][3][3];
        // set blocks
        for (int xAxis = 0; xAxis < worldMatrix[0].length; xAxis++) {
            for (int yAxis = 0; yAxis < worldMatrix[0][0].length; yAxis++) {
                for (int zAxis = 0; zAxis < worldMatrix[0][0][0].length; zAxis++) {
                    if (worldMatrix[0][xAxis][yAxis][zAxis] != 0.0F) {
                        world[xAxis][yAxis][zAxis] = "B";
                    } else {
                        world[xAxis][yAxis][zAxis] = "0";
                    }
//                    System.out.println(worldMatrix[0][xAxis][yAxis][zAxis]);
                }
            }
        }
        // set target
        for (int xAxis = 0; xAxis < worldMatrix[1].length; xAxis++) {
            for (int yAxis = 0; yAxis < worldMatrix[1][0].length; yAxis++) {
                for (int zAxis = 0; zAxis < worldMatrix[1][0][0].length; zAxis++) {
                    if (worldMatrix[1][xAxis][yAxis][zAxis] != 0.0F) {
                        world[xAxis][yAxis][zAxis] = "T";
                    }
                }
            }
        }
        // set structures
        String[] structureNames = new String[]{"B", "T", "W", "F", "R"}; // blocks, target, row, floor, railing
        for (int i = 2; i < worldMatrix.length; i++) {
            for (int xAxis = 0; xAxis < worldMatrix[i].length; xAxis++) {
                for (int yAxis = 0; yAxis < worldMatrix[1][0].length; yAxis++) {
                    for (int zAxis = 0; zAxis < worldMatrix[1][0][0].length; zAxis++) {
                        if (worldMatrix[i][xAxis][yAxis][zAxis] != 0.0F) {
                            if (world[xAxis][yAxis][zAxis] == "B") {
                                world[xAxis][yAxis][zAxis] = structureNames[i];
                            } else {
                                world[xAxis][yAxis][zAxis] += structureNames[i];
                            }
                        }
                    }
                }
            }
        }

        // visualize world
        String worldState = "";
        for (String[][] xAxis : world) {
            for (String[] yAxis : xAxis) {
                for (String zAxis : yAxis) {
                    worldState += zAxis + " ";
                }
                worldState += "\n";
            }
            worldState += "\n--------------\n";
        }
        return worldState;
    }
}