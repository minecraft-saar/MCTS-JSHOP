package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;
import umd.cs.shop.JSUtil;

public class SDMCHighLevel implements CostFunction{

    @Override
    public double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {
        assert groundedOperator.isGround();
        String operatorName = groundedOperator.get(0).toString();

        switch (operatorName) {
            case "!place-block-hidden":
            case "!build-wall-starting":
            case "!build-wall-finished":
            case "!build-railing-finished":
            case "!build-railing-starting":
            case "!build-floor-finished":
            case "!build-floor-starting":
                return 0;
            case "!place-block":
               return 100000;
            case "!build-row":
                return 10;
            case "!build-column":
                return 10;
            case "!build-railing":
                return 1;
            case "!build-wall":
                return 1;
            case "!build-floor":
                return 1;
            case "!remove":
                return 5;
            default:
                System.err.println("Unrecognized action name: " + operatorName);
                System.exit(-1);
                return 0;
        }

    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
