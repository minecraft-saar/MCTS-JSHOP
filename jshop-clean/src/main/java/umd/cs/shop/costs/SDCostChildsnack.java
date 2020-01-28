package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

public class SDCostChildsnack implements CostFunction {

    @Override
    public double getCost(JSTState state, JSOperator op, JSTaskAtom grounded_operator, boolean approx) {
        return realCost(state, op, grounded_operator);
    }

    public double realCost(JSTState state, JSOperator op, JSTaskAtom grounded_operator) {
        String operator_name = grounded_operator.get(0).toString();
        return 0;
    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
