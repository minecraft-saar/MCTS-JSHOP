package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

public class SDCostMinecraft implements CostFunction {

    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx){
        if(approx){
            return  approximate(state, op, groundedOperator);
        } else {
            return realCost(state, op, groundedOperator);
        }
    }

    public double approximate(JSTState state, JSOperator op, JSTaskAtom groundedOperator) {
        return realCost(state, op, groundedOperator);
    }

    public double realCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator) {
        assert groundedOperator.isGround();
        String operator_name = groundedOperator.get(0).toString();

        switch (operator_name) {
            case "!place-block":
                String block_type = groundedOperator.get(1).toString();
                int x = (int) Double.parseDouble(groundedOperator.get(2).toString().replace("[", "").replace("]", ""));
                int y = (int) Double.parseDouble(groundedOperator.get(3).toString().replace("[", "").replace("]", ""));
                int z = (int) Double.parseDouble(groundedOperator.get(4).toString().replace("[", "").replace("]", ""));

                int lx = (int) Double.parseDouble(groundedOperator.get(5).toString().replace("[", "").replace("]", ""));
                int ly = (int) Double.parseDouble(groundedOperator.get(6).toString().replace("[", "").replace("]", ""));
                int lz = (int) Double.parseDouble(groundedOperator.get(7).toString().replace("[", "").replace("]", ""));

                if (Math.pow(lx - x, 2) + Math.pow(ly - y, 2) + Math.pow(lz - z, 2) <= 1) {
                    return 0.5;
                } else {
                    return 2;
                }
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
            case "!use_block_type":
                return 10.0;
            case "!build-row":
                return 4;
            case "!build-column":
                return 4;
            case "!build-railing":
                return 10;
            case "!build-wall":
                return 20;
            case "!build-floor":
                return 20;
            case "!remove":
                return 5;
            default:
                System.err.println("Unrecognized action name: " + operator_name + " in CostMinecraft.");
                System.exit(-1);
                return 0;
        }

    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
