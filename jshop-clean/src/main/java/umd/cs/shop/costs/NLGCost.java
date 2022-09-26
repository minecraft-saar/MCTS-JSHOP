package umd.cs.shop.costs;

import de.saar.basic.Pair;
import de.saar.coli.minecraft.relationextractor.*;
import de.saar.coli.minecraft.MinecraftRealizer;
import de.saar.minecraft.analysis.WeightEstimator;
import umd.cs.shop.*;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class NLGCost implements CostFunction {

    MinecraftRealizer nlgSystem;
    CostFunction.InstructionLevel instructionLevel;
    protected WeightEstimator.WeightResult weights;
    boolean weightsPresent;
    Double lowestCost;
    public FileWriter NNData;
    public boolean writeNNData = true;
    String model = "";

    public NLGCost(CostFunction.InstructionLevel ins, String weightFile) {
        instructionLevel = ins;
        nlgSystem = MinecraftRealizer.createRealizer();
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
        JSUtil.println(nlgSystem.getWeightsAsJson());

        if (writeNNData) {
            try {
                File yourFile = new File("E:\\Bachelor_Arbeit\\jshop-cost-estimation\\jshop-clean\\fancy_bridge_data_neutral_format.json");
                yourFile.createNewFile(); // if file already exists will do nothing
                NNData = new FileWriter(yourFile);
                NNData.write("{");
            } catch (IOException e) {
                System.out.println("An error occurred while opening NN-data file");
                e.printStackTrace();
            }
        }
    }

    public void closeFile() {
        try {
            NNData.flush();
            NNData.close();
        } catch (IOException e) {
            System.out.println("Problem while closing NNData File");
            e.printStackTrace();
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
        MinecraftObject currentObject = createCurrentMinecraftObject(groundedOperator);
        Set<String> knownObjects = new HashSet<>();
        Pair<Set<MinecraftObject>, Set<MinecraftObject>> pair = createWorldFromState(state, knownObjects, currentObject);
        Set<MinecraftObject> world = pair.getRight();
        Set<MinecraftObject> it = pair.getLeft();
        if (currentObject instanceof IntroductionMessage intro) {
            if (knownObjects.contains(intro.name)) {
                //make unattractive
                return 80000.0;
            } else {
                return 0.000001;
            }
        }
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
//        world.addAll(currentObject.getChildren());
//        world.add(currentObject);
        double returnValue = nlgSystem.estimateCostForPlanningSystem(world, currentObject, it);

        if (writeNNData) {
            model = model.substring(0, model.length() - 1);
            model = model + ",\"cost\":[[" + returnValue + "]]}";
            try {
                NNData.write(model);
                NNData.write(",\n");
            } catch (IOException e) {
                System.out.println("An error occurred while writing NN-data file");
                e.printStackTrace();
            }
        }

        if (returnValue < 0.0) {
            if (returnValue < lowestCost) {
                lowestCost = returnValue;
                JSUtil.println(lowestCost.toString());
            }
        }
        if (objectFirstOccurence) {
            knownObjects.add(currentObjectType);
            // reset weights
            if (weights != null && weights.weights.containsKey("i" + currentObjectType)) {
                nlgSystem.setExpectedDurations(
                        Map.of("i" + currentObjectType, weights.weights.get("i" + currentObjectType)),
                        false);
            }
        }

        return returnValue;
    }

    @Override
    public boolean isUnitCost() {
        return false;
    }

    public Pair<Set<MinecraftObject>, Set<MinecraftObject>> createWorldFromState(JSTState state, Set<String> knownObjects, MinecraftObject currentObject) {
        Set<MinecraftObject> world = new HashSet<>();
        Set<MinecraftObject> it = new HashSet<>();
        JSTerm tmp;
        Pair<Boolean, Triple> itStairs = checkForIt(state, "it-staircase");
        Pair<Boolean, Triple> itRailing = checkForIt(state, "it-railing");
        Pair<Boolean, Triple> itWall = checkForIt(state, "it-wall");
        Pair<Boolean, Triple> itRow = checkForIt(state, "it-row");
        boolean foundItStairs = !itStairs.left;
        boolean foundItRailing = !itRailing.left;
        boolean foundItWall = !itWall.left;
        boolean foundItRow = !itRow.left;
        LinkedList<String> blocks = new LinkedList<>();
        LinkedList<String> row = new LinkedList<>();
        LinkedList<String> floor = new LinkedList<>();
        LinkedList<String> railing = new LinkedList<>();
        LinkedList<String> wall = new LinkedList<>();
        LinkedList<String> staircase = new LinkedList<>();
//        if (!(currentObject instanceof IntroductionMessage)) {
//            if (currentObject instanceof Block) {
//                blocks.add("[\"" + currentObject + "\"]");
//            } else if (currentObject instanceof Railing) {
//                railing.add("[\"" + currentObject + "\"]");
//            } else if (currentObject instanceof Floor) {
//                floor.add("[\"" + currentObject + "\"]");
//            } else if (currentObject instanceof Row) {
//                row.add("[\"" + currentObject + "\"]");
//            } else if (currentObject instanceof Stairs) {
//                staircase.add("[\"" + currentObject + "\"]");
//            } else if (currentObject instanceof Wall) {
//                wall.add("[\"" + currentObject + "\"]");
//            }
//        }
        for (JSPredicateForm term : state.state().atoms()) {
            String name = (String) term.elementAt(0);
            MinecraftObject mco;
            String type;
            int x, y, z;
            switch (name) {
                case "block-at" -> {
                    tmp = (JSTerm) term.elementAt(1);
                    type = (String) tmp.elementAt(0);
                    if (type.equals("water")) {
                        // water behaves differently from normal blocks,
                        // e.g. you can still put blocks into water blocks
                        // this confuses the tracking and we therefore ignore water.
                        continue;
                    }
                    tmp = (JSTerm) term.elementAt(2);
                    x = (int) Double.parseDouble(tmp.toStr().toString());
                    tmp = (JSTerm) term.elementAt(3);
                    y = (int) Double.parseDouble(tmp.toStr().toString());
                    tmp = (JSTerm) term.elementAt(4);
                    z = (int) Double.parseDouble(tmp.toStr().toString());
                    //System.out.println("Block: " + type + " " + x + " " + y + " "+ z);
                    if (type.equals("stone")) {
                        mco = new Block(x, y, z);
                        world.add(mco);
                        knownObjects.add(mco.getClass().getSimpleName().toLowerCase());
                    } else {
                        mco = new UniqueBlock(type, x, y, z);
                        world.add(mco);
                    }
                    blocks.add("[\"" + mco + "\"]");
                }
                case "last-placed" -> {
                    tmp = (JSTerm) term.elementAt(1);
                    x = (int) Double.parseDouble(tmp.toStr().toString());
                    tmp = (JSTerm) term.elementAt(2);
                    y = (int) Double.parseDouble(tmp.toStr().toString());
                    tmp = (JSTerm) term.elementAt(3);
                    z = (int) Double.parseDouble(tmp.toStr().toString());
                    //System.out.println("Block: " + type + " " + x + " " + y + " "+ z);
                    if (!(x == 100 && y == 100 && z == 100)) {
                        it.add(new Block(x, y, z));
                    }
                }
                case "it-row", "it-railing", "it-wall", "it-staircase" -> {
                }
                case "wall-at" -> {
                    mco = createWall(term);
                    world.add(mco);
                    knownObjects.add(mco.getClass().getSimpleName().toLowerCase());
                    this.formatForJSON(wall, mco);
//                    wall.add("[\"" + mco + "\"]");
                    if (itWall.left) {
                        Triple wallCoord = parseCoordinates(term);
                        if (wallCoord.equals(itWall.right)) {
                            it.add(mco);
                            foundItWall = true;
                        }
                    }
                }
                case "row-at" -> {
                    mco = createRow(term);
                    world.add(mco);
                    knownObjects.add(mco.getClass().getSimpleName().toLowerCase());
                  this.formatForJSON(row, mco);
//                    row.add("[\"" + mco + "\"]");
                    if (itRow.left) {
                        Triple rowCoord = parseCoordinates(term);
                        if (rowCoord.equals(itRow.right)) {
                            it.add(mco);
                            foundItRow = true;
                        }
                    }
                }
                case "railing-at" -> {
                    mco = createRailing(term);
                    world.add(mco);
                    knownObjects.add(mco.getClass().getSimpleName().toLowerCase());
                    this.formatForJSON(railing, mco);
//                    railing.add("[\"" + mco + "\"]");
                    if (itRailing.left) {
                        Triple railingCoord = parseCoordinates(term);
                        if (railingCoord.equals(itRailing.right)) {
                            it.add(mco);
                            foundItRailing = true;
                        }
                    }
                }
                case "floor-at" -> {
                    mco = createFloor(term);
                    world.add(mco);
                    it.add(mco); //Floor is a special case right now, because we can only have one the it never changes
                    knownObjects.add(mco.getClass().getSimpleName().toLowerCase());
                    this.formatForJSON(floor, mco);
//                    floor.add("[\"" + mco + "\"]");
                }
                case "stairs-at" -> {
                    mco = createStairs(term);
                    world.add(mco);
                    knownObjects.add(mco.getClass().getSimpleName().toLowerCase());
                    this.formatForJSON(staircase, mco);
//                    staircase.add("[\"" + mco + "\"]");
                    if (itStairs.left) {
                        Triple stairsCoord = parseCoordinates(term);
                        if (stairsCoord.equals(itStairs.right)) {
                            it.add(mco);
                            foundItStairs = true;
                        }
                    }
                }
            }
        }
        model = "{\"block\":";
        model = model + blocks + ",";
        if (!row.isEmpty()) {
            model = model + "\"row\":" + row + ",";
        }
        if (!floor.isEmpty()) {
            model = model + "\"floor\":" + floor + ",";
        }
        if (!railing.isEmpty()) {
            model = model + "\"railing\":" + railing + ",";
        }
        if (!wall.isEmpty()) {
            model = model + "\"wall\":" + wall + ",";
        }
        if (!staircase.isEmpty()) {
            model = model + "\"staircase\":" + staircase + ",";
        }
        model = model + "\"target\":" + this.formatTargetForJSON(currentObject) + "}";
//        model = model + "\"target\":[[\"" + currentObject.toString() + "\"]]}";
        if (!foundItWall || !foundItRailing || !foundItRow || !foundItStairs) {
            JSUtil.println("could not find an it-object");
            System.exit(-1);
        }
        return new Pair<>(it, world);
    }

    /**
     * Small helper function that correctly formats an object and its children (blocks) for usage in the json file.
     *
     * @param objectList list for current specific object that stores everything for the json file
     * @param mco MinecraftObject of the current object
     */
    void formatForJSON(LinkedList<String> objectList, MinecraftObject mco) {
        objectList.add("[{\"object\":[\"" + mco +"\"], \"children\":[");
        for (Block b : mco.getBlocks()) {
            objectList.set(objectList.size() - 1, objectList.getLast() + "[\"" + b + "\"],");
        }
        objectList.set(objectList.size() - 1, objectList.getLast().substring(0, objectList.getLast().length() - 1));
        objectList.set(objectList.size() - 1, objectList.getLast() + "]}]");
    }

    /**
     * Small helper function that correctly formats a target object and its children (blocks) for usage in the json file.
     *
     * @param currentObject MinecraftObject of the current target object
     */
    String formatTargetForJSON(MinecraftObject currentObject) {
        String targetJSON = "[[{\"object\":[\"" + currentObject +"\"], \"children\":[";
        for (Block b : currentObject.getBlocks()) {
            targetJSON += "[\"" + b + "\"],";
        }
        targetJSON = targetJSON.substring(0, targetJSON.length() - 1);
        targetJSON += "]}]]";
        return targetJSON;
    }

    public MinecraftObject createFloor(JSPredicateForm term) {
        int x1, x2, y1, z1, z2, length, width, dir;
        JSTerm tmp = (JSTerm) term.elementAt(1);
        x1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(2);
        y1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(3);
        z1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(4);
        length = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(5);
        width = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(6);
        dir = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        //east=1=>x+, west=2=>x-, north=3=>z-, south=4=>z+
        if (dir == 1) {
            x2 = x1 + width - 1;
            z2 = z1 + length - 1;
        } else if (dir == 2) {
            x2 = x1;
            x1 = x1 - width + 1;
            z2 = z1 + length - 1;
        } else if (dir == 3) {
            x2 = x1 + length - 1;
            z2 = z1;
            z1 = z1 - width + 1;
        } else {
            x2 = x1 + length - 1;
            z2 = z1 + width - 1;
        }
        return new Floor("floor", x1, z1, x2, z2, y1);

    }

    //((stairs-at ?x ?y ?z ?width ?depth ?height ?dir))
    public MinecraftObject createStairs(JSPredicateForm term) {
        int x1, x2, x3, y1, y3, z1, z2, z3, length, width, height, dir;
        JSTerm tmp = (JSTerm) term.elementAt(1);
        x1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(2);
        y1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(3);
        z1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(4);
        width = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(5);
        length = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(6);
        height = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(7);
        dir = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        //east=1=>x+, west=2=>x-, north=3=>z-, south=4=>z+
        if (dir == 1) {
            x2 = x1 + width - 1;
            z2 = z1;
            x3 = x1;
            z3 = z1 + length - 1;
        } else if (dir == 2) {
            x2 = x1;
            x1 = x1 - width + 1;
            z2 = z1;
            x3 = x1;
            z3 = z1 - length + 1;
        } else if (dir == 3) {
            z2 = z1;
            z1 = z1 - width + 1;
            x2 = x1;
            z3 = z1;
            x3 = x1 + length - 1;
        } else {
            z2 = z1 + width - 1;
            x2 = x1;
            z3 = z1;
            x3 = x1 - length + 1;
        }
        y3 = y1 + height - 1;
        return new Stairs("staircase", x1, y1, z1, x2, z2, x3, y3, z3);
    }

    public MinecraftObject createRailing(JSPredicateForm term) {
        int x1, x2, y1, z1, z2, length, dir;
        JSTerm tmp = (JSTerm) term.elementAt(1);
        x1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(2);
        y1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(3);
        z1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(4);
        length = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(5);
        dir = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        //east=1=>x+, west=2=>x-, north=3=>z-, south=4=>z+
        if (dir == 1) {
            x2 = x1 + length - 1;
            z2 = z1;
        } else if (dir == 2) {
            x2 = x1;
            x1 = x1 - length + 1;
            z2 = z1;
        } else if (dir == 3) {
            x2 = x1;
            z2 = z1;
            z1 = z1 - length + 1;
        } else { // dir == 4
            x2 = x1;
            z2 = z1 + length - 1;
        }
        return new Railing("railing", x1, z1, x2, z2, y1);
    }

    public MinecraftObject createRow(JSPredicateForm term) {
        int x1, y1, z1, x2, z2, length, dir;
        JSTerm tmp = (JSTerm) term.elementAt(1);
        x1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(2);
        y1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(3);
        z1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(4);
        length = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(5);
        dir = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        //east=1=>x+, west=2=>x-, north=3=>z-, south=4=>z+
        if (dir == 1) {
            x2 = x1 + length - 1;
            z2 = z1;
        } else if (dir == 2) {
            x2 = x1;
            x1 = x1 - length + 1;
            z2 = z1;
        } else if (dir == 3) {
            x2 = x1;
            z2 = z1;
            z1 = z1 - length + 1;
        } else {
            x2 = x1;
            z2 = z1 + length - 1;
        }
        return new Row("row", x1, z1, x2, z2, y1);
    }

    public MinecraftObject createWall(JSPredicateForm term) {
        int x1, x2, y1, y2, z1, z2, length, height, dir;
        JSTerm tmp = (JSTerm) term.elementAt(1);
        x1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(2);
        y1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(3);
        z1 = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(4);
        length = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(5);
        height = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        tmp = (JSTerm) term.elementAt(6);
        dir = (int) Double.parseDouble(tmp.toStr().toString().replace("[", "").replace("]", ""));
        //east=1=>x+, west=2=>x-, north=3=>z-, south=4=>z+
        if (dir == 1) {
            x2 = x1 + length - 1;
            z2 = z1;
        } else if (dir == 2) {
            x2 = x1;
            x1 = x1 - length + 1;
            z2 = z1;
        } else if (dir == 3) {
            x2 = x1;
            z2 = z1;
            z1 = z1 - length + 1;
        } else {
            x2 = x1;
            z2 = z1 + length - 1;
        }
        y2 = y1 + height - 1;
        return new Wall("wall", x1, y1, z1, x2, y2, z2);
    }


    public MinecraftObject createCurrentMinecraftObject(JSTaskAtom groundedOperator) {
        //MinecraftObject result = null;
        int x1, y1, z1; // , x2, y2, z2, length, width, height, dir;
        String operator_name = groundedOperator.get(0).toString();

        return switch (operator_name) {
            case "!place-block":
                x1 = (int) Double.parseDouble(groundedOperator.get(2).toString().replace("[", "").replace("]", ""));
                y1 = (int) Double.parseDouble(groundedOperator.get(3).toString().replace("[", "").replace("]", ""));
                z1 = (int) Double.parseDouble(groundedOperator.get(4).toString().replace("[", "").replace("]", ""));
                yield new Block(x1, y1, z1);
            case "!build-row":
                yield createRow(groundedOperator);
            case "!build-row-starting":
                if (instructionLevel != InstructionLevel.BLOCK)
                    yield new IntroductionMessage(createRow(groundedOperator), true, "row");
            case "!build-row-finished":
                if (instructionLevel != InstructionLevel.BLOCK)
                    yield new IntroductionMessage(createRow(groundedOperator), false, "row");
            case "!build-wall-starting":
                if (instructionLevel != InstructionLevel.BLOCK)
                    yield new IntroductionMessage(createWall(groundedOperator), true, "wall");
            case "!build-wall-finished":
                if (instructionLevel != InstructionLevel.BLOCK)
                    yield new IntroductionMessage(createWall(groundedOperator), false, "wall");
            case "!build-wall":
                yield createWall(groundedOperator);
            case "!build-railing-starting":
                if (instructionLevel != InstructionLevel.BLOCK)
                    yield new IntroductionMessage(createRailing(groundedOperator), true, "railing");
            case "!build-railing-finished":
                if (instructionLevel != InstructionLevel.BLOCK)
                    yield new IntroductionMessage(createRailing(groundedOperator), false, "railing");
            case "!build-railing":
                yield createRailing(groundedOperator);
            case "!build-floor-starting":
                if (instructionLevel != InstructionLevel.BLOCK)
                    yield new IntroductionMessage(createFloor(groundedOperator), true, "floor");
            case "!build-floor-finished":
                if (instructionLevel != InstructionLevel.BLOCK)
                    yield new IntroductionMessage(createFloor(groundedOperator), false, "floor");
            case "!build-floor":
                yield createFloor(groundedOperator);
            case "!build-stairs-starting":
                if (instructionLevel != InstructionLevel.BLOCK)
                    yield new IntroductionMessage(createStairs(groundedOperator), true, "staircase");
            case "!build-stairs-finished":
                if (instructionLevel != InstructionLevel.BLOCK)
                    yield new IntroductionMessage(createStairs(groundedOperator), false, "staircase");
            case "!build-stairs":
                yield createStairs(groundedOperator);
            case "!place-block-hidden":
                System.out.println("Tried to get Minecraft Object for place-block-hidden something went wrong ");
                System.exit(-1);
                yield null;
            default:
                //log(task, "NewAction");
                System.out.println("New Action " + groundedOperator);
                System.exit(-1);
                yield null;
        };
    }

    Triple parseCoordinates(JSPredicateForm term) {
        JSTerm tmp;
        tmp = (JSTerm) term.elementAt(1);
        int x = (int) Double.parseDouble(tmp.toStr().toString());
        tmp = (JSTerm) term.elementAt(2);
        int y = (int) Double.parseDouble(tmp.toStr().toString());
        tmp = (JSTerm) term.elementAt(3);
        int z = (int) Double.parseDouble(tmp.toStr().toString());
        return new Triple(x, y, z);
    }

    Pair<Boolean, Triple> checkForIt(JSTState state, String itName) {
        List<JSPredicateForm> itList = state.state().atoms().stream().filter(term -> (term.elementAt(0)).equals(itName)).toList();
        if (itList.isEmpty()) {
            return new Pair<>(false, new Triple(100, 100, 100));
        } else {
            Triple coord = parseCoordinates(itList.get(0));
            if (coord.x == 100) {
                return new Pair<>(false, coord);
            } else {
                return new Pair<>(true, coord);
            }
        }
    }
}

class Triple {
    int x;
    int y;
    int z;

    Triple(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple triple = (Triple) o;
        return triple.x == x && triple.y == y && triple.z == z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
