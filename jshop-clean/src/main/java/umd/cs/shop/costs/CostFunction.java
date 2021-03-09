package umd.cs.shop.costs;

import de.saar.coli.minecraft.MinecraftRealizer;
import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

public interface CostFunction {

    double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx);

    boolean isUnitCost();

    enum CostFunctionType {
        UNIT,
        BASIC,
        STATEDEPENDENT,
        NLG
    }

    enum InstructionLevel{
        BLOCK,
        MEDIUM,
        HIGHLEVEL,
        NONE
    }

    public static CostFunction getCostFunction(CostFunctionType costFunctionName, String domainName) {
        switch (costFunctionName) {
            case UNIT:
                return new UnitCost();
            case BASIC:
                return new BasicCost();
            case STATEDEPENDENT:
                switch (domainName) {
                    case "house":
                        return new SDCostMinecraft();
                    case "blocksworld":
                        return new SDCostBlocksworld();
                    case "childsnack":
                        return new SDCostChildsnack();
                    default:
                        System.err.println("No state dependent cost function defined for " + domainName);
                        System.exit(-1);
                }
            default:
                System.err.println("Unknown cost function: " + costFunctionName);
                System.err.println("Options are: unit basic stateDependent");
                System.exit(-1);
        }
        return null;
    }

    public static CostFunction getCostFunction(CostFunctionType costFunctionName, String domainName, InstructionLevel level) {
        if(costFunctionName == CostFunctionType.NLG){
            return new NLGCost(level);
        }
        switch (level) {
            case BLOCK:
                return new SDMCBlock();
            case MEDIUM:
                return new SDMCMedium();
            case HIGHLEVEL:
                return new SDMCHighLevel();
            default:
                System.err.println("Unknown cost function: " + costFunctionName);
                System.err.println("Options are: unit basic stateDependent");
                System.exit(-1);
        }
        return null;
    }

}
