package umd.cs.shop.costs;

public class CostFunctionFactory {
    public static CostFunction get_cost_function(String name) {
        if (name.equals("unit")) {
            return new UnitCost();
        } else if (name.equals("basic")) {
            return new BasicCost();
        } else if (name.equals("stateDependent")) {
            return new StateDependentCostMinecraft();
        } else {
            System.err.println("Unknown cost function: " + name);
            System.exit(-1);
        }

        return null;
    }

}
