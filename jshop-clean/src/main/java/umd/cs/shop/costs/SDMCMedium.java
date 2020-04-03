package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSPredicateForm;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

public class SDMCMedium implements CostFunction {


    @Override
    public double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {
        assert groundedOperator.isGround();
        String operator_name = groundedOperator.get(0).toString();
        boolean wall_built = false;
        for (JSPredicateForm pred : state.state().atoms()) {
            if (pred.elementAt(0).equals("wall-at")) {
                wall_built = true;
                break;
            }
        }
        if (wall_built) {
            switch (operator_name) {
                case "!place-block":
                    return 100000;
                case "!place-block-hidden":
                    return 0;
                case "!build-wall-finished":
                    return 0;
                case "!build-row":
                    return 10;
                case "!build-column":
                    return 10;
                case "!build-railing":
                    return 1;
                case "!build-wall":
                    return 1;
                case "!build-plane":
                    return 1;
                case "!remove":
                    return 5;
                default:
                    System.err.println("Unrecognized action name: " + operator_name);
                    System.exit(-1);
                    return 0;
            }
        } else {
            switch (operator_name) {
                case "!place-block":
                    return 1;
                case "!place-block-hidden":
                    return 0;
                case "!build-wall-finished":
                    return 0;
                case "!build-row":
                    return 1000;
                case "!build-column":
                    return 1000;
                case "!build-railing":
                    return 1000;
                case "!build-wall":
                    return 1000;
                case "!build-plane":
                    return 1000;
                case "!remove":
                    return 5;
                default:
                    System.err.println("Unrecognized action name: " + operator_name);
                    System.exit(-1);
                    return 0;
            }
        }

    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
