package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;

import java.util.Dictionary;
import java.util.Optional;

public class Minimax{

    private final double timeout_factor = 0.95;

    private final long start;
    private final long limit;
    private final int maxDepth;
    private final AiState initialAiState;

    private Dictionary<Long, Optional<Integer>> table;

    public AiState getInitialState(){
        return this.initialAiState;
    }

    public int getMaxDepth(){
        return this.maxDepth;
    }

    protected Pair<Boolean, Integer> minimax(AiState aiState, int depth, double alpha, double beta){
        System.out.println(depth);
        if(depth == 0 || aiState.isTerminal()){
            System.out.println("T");
            if(aiState.isTerminal()) //System.out.println("terminal, " + state.getScore());
            return Pair.pair(false, aiState.getScore());
        }
        int minMaxEval = 1000;
        if(aiState.isMaximizing()) minMaxEval = -minMaxEval;
        for(Action action : aiState.getActions()){
            //System.out.println(System.currentTimeMillis() - timeout.left());
            if(System.currentTimeMillis() - start >= (timeout_factor * limit)){
                if(aiState.isMaximizing()){
                    if(action.getNextState().getScore() > minMaxEval) return Pair.pair(true, aiState.getScore());
                    else return Pair.pair(true, minMaxEval);
                }else{
                    if(action.getNextState().getScore() < minMaxEval) return Pair.pair(true, aiState.getScore());
                    else return Pair.pair(true, minMaxEval);
                }


            }

            Optional<Integer> tableEntry;
            try{
                tableEntry = table.get(action.getNextState().getUniqueKey());
            } catch(Exception e){
                tableEntry = Optional.empty();
            }

            Pair<Boolean, Integer> evalPair;

            if (tableEntry.isEmpty()) {
                evalPair = minimax(action.getNextState(), depth - 1, alpha, beta);
                //table.put(action.getNextState().getUniqueKey(), Optional.of(evalPair.right()));

            } else {
                evalPair = Pair.pair(false, tableEntry.get());
            }

            int eval = evalPair.right();
            if (aiState.isMaximizing()) {
                if (eval > alpha) alpha = eval;
            } else {
                if (eval < beta) beta = eval;
            }

            if (beta <= alpha) {
                //System.out.println("prune");
                break;
            }

            if (aiState.isMaximizing()) {
                if (eval > minMaxEval) minMaxEval = eval;
            } else {
                if (eval < minMaxEval) minMaxEval = eval;
            }

            if (evalPair.left()) return Pair.pair(true, minMaxEval);
        }
        if(minMaxEval == -1000){
            System.out.println(depth);
            for(Action action : aiState.getActions()){

            }
            System.exit(0);
        }
        return Pair.pair(false, minMaxEval);
    }
    public Minimax(AiState initialAiState, int maxDepth, Pair<Long, Long> timeout){
        this.start = timeout.left();
        this.limit = timeout.right();
        this.maxDepth = maxDepth;
        this.initialAiState = initialAiState;
    }

}
