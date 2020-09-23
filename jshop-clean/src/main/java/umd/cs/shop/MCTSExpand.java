package umd.cs.shop;

import java.util.Vector;

public interface MCTSExpand {

    public Vector<MCTSNode> expand(MCTSNode node);

    public static MCTSExpand getPolicy(String policy, boolean recursive, JSJshopVars vars) {
        switch (policy) {
            case "simple":
                return new MCTSExpansionSimple(recursive, false, vars);
            case "primitive":
                return new MCTSExpansionPrimitive(recursive, vars);
            case "deadEnd":
                return new MCTSExpansionSimple(recursive, true, vars);
            case "landmark":
                return new MCTSExpansionLandmark(recursive, false, vars);
            default:
                System.err.println("Unknown expansion policy name: " + policy);
                System.err.println("Options are: simple primitive deadEnd");
                System.exit(-1);
        }
        return null;
    }
}
