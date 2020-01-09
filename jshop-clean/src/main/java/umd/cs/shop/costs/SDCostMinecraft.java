package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

public class SDCostMinecraft implements CostFunction {


    @Override
    public double approximate(JSTState state, JSOperator op, JSTaskAtom grounded_operator) {
        return realCost(state, op, grounded_operator);
    }

    @Override
    public double realCost(JSTState state, JSOperator op, JSTaskAtom grounded_operator) {
        assert grounded_operator.isGround();
        String operator_name = grounded_operator.get(0).toString();

        if (operator_name.equals("!place-block")) {
            String block_type = grounded_operator.get(1).toString();
            int x = (int)Double.parseDouble(grounded_operator.get(2).toString().replace("[","").replace("]",""));
            int y = (int)Double.parseDouble(grounded_operator.get(3).toString().replace("[","").replace("]",""));
            int z = (int)Double.parseDouble(grounded_operator.get(4).toString().replace("[","").replace("]",""));

            int lx = (int)Double.parseDouble(grounded_operator.get(5).toString().replace("[","").replace("]",""));
            int ly = (int)Double.parseDouble(grounded_operator.get(6).toString().replace("[","").replace("]",""));
            int lz = (int)Double.parseDouble(grounded_operator.get(7).toString().replace("[","").replace("]",""));

            if (Math.pow(lx-x, 2) + Math.pow(ly-y, 2) + Math.pow(lz -z , 2) <= 1) {
                return 0.1;
            } else{
                return 2;
            }
        }

        System.err.println("Unrecognized place-block");
        System.exit(-1);
        return 0;
    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}
