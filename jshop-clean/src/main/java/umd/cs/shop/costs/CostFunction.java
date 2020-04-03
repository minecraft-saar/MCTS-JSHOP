package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

public interface CostFunction {

    double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx);

    boolean isUnitCost();

    enum CostFunctionType {
        UNIT,
        BASIC,
        STATEDEPENDENT
    }

    enum InstructionLevel{
        BLOCK,
        MEDIUM,
        HIGHLEVEL
    }

    public static CostFunction getCostFunction(CostFunctionType costFunctionName, String domainName) {
        switch (costFunctionName) {
            case UNIT:
                return new UnitCost();
            case BASIC:
                return new BasicCost();
            case STATEDEPENDENT:
                if (domainName.equals("house")) {
                    return new SDCostMinecraft();
                }else if (domainName.equals("blocksworld")) {
                    return new SDCostBlocksworld();
                }else if (domainName.equals("childsnack")) {
                    return new SDCostChildsnack();
                } else {
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
