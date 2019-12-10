package umd.cs.shop.costs;

public class CostFunctionFactory {
    public static CostFunction get_cost_function(String costFunctionName, String domainName) {
        if (costFunctionName.equals("unit")) {
            return new UnitCost();
        } else if (costFunctionName.equals("basic")) {
            return new BasicCost();
        } else if (costFunctionName.equals("stateDependent")) {
            if (domainName.equals("house")) {
                return new StateDependentCostMinecraft();
            } else {
                System.err.println("No state dependent cost function defined for " + domainName);
                System.exit(-1);
            }
        } else {
            System.err.println("Unknown cost function: " + costFunctionName);
            System.exit(-1);
        }

        return null;
    }

}
