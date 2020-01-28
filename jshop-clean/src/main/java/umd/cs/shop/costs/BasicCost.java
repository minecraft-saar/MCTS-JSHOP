package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSPairOperatorState;
import umd.cs.shop.JSTState;
import umd.cs.shop.JSTaskAtom;

import java.util.HashMap;
import java.util.Random;

public class BasicCost implements CostFunction {

    /*HashMap<JSPairOperatorState, Double> approxCosts = new HashMap<JSPairOperatorState, Double>(1000);
    HashMap<JSPairOperatorState, Double> realCosts = new HashMap<JSPairOperatorState, Double>(1000);
    Random noiseGen = new Random(42);*/


    @Override
    public double getCost(JSTState state, JSOperator op, JSTaskAtom grounded_operator, boolean approx) {
/*
        JSPairOperatorState pair = new JSPairOperatorState(op, state.state());
        if(realCosts.containsKey(pair)){
            double res = realCosts.get(pair);
            //JSUtil.println("cost Function hashMap = " + res);
            return res;
        }
        int add = op.addList().size();
        int delete = op.deleteList().size();
        Double res = op.cost();
        realCosts.put(pair,res);*/
        //JSUtil.println("cost Function  = " + res);
        return op.cost();
    }

    @Override
    public boolean isUnitCost() {
        return false;
    }
}

