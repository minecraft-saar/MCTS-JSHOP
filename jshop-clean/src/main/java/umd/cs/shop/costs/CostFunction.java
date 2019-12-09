package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

public interface CostFunction {
    double approximate(JSTState state, JSOperator op, JSTaskAtom grounded_operator);


    double realCost(JSTState state, JSOperator op, JSTaskAtom grounded_operator);

    boolean isUnitCost();
}
