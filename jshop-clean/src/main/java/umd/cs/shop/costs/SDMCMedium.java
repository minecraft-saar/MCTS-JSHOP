package umd.cs.shop.costs;

import umd.cs.shop.*;

public class SDMCMedium implements CostFunction {


    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {
        assert groundedOperator.isGround();
        String operatorName = groundedOperator.get(0).toString();
        boolean wall_built = state.state().wallBuilt;
        boolean railing_built = state.state().railingBuilt;
        boolean stairs_built = state.state().stairsBuilt;
        switch (operatorName) {
            case "!place-block":
                return 100.0;
            case "!build-row":
                return 1.0;
            case "!place-block-hidden":
            case "!build-row-finished":
            case "!build-row-starting":
            case "!build-railing-finished":
            case "!build-railing-starting":
            case "!build-floor-finished":
            case "!build-floor-starting":
            case "!build-stairs-starting":
            case "!build-stairs-finished":
            case "!build-wall-finished":
            case "!build-wall-starting":
            case "!remove-it-stairs":
            case "!remove-it-wall":
            case "!remove-it-row":
            case "!remove-it-railing":
                return 0.0;
            case "!build-column":
                return 1.0;
            case "!build-railing":
                if (railing_built) {
                    return 1.0;
                } else {
                    return 10000.0;
                }
            case "!build-wall":
                if (wall_built) {
                    return 1.0;
                } else {
                    return 10000.0;
                }
            case "!build-stairs":
                if (stairs_built) {
                    return 1.0;
                } else {
                    return 10000.0;
                }
            case "!build-floor":
                return 1.0;
            case "!remove-block":
                return 5.0;
            case "!place-door":
                return 2.0;
            default:
                System.err.println("Unrecognized action name: " + operatorName);
                System.exit(-1);
                return 0.0;
        }
    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
