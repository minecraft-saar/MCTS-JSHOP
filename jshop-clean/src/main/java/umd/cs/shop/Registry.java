package umd.cs.shop;

import java.util.Hashtable;
import java.util.LinkedList;

public class Registry {
    public Hashtable<Integer, JSState> stateRegistry;
    public Hashtable<Integer, JSTasks> taskNetworkRegistry;
    public LinkedList<MCTSNode> allNodes = new LinkedList<>();
    int numStates;

    public Registry() {
        this.stateRegistry = new Hashtable<>();
        this.taskNetworkRegistry = new Hashtable<>();

        this.numStates = 0;
    }


    public boolean addToStateRegistry(JSState state) {
        Integer key = state.hashCode();
        if (stateRegistry.containsKey(key)) {
            JSState mapResult = stateRegistry.get(key);
            if (mapResult.equals(state)) {
                return false;
            } else {
                JSUtil.println("Different States with same hash Code");
                JSUtil.println("State in Registry: ");
                mapResult.print();
                JSUtil.println(" \n New State: ");
                state.print();
                JSUtil.println("\n");
                //System.exit(0);
            }
        }
        this.numStates++;
        stateRegistry.put(key, state);
        return true;

        //return false;
    }

    public boolean addToTaskNetworkRegistry(JSTasks tasks) {
        Integer key = tasks.hashCode();
        if (taskNetworkRegistry.containsKey(key)) {
            JSTasks mapResult = taskNetworkRegistry.get(key);
            if (mapResult.equals(tasks)) {
                return false;
            } else {
                System.out.println("Different TaskNetworks with same hash Code");
                //System.exit(0);
            }
        } //else {
        taskNetworkRegistry.put(key, tasks);
        return true;
        //}
        //return false;
    }

    public boolean checkStateTaskNetwork(JSState state, JSTasks tasks) {
        //JSUtil.println("Checking states and tasknetwork");
        if (stateRegistry.containsKey(state.hashCode())) {
            JSUtil.println("State equal");
            if (taskNetworkRegistry.containsKey(tasks.hashCode())) {
                JSUtil.println("Task Network equal");
                for (MCTSNode child : allNodes) {
                    if (child.tState().state.equals(state)) {
                        if (child.taskNetwork().equals(tasks)) {
                            JSUtil.println("Combination occurred before");
                            return true;
                        }
                    }
                }
                JSUtil.println("Combination did not occur before");
                //JSUtil.println("Size of allnodes: " + allNodes.size());
                //allNodes.add(node);
                return false;
            }
        }
        //allNodes.add(node);
        return false;
    }


}
