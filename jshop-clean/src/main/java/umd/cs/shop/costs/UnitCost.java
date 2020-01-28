package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

public class UnitCost implements CostFunction {

    @Override
    public double getCost(JSTState state, JSOperator op, JSTaskAtom grounded_operator, boolean approx) {
        return 1;
    }

    @Override
    public boolean isUnitCost() {
        return true;
    }
}
