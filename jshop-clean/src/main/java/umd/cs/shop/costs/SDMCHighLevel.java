package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;
import umd.cs.shop.JSUtil;

public class SDMCHighLevel implements CostFunction{

    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {
        assert groundedOperator.isGround();
        String operatorName = groundedOperator.get(0).toString();

        switch (operatorName) {
            case "!place-block-hidden":
            case "!build-wall-starting":
            case "!build-wall-finished":
            case "!build-row-starting":
            case "!build-row-finished":
            case "!build-railing-finished":
            case "!build-railing-starting":
            case "!build-floor-finished":
            case "!build-floor-starting":
                return 0.0;
            case "!use_block_type":
                return 20.0;
            case "!place-block":
               return 100000.0;
            case "!build-row":
                return 10.0;
            case "!build-column":
                return 10.0;
            case "!build-railing":
                return 1.0;
            case "!build-wall":
                return 1.0;
            case "!build-floor":
                return 1.0;
            case "!remove":
                return 5.0;
            default:
                System.err.println("Unrecognized action name: " + operatorName + " in cost function HighLevel.");
                System.exit(-1);
                return 0.0;
        }

    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
