package umd.cs.shop;

import java.util.HashMap;
import java.util.Hashtable;

public class Registry {
    public Hashtable<Integer, JSState> StateRegistry;
    public Hashtable<Integer, JSTasks> TaskNetworkRegistry;
    int numStates;

    public Registry() {
        this.StateRegistry = new Hashtable<>();
        this.TaskNetworkRegistry = new Hashtable<>();
        this.numStates = 0;
    }


    public boolean addToStateRegistry(JSState state) {
        Integer key = state.hashCode();
        if (StateRegistry.containsKey(key)) {
            JSState mapResult = StateRegistry.get(key);
            if (mapResult.equals(state)) {
                return false;
            } else {
                System.out.println("Different States with same hash Code");
                //System.exit(0);
            }
        }
        this.numStates++;
        StateRegistry.put(key, state);
        return true;

        //return false;
    }

    public boolean addToTaskNetworkRegistry(JSTasks tasks) {
        Integer key = tasks.hashCode();
        if (TaskNetworkRegistry.containsKey(key)) {
            JSTasks mapResult = TaskNetworkRegistry.get(key);
            if (mapResult.equals(tasks)) {
                return false;
            } else {
                System.out.println("Different TaskNetworks with same hash Code");
                //System.exit(0);
            }
        } //else {
            TaskNetworkRegistry.put(key, tasks);
            return true;
        //}
        //return false;
    }

    public boolean checkStateTaskNetwork(JSState state, JSTasks tasks) {
        //JSUtil.println("Checking states and tasknetwork");
        if (StateRegistry.containsKey(state.hashCode())) {
            return TaskNetworkRegistry.containsKey(tasks.hashCode());
        }
        return false;
    }


}
