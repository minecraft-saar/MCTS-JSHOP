package umd.cs.shop.costs;

import umd.cs.shop.*;

import static java.lang.Math.max;

public class SDCostBlocksworld implements CostFunction {

    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom grounded_operator, boolean approx) {
        return realCost(state, op, grounded_operator);
    }

    public int height (JSTState state, String target) {
        int height = 0;
        while (!target.equals("ontable")) {
            //System.out.println(target);
            height++;
            boolean found = false;
            for (JSPredicateForm pred : state.state().atoms()) {
               // System.out.println(pred.toString());
                if (pred.get(0).equals("ontable") && pred.get(1).toString().equals(target)) {
                    found = true;
                    target = "ontable";
                    break;
                } else if (pred.get(0).equals("on") && pred.get(1).toString().equals(target)) {
                    found = true;
                    target = pred.get(2).toString();
                    break;
                }
            }

            if (!found) {
                System.exit(0);
            }
        }
        return height;

    }

    public double realCost(JSTState state, JSOperator op, JSTaskAtom grounded_operator) {
        assert grounded_operator.isGround();
        String operator_name = grounded_operator.get(0).toString();

        switch (operator_name) {
            case "!put-down": case "!pick-up":
                return 10;

            case "!unstack":
             return max(0, 10 - height(state, grounded_operator.get(1).toString()));

            case "!stack":
                return max(0, 10 - height(state, grounded_operator.get(2).toString()));

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
