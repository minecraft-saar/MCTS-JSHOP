package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;

public class StateDependentCostMinecraft implements CostFunction {


    @Override
    public double approximate(JSTState state, JSOperator op) {
        return realCost(state, op);
    }

    @Override
    public double realCost(JSTState state, JSOperator op) {
        System.out.println(op.head());
        return 1;
    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
