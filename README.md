# jshop

JSHOP Usage:

The JSHOp planner has to search modi right now, a blind depth-first search and Monte Carlo Tree Search.
The first is a blind depth first search and is started with:

java -jar PathToJAR DOMAINFILE PROBLEMFILE -s [--all] [-d INT1-10]

Without "--all" the planner will stop after the first solution found, with it the planner will search for all solutions. 
With the option "-d" the output can be made more verbose with 1 being the lowest amount of additional output, and it increasing with each number up to a max of 10

Monte Carlo Tree Search can be run with: 

java -jar PathToJAR DOMAINFILE PROBLEMFILE -m MCTSRUNS -c COSTFUNCTION -e EXPANSIONPOLICY [-b] [--recursiveSimulation NUMBER]

MCTSRUNS can be any positive integer. 

"-b": enables branch and bound pruning (i.e. prune against best solution found so far)

"--recursiveSimulation NUMBER": makes "NUMBER" many "depth-first-search" like runs that use backtracking

EXPANSIONPOLICY can be: 
"simple" : Standard MCTS behavior
"deadEnd" : Deletes DeadEnd Nodes to prevent repeat visits of same deadend. The best performing setting
"primitive" : Collapses MCTS Search Nodes unless the task is primitive. This is an old experimental setting.
"landmark" : Same MCTS config as "simple" but use landmarks as search guidance. To work options "-l" and "-lf LANDMRAKFILE" are also required. 

COSTFUNCTION can be:
"UNIT": All actions have unit cost
"BASIC": Actions have cost as specified in the domain file
"STATEDEPENDENT": Uses hand crafted costfunctions for Minecraft, Blocksworld and Rovers domains does not work for other domains.
"NLG": Only usable with Minecraft domain, calls the NLG System for cost estimates. 
If the option "NLG" is chosen, the abstraction level for Minecraft instructions can be chosen by adding option "--level ABSTRACTION". ABSTARCTION can be "BLOCK", "MEDIUM" or "HIGHLEVEL".

Additional options for MCTS, their default is already set according to best performance:
"--noRec": Disables the collapsing of MCTS Search Nodes. Really bad for performance

"--noRandom": The first MCTS run will not include any randomness and follow the method ordering given in the domain
"--printTree": Prints a .dot style output of the MCTS search tree
"--min": Uses the minimum cost when updating the MCTS Nodes instead of the UCT-formula
"--fastSimulation": The states generated in simulations are not saved
"-bf": Use branch and bound pruning and fastSimulations
"-exp REALNUMBER": Set the exploration factor of MCTS to given real number
"--randomSeed NUMBER": Set the seed of the random generator to NUMBER
