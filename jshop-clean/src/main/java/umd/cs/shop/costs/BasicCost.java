package umd.cs.shop.costs;

import umd.cs.shop.JSOperator;
import umd.cs.shop.JSPairOperatorState;
import umd.cs.shop.JSTState;

import java.util.HashMap;
import java.util.Random;

public class BasicCost implements CostFunction {

    /*HashMap<JSPairOperatorState, Double> approxCosts = new HashMap<JSPairOperatorState, Double>(1000);
    HashMap<JSPairOperatorState, Double> realCosts = new HashMap<JSPairOperatorState, Double>(1000);
    Random noiseGen = new Random(42);*/

    @Override
    public double approximate(JSTState state, JSOperator op) {

/*        JSPairOperatorState pair = new JSPairOperatorState(op, state.state());
        if(this.approxCosts.containsKey(pair)){
            return this.approxCosts.get(pair);
        }
        //if(this.realCosts.containsKey(pair)){
        //    res =  this.realCosts.get(pair);
        //}
        //nextInt can produce 0, but does not produce the bound
        int noise = this.noiseGen.nextInt(25);
        Double res = noise + 1.0;
        //so adding 1 to noise to prevent division by 0
        //double tmp = 1.0/(noise+1);
        //res = res + (res*tmp);

        approxCosts.put(pair,res);
*/
        return op.cost();
    }

    @Override
    public double realCost(JSTState state, JSOperator op) {
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

