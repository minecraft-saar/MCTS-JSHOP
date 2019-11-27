package umd.cs.shop;

import java.util.Objects;

public class JSPairOperatorState {

    public JSOperator op;
    public JSState state;

    public JSPairOperatorState(JSOperator o, JSState s){
        this.op = o;
        this.state = s;
    }

    public boolean equals(Object o){
        if(! (o instanceof  JSPairOperatorState))
            return false;
        JSPairOperatorState pair = (JSPairOperatorState) o;
        if(! (this.op.equals(pair.op)))
            return false;

        return this.state.equals(pair.state);
    }

    public int hashCode(){
        return Objects.hash(this.op, this.state);
    }

}
