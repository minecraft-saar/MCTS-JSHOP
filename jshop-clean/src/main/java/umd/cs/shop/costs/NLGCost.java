package umd.cs.shop.costs;

import de.saar.coli.minecraft.relationextractor.*;
import de.saar.coli.minecraft.MinecraftRealizer;
import umd.cs.shop.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NLGCost implements CostFunction {

    MinecraftRealizer nlgSystem;
    CostFunction.InstructionLevel instructionLevel;

    public NLGCost(CostFunction.InstructionLevel ins) {
        instructionLevel = ins;
        nlgSystem = MinecraftRealizer.createRealizer();
    }

    @Override
    public double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {

        MinecraftObject currentObject = createCurrentMinecraftObject(op, groundedOperator);
        Set<MinecraftObject> world = createWorldFromState(state);
        HashSet<MinecraftObject> it = new HashSet<>();
        double returnValue = nlgSystem.estimateCostForPlanningSystem(world, currentObject, it);

        return returnValue;
    }

    @Override
    public boolean isUnitCost() {
        return false;
    }

    private Set<MinecraftObject> createWorldFromState(JSTState state) {
        Set<MinecraftObject> world = new HashSet<>();
        for (JSPredicateForm term : state.state().atoms()) {
            String name = (String) term.elementAt(0);
            switch (name) {
                case "block-at":
                    JSTerm data = (JSTerm) term.elementAt(1);
                    String type = (String) data.elementAt(0);
                    if (type.equals("water")) {
                        // water behaves differently from normal blocks,
                        // e.g. you can still put blocks into water blocks
                        // this confuses the tracking and we therefore ignore water.
                        continue;
                    }
                    JSTerm tmp = (JSTerm) term.elementAt(2);
                    int x = (int) Double.parseDouble(tmp.toStr().toString());
                    tmp = (JSTerm) term.elementAt(3);
                    int y = (int) Double.parseDouble(tmp.toStr().toString());
                    tmp = (JSTerm) term.elementAt(4);
                    int z = (int) Double.parseDouble(tmp.toStr().toString());
                    //System.out.println("Block: " + type + " " + x + " " + y + " "+ z);
                    world.add(new UniqueBlock(type, x, y, z));
                    break;
                case "wall-at":
                    world.add(createWall(term));
                    break;
                case "row-at":
                    world.add(createRow(term));
                    break;
                case "railing-at":
                    world.add(createRailing(term));
                    break;
                case "floor-at":
                    world.add(createFloor(term));
                    break;
            }
        }
        return world;
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
