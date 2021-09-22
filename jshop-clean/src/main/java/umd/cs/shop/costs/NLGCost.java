package umd.cs.shop.costs;

import de.saar.basic.Pair;
import de.saar.coli.minecraft.relationextractor.*;
import de.saar.coli.minecraft.MinecraftRealizer;
import de.saar.minecraft.analysis.WeightEstimator;
import umd.cs.shop.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class NLGCost implements CostFunction {

    MinecraftRealizer nlgSystem;
    CostFunction.InstructionLevel instructionLevel;
    protected WeightEstimator.WeightResult weights;
    boolean weightsPresent;
    Double lowestCost;

    public NLGCost(CostFunction.InstructionLevel ins, String weightFile) {
        instructionLevel = ins;
        nlgSystem = MinecraftRealizer.createRealizer();
        lowestCost = 10.0;
        if(weightFile.equals("")){
            weightsPresent = false;
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


    }

    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {
        if(groundedOperator.get(0).equals("!place-block-hidden")){
            return 0.0;
        }
        MinecraftObject currentObject = createCurrentMinecraftObject(op, groundedOperator);
        Set<String> knownObjects = new HashSet<>();
        Pair<Set<MinecraftObject>,Set<MinecraftObject>> pair = createWorldFromState(state, knownObjects);
        Set<MinecraftObject> world = pair.getRight();
        Set<MinecraftObject> it = pair.getLeft();
        if(currentObject instanceof IntroductionMessage ){
            IntroductionMessage intro = (IntroductionMessage) currentObject;
            //JSUtil.println(intro.toString());
            //JSUtil.println(world.toString());
            if(knownObjects.contains(intro.name)){
                //make dead end
                return 30000.0;
            } else {
                return 0.000001;
            }
        }
        //JSUtil.println(groundedOperator.toString());
        //JSUtil.println(currentObject.toString() + " it: " + it.toString());
        String currentObjectType = currentObject.getClass().getSimpleName().toLowerCase();
        boolean objectFirstOccurence = ! knownObjects.contains(currentObjectType);
        if (objectFirstOccurence && weights != null) {
            // temporarily set the weight to the first occurence one
            // ... if we have an estimate for the first occurence
            if (weights.firstOccurenceWeights.containsKey("i" + currentObjectType)) {
                nlgSystem.setExpectedDurations(
                        Map.of("i" + currentObjectType, weights.firstOccurenceWeights.get("i" + currentObjectType)),
                        false);
            }
        }
        double returnValue = nlgSystem.estimateCostForPlanningSystem(world, currentObject, it);
        //JSUtil.println(returnValue + " ");
        //JSUtil.println(world.toString());
        if (returnValue < 0.0){
            if(returnValue < lowestCost){
                lowestCost = returnValue;
                JSUtil.println(lowestCost.toString());
                //returnValue = 0.1;
            }
            //returnValue = 0.0;
        }
        //returnValue = returnValue + 21864;
        //if(returnValue < 0.0){
        //    JSUtil.println("returnValue is smaller 0: " + returnValue);
        //    System.exit(2);
        //}
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

    private Pair<Set<MinecraftObject>,Set<MinecraftObject>> createWorldFromState(JSTState state, Set<String> knownObjects) {
        Set<MinecraftObject> world = new HashSet<>();
        HashSet<MinecraftObject> it = new HashSet<MinecraftObject>();
        for (JSPredicateForm term : state.state().atoms()) {
            String name = (String) term.elementAt(0);
            MinecraftObject mco;
            JSTerm data;
            String type;
            JSTerm tmp;
            int x, y, z;
            switch (name) {
                case "block-at":
                    data = (JSTerm) term.elementAt(1);
                    type = (String) data.elementAt(0);
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
                    if(type.equals("stone")){
                        mco = new Block(x, y, z);
                        world.add(mco);
                        knownObjects.add(mco.getClass().getSimpleName().toLowerCase());
                    } else {
                        world.add(new UniqueBlock(type, x, y, z));
                    }
                    break;
                case "last-placed":
                    data = (JSTerm) term.elementAt(1);
                    tmp = (JSTerm) term.elementAt(1);
                    x = (int) Double.parseDouble(tmp.toStr().toString());
                    tmp = (JSTerm) term.elementAt(2);
                    y = (int) Double.parseDouble(tmp.toStr().toString());
                    tmp = (JSTerm) term.elementAt(3);
                    z = (int) Double.parseDouble(tmp.toStr().toString());
                    //System.out.println("Block: " + type + " " + x + " " + y + " "+ z);
                    if(!(x == 100 && y == 100 && z == 100)){
                        it.add(new Block( x, y, z));
                    }
                    break;
                case "wall-at":
                     mco = createWall(term);
                    world.add(mco);
                    knownObjects.add(mco.getClass().getSimpleName().toLowerCase());
                    break;
                case "row-at":
                    mco = createRow(term);
                    world.add(mco);
                    knownObjects.add(mco.getClass().getSimpleName().toLowerCase());
                    break;
                case "railing-at":
                    mco = createRailing(term);
                    world.add(mco);
                    it.add(mco);
                    knownObjects.add(mco.getClass().getSimpleName().toLowerCase());
                    break;
                case "floor-at":
                    mco = createFloor(term);
                    world.add(mco);
                    it.add(mco);
                    knownObjects.add(mco.getClass().getSimpleName().toLowerCase());
                    break;
            }
        }
        Pair<Set<MinecraftObject>, Set<MinecraftObject>> ret = new Pair<Set<MinecraftObject>, Set<MinecraftObject>>(it, world);
        return ret;
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

    private MinecraftObject createCurrentMinecraftObject(JSOperator op, JSTaskAtom groundedOperator) {
        MinecraftObject result = null;
        int x1, y1, z1; // , x2, y2, z2, length, width, height, dir;
        String operator_name = groundedOperator.get(0).toString();
        switch (operator_name) {
            case "!place-block":
                x1 = (int) Double.parseDouble(groundedOperator.get(2).toString().replace("[", "").replace("]", ""));
                y1 = (int) Double.parseDouble(groundedOperator.get(3).toString().replace("[", "").replace("]", ""));
                z1 = (int) Double.parseDouble(groundedOperator.get(4).toString().replace("[", "").replace("]", ""));
                result = new Block(x1, y1, z1);
                break;
            case "!build-row":
                result = createRow(groundedOperator);
                break;
            case "!build-row-starting":
                if (instructionLevel != CostFunction.InstructionLevel.BLOCK)
                    result = new IntroductionMessage(createRow(groundedOperator), true, "row");
                break;
            case "!build-row-finished":
                if (instructionLevel != CostFunction.InstructionLevel.BLOCK)
                    result = new IntroductionMessage(createRow(groundedOperator), false, "row");
                break;
            case "!build-wall-starting":
                if (instructionLevel != CostFunction.InstructionLevel.BLOCK)
                    result = new IntroductionMessage(createWall(groundedOperator), true, "wall");
                break;
            case "!build-wall-finished":
                if (instructionLevel != CostFunction.InstructionLevel.BLOCK)
                    result = new IntroductionMessage(createWall(groundedOperator), false, "wall");
                break;
            case "!build-wall":
                result = createWall(groundedOperator);
                break;
            case "!build-railing-starting":
                if (instructionLevel != CostFunction.InstructionLevel.BLOCK)
                    result = new IntroductionMessage(createRailing(groundedOperator), true, "railing");
                break;
            case "!build-railing-finished":
                if (instructionLevel != CostFunction.InstructionLevel.BLOCK)
                    result = new IntroductionMessage(createRailing(groundedOperator), false, "railing");
                break;
            case "!build-railing":
                result = createRailing(groundedOperator);
                break;
            case "!build-floor-starting":
                if (instructionLevel != CostFunction.InstructionLevel.BLOCK)
                    result = new IntroductionMessage(createFloor(groundedOperator), true, "floor");
                break;
            case "!build-floor-finished":
                if (instructionLevel != CostFunction.InstructionLevel.BLOCK)
                    result = new IntroductionMessage(createFloor(groundedOperator), false, "floor");
                break;
            case "!build-floor":
                result = createFloor(groundedOperator);
                break;
            case "!place-block-hidden":
                System.out.println("Tried to get Minecraft Object for place-block-hidden something went wrong ");
                result = null;
                break;
            default:
                //log(task, "NewAction");
                System.out.println("New Action " + groundedOperator);
                result = null;
                break;
        }

        return result;
    }

}
