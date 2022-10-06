# MCTS-JSHOP Usage:

The MCTS-JSHOP planner has to search modi right now, a blind depth-first search and Monte Carlo Tree Search.

1. Blind depth first search

java -jar PathToJAR DOMAINFILE PROBLEMFILE -s [--all] [-d INT1-10]

Without "--all" the planner will stop after the first solution found, with it the planner will search for all solutions. 
With the option "-d" the output can be made more verbose with 1 being the lowest amount of additional output, and it increasing with each number up to a max of 10

2. Monte Carlo Tree Search 

java -jar PathToJAR DOMAINFILE PROBLEMFILE -m MCTSRUNS -c COSTFUNCTION -e EXPANSIONPOLICY [-b] [--recursiveSimulation NUMBER]

* MCTSRUNS can be any positive integer. 

* "-b": enables branch and bound pruning (i.e. prune against best solution found so far)

* "--recursiveSimulation NUMBER": makes "NUMBER" many "depth-first-search" like runs that use backtracking

* EXPANSIONPOLICY can be:  
  + "simple" : Standard MCTS behavior  
  + "deadEnd" : Deletes DeadEnd Nodes to prevent repeat visits of same deadend. The best performing setting.  
  + "primitive" : Collapses MCTS Search Nodes unless the task is primitive. This is an old experimental setting.  
  + "landmark" : Same MCTS config as "simple" but use landmarks as search guidance. To work options "-l" and "-lf LANDMRAKFILE" are also required.   

* COSTFUNCTION can be:  
  + "UNIT": All actions have unit cost  
  + "BASIC": Actions have cost as specified in the domain file.  
  + "STATEDEPENDENT": Uses hand crafted costfunctions for Minecraft, Blocksworld and Rovers domains does not work for other domains.  
  + "NLG": Only usable with Minecraft domain, calls the NLG System for cost estimates.  
  + "NN": Calls a pre-trained NN for cost estimates.
  
  If the option "NLG" or "STATEDEPENDENT" is chosen, the abstraction level for Minecraft instructions can be chosen by adding option "--level ABSTRACTION". ABSTRACTION can be "BLOCK", "MEDIUM" or "HIGHLEVEL".  

Additional options for MCTS, their default is already set according to best performance:  
* "-p FILENAME" or "--planFile FILENAME" Best plan will be printed into file with name FILENAME, will overwrite anything already in File and create new File if none with that name is present
* "--allPlans FILENAME" All found plans will be printed into file with name FILENAME, will overwrite anything already in File and create new File if none with that name is present
* "--noRec": Disables the collapsing of MCTS Search Nodes. Really bad for performance  
* "--noRandom": The first MCTS run will not include any randomness and follow the method ordering given in the domain  
* "--printTree": Prints a .dot style output of the MCTS search tree  
* "--min": Uses the minimum cost when updating the MCTS Nodes instead of the UCT-formula  
* "--fastSimulation": The states generated in simulations are not saved  
* "-bf": Use branch and bound pruning and fastSimulations  
* "-exp REALNUMBER": Set the exploration factor of MCTS to given real number  
* "--randomSeed NUMBER": Set the seed of the random generator to NUMBER  

Currently best config to start planning using the integrated NLG system:
java -jar $PATH_TO_JAR $PATH_TO_DOMAIN $PATH_TO_PROBLEM -m 1000000 -t 1 -exp 10 -c NLG --level MEDIUM -e deadEnd -wf $PATH_TO_WEIGHT_FILE

# NN Usage

Do not forget to use the NN argument for COSTFUNCTION if you wish to use this (``-c NN``)!

Additional options specifically for NN:
* "-nnp NNPATH" or "--nnPath NNPATH": Path to the pre-trained NN; The default value uses a pretrained model located at src/main/java/umd/cs/shop/costs/models/trained_model.zip.
* "-scp SCALERPATH" or "--scalerPath SCALERPATH": Path to a json file containing parameters for the scaler. Should contain the same values that were used for training the used model. The default file is located at src/main/java/umd/cs/shop/costs/models/scaler.json.
* "-cmp" or "--compare": Whether to run the NLG system next to the NN for cost estimation in order to compare the costs of the two. The results will be printed into  the file ``cost_comparison.txt`` inside of the ``jshop-clean`` folder. Default is false (no comparison).
* "-tar" or "--useTarget": Whether to use information of the current instruction target for the cost estimation. Default is false (no target usage).
* "-str" or "--useStructures": Whether to use information of existing structures for the cost estimation. Default is false (no structures usage).
* "-st" or "--scenarioType": What kind of scenario is being used. Default is "SimpleBridge".

When using NNs, keep in mind that it may be necessary to change the data scaling according to what the Python script has output for min and max values.
For that, either create a new json file containing the parameters, or change the values in the default file.
The values should always fit the values that were used for the training of the corresponding NN model.
For more information on how to adapt to new scenarios, check the wiki.
Also, depending on which results you expect (fixed vs non-fixed versions), make sure to be using the correct ``bridge.lisp``-file.

If you wish to train and use your own models, please refer to: https://github.com/minecraft-saar/cost-estimation
 
A good possible config to start planning using a trained NN for the simple bridge scenario is:
 ``java -jar $PATH_TO_JAR $PATH_TO_DOMAIN $PATH_TO_PROBLEM -m 1000000 -t 18000000 -exp 10 -c NN --level MEDIUM -e deadEnd -wf $PATH_TO_WEIGHT_FILE -nnp NNPATH -tar -str``
 (The high value for timeout is just a general setting for good measure, otherwise the program may take too long when running NLG and NN at once for comparison.)
