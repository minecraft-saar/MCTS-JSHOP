package umd.cs.shop.costs;

import de.saar.basic.Pair;
import de.saar.coli.minecraft.MinecraftRealizer;
import de.saar.coli.minecraft.relationextractor.IntroductionMessage;
import de.saar.coli.minecraft.relationextractor.MinecraftObject;
import umd.cs.shop.*;

import javax.swing.*;
import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EstimationCost extends NLGCost {

    MinecraftRealizer nlgSystem;

    public EstimationCost(CostFunction.InstructionLevel ins, String weightFile) {
        super(ins, weightFile);
        nlgSystem = MinecraftRealizer.createRealizer();
    }

    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {

        if(groundedOperator.get(0).equals("!place-block-hidden")){
            return 0.0;
        }
        MinecraftObject currentObject = createCurrentMinecraftObject(op, groundedOperator);
        Set<String> knownObjects = new HashSet<>();
        Pair<Set<MinecraftObject>,Set<MinecraftObject>> pair = createWorldFromState(state, knownObjects);
        Set<MinecraftObject> world = pair.getRight();
        Set<MinecraftObject> it = pair.getLeft();
        if(currentObject instanceof IntroductionMessage){
            IntroductionMessage intro = (IntroductionMessage) currentObject;
            if(knownObjects.contains(intro.name)){
                //make dead end
                return 30000.0;
            } else {
                return 0.000001;
            }
        }
        String currentObjectType = currentObject.getClass().getSimpleName().toLowerCase();
        boolean objectFirstOccurence = ! knownObjects.contains(currentObjectType);
        if (objectFirstOccurence && weights != null) {
            // temporarily set the weight to the first occurence one
            // ... if we have an estimate for the first occurence
            if (weights.firstOccurenceWeights.containsKey("i" + currentObjectType)) {
                nlgSystem.setExpectedDurations(
                        Map.of("i" + currentObjectType, weights.firstOccurenceWeights.get("i" + currentObjectType)),
                        false);
            }
        }
        // TODO call my NN system here
//        double returnValue = nlgSystem.estimateCostForPlanningSystem(world, currentObject, it);
        String model = nlgSystem.getModelforNN(world, currentObject, it);
        ProcessBuilder pb = new ProcessBuilder("python", "../../cost-estimation/nn/main.py");
        pb.redirectErrorStream(true);
        Process process = null;
        try {
            process = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        double returnValue = 0;
        try {
            returnValue = Double.valueOf(in.readLine()).doubleValue();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            int exitCode = process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        System.out.println(model);
        return returnValue;
    }

}