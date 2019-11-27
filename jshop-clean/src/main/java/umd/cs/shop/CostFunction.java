package umd.cs.shop;

public interface CostFunction {
    double approximate(JSTState state, JSOperator op);


    double realCost(JSTState state, JSOperator op);
}
