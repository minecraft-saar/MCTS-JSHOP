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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EstimationCost extends NLGCost {

    //MinecraftRealizer nlgSystem;

    public EstimationCost(CostFunction.InstructionLevel ins, String weightFile) {
        super(ins, weightFile);
    }

    @Override
    public Double getCost(JSTState state, JSOperator op, JSTaskAtom groundedOperator, boolean approx) {

        if(groundedOperator.get(0).equals("!place-block-hidden")||
                groundedOperator.get(0).equals("!remove-it-row") ||
                groundedOperator.get(0).equals("!remove-it-railing") ||
                groundedOperator.get(0).equals("!remove-it-stairs") ||
                groundedOperator.get(0).equals("!remove-it-wall")) {
            return 0.0;
        }
        MinecraftObject currentObject = createCurrentMinecraftObject(op, groundedOperator);
        Set<String> knownObjects = new HashSet<>();
        Pair<Set<MinecraftObject>,Set<MinecraftObject>> pair = createWorldFromState(state, knownObjects, currentObject);
        //Set<MinecraftObject> world = pair.getRight();
        //Set<MinecraftObject> it = pair.getLeft();
        if(currentObject instanceof IntroductionMessage intro){
            if(knownObjects.contains(intro.name)){
                //make dead end
                return 30000.0;
            } else {
                return 0.000001;
            }
        }
        // call NN python script here
        //calling nlgsysstem for model:
        //String model = nlgSystem.getModelforNN(world, currentObject, it);
        //new way
        String model = this.model;
        // add missing \" to string
//        System.out.println(model);
        Pattern pattern = Pattern.compile("(\"[a-zA-Z_\\-\\d]+)(\")");  // ([a-zA-Z_\-\d]+)
        Matcher matcher = pattern.matcher(model);
        model = matcher.replaceAll("\\\\$1\\\\$2");
        model = "[" + model + "]";
//        System.out.println(model);

        // I guess ProcessBuilder init input can be understood as the line you would put into the command line
        ProcessBuilder pb = new ProcessBuilder("python", "../../cost-estimation/nn/main.py", "-c True", "-d " + model);
        pb.directory(new File("../../cost-estimation/nn"));
        pb.redirectErrorStream(true);
        Process process = null;
        double returnValue = Double.POSITIVE_INFINITY;
        try {
            process = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String ret;
            while ((ret = in.readLine()) != null) {
                returnValue = Double.parseDouble(ret);
            }
            int exitCode = process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return returnValue;
    }

}