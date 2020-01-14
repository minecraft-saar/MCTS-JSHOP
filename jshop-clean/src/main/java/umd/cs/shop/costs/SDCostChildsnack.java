package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

public class SDCostChildsnack implements CostFunction {

    @Override
    public double approximate(JSTState state, JSOperator op, JSTaskAtom grounded_operator) {
        return realCost(state, op, grounded_operator);
    }

    @Override
    public double realCost(JSTState state, JSOperator op, JSTaskAtom grounded_operator) {
        String operator_name = grounded_operator.get(0).toString();
        return 0;
    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
