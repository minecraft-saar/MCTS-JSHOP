package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;

public interface CostFunction {
    double approximate(JSTState state, JSOperator op);


    double realCost(JSTState state, JSOperator op);

    boolean isUnitCost();
}
