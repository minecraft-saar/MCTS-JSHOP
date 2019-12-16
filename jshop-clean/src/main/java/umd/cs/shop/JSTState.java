package umd.cs.shop;

public class JSTState {
    JSState state;
    JSListLogicalAtoms addList;
    JSListLogicalAtoms deleteList;

    JSTState() {
        super();
    }

    JSTState(JSState st, JSListLogicalAtoms al, JSListLogicalAtoms dl) {
        super();
        state = st;
        addList = al;
        deleteList = dl;
    }

    JSTState(JSTState st) {
        super();
        state = new JSState(st.state.atoms());
        addList = (JSListLogicalAtoms) st.addList.clone();
        deleteList = (JSListLogicalAtoms) st.deleteList.clone();
    }

    public JSState state() {
        return state;
    }

    public JSListLogicalAtoms addList() {
        return addList;
    }

    public JSListLogicalAtoms deleteList() {
        return deleteList;
    }


    public void print() {
        JSState st = this.state();
        JSListLogicalAtoms al = this.addList();
        JSListLogicalAtoms dl = this.deleteList();
        JSUtil.print("[state:");
        st.print();
        JSUtil.println("+:");
        al.print();
        JSUtil.println("-:");
        dl.print();
        JSUtil.print("]");

    }

}
