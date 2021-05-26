package umd.cs.shop.costs;

import umd.cs.shop.*;

public class SDMCMedium implements CostFunction {


    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {
        assert groundedOperator.isGround();
        String operatorName = groundedOperator.get(0).toString();
        boolean wall_built = state.state().wallBuilt;
        boolean railing_built = state.state().railingBuilt;
        if (wall_built || railing_built) {
            switch (operatorName) {
                case "!place-block":
                    return 100.0;
                case "!place-block-hidden":
                case "!build-wall-finished":
                case "!build-wall-starting":
                case "!build-row-finished":
                case "!build-row-starting":
                case "!build-railing-finished":
                case "!build-railing-starting":
                case "!build-floor-finished":
                case "!build-floor-starting":
                    return 0.0;
                case "!build-row":
                    return 1.0;
                case "!build-column":
                    return 1.0;
                case "!build-railing":
                    return 1.0;
                case "!build-wall":
                    return 1.0;
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
        } else {
            switch (operatorName) {
                case "!place-block":
                    int x = (int) Double.parseDouble(groundedOperator.get(2).toString().replace("[", "").replace("]", ""));
                    int y = (int) Double.parseDouble(groundedOperator.get(3).toString().replace("[", "").replace("]", ""));
                    int z = (int) Double.parseDouble(groundedOperator.get(4).toString().replace("[", "").replace("]", ""));

                    int lx = (int) Double.parseDouble(groundedOperator.get(5).toString().replace("[", "").replace("]", ""));
                    int ly = (int) Double.parseDouble(groundedOperator.get(6).toString().replace("[", "").replace("]", ""));
                    int lz = (int) Double.parseDouble(groundedOperator.get(7).toString().replace("[", "").replace("]", ""));

                    if (Math.pow(lx - x, 2) + Math.pow(ly - y, 2) + Math.pow(lz - z, 2) <= 1) {
                        return 0.5;
                    } else {
                        return 2.0;
                    }
                case "!place-block-hidden":
                case "!build-wall-starting":
                case "!build-wall-finished":
                case "!build-railing-finished":
                case "!build-railing-starting":
                case "!build-floor-finished":
                case "!build-floor-starting":
                    return 0.0;
                case "!build-row":
                    return 1000.0;
                case "!build-column":
                    return 1000.0;
                case "!build-railing":
                    return 1000.0;
                case "!build-wall":
                    return 10000.0;
                case "!build-floor":
                    return 1000.0;
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

    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
