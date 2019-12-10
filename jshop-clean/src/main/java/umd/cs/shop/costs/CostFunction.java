package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

public interface CostFunction {
    double approximate(JSTState state, JSOperator op, JSTaskAtom grounded_operator);

    double realCost(JSTState state, JSOperator op, JSTaskAtom grounded_operator);

    boolean isUnitCost();

    public static CostFunction getCostFunction(String costFunctionName, String domainName) {
        switch (costFunctionName) {
            case "unit":
                return new UnitCost();
            case "basic":
                return new BasicCost();
            case "stateDependent":
                if (domainName.equals("house")) {
                    return new StateDependentCostMinecraft();
                } else {
                    System.err.println("No state dependent cost function defined for " + domainName);
                    System.exit(-1);
                }
            default:
                System.err.println("Unknown cost function: " + costFunctionName);
                System.exit(-1);
        }
        return null;
    }

}
