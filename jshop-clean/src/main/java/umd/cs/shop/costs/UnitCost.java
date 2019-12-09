package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;

public class UnitCost implements CostFunction {

    @Override
    public double approximate(JSTState state, JSOperator op) {
        return 1;
    }

    @Override
    public double realCost(JSTState state, JSOperator op) {
        return 1;
    }

    @Override
    public boolean isUnitCost() {
        return true;
    }
}
