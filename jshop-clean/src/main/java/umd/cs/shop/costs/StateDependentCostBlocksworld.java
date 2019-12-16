package umd.cs.shop.costs;

import umd.cs.shop.*;

public class StateDependentCostBlocksworld implements CostFunction {

    private static int block_id (String block){
        try{

        } catch(Exception e) {

        }
        return 10;

    }
    @Override
    public double approximate(JSTState state, JSOperator op, JSTaskAtom grounded_operator) {
        return realCost(state, op, grounded_operator);
    }

    @Override
    public double realCost(JSTState state, JSOperator op, JSTaskAtom grounded_operator) {
        assert grounded_operator.isGround();
        String operator_name = grounded_operator.get(0).toString();

        for (JSPredicateForm term : state.state().atoms()) {
            term.print();
          //  System.out.println("X: " + term.elementAt(0));

        }
        switch (operator_name) {
            case "!pick-up":
                //String block = grounded_operator.get(1).toString();
                return 10;
            case "!put-down":
                return 10;
            case "!stack":
                String target = grounded_operator.get(2).toString();
                return block_id(target);
            case "!unstack":
                target = grounded_operator.get(2).toString();
                return block_id(target);
            case "!nop":
                return 0;
        }

        System.err.println("Unrecognized operator: " + grounded_operator);
        System.exit(-1);
        return 0;
    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
