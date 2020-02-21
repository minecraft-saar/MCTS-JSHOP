package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

public class SDCostMinecraft implements CostFunction {

    @Override
    public double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx){
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
                return 0;
            case "!build-row":
                return 4;
            case "!build-column":
                return 4;
            case "!build-railing":
                return 10;
            case "!build-wall":
                return 20;
            case "!build-plane":
                return 20;
            default:
                System.err.println("Unrecognized action name: " + operator_name);
                System.exit(-1);
                return 0;
        }

    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
