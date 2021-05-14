package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;

import java.util.*;

/**
 * Thread designed for minimax, using a given state as the root
 */
public class MinimaxThread extends Thread{
    private Optional<Integer> score;
    private final int maxDepth;
    private final AiState state;

    private final long start;
    private final long limit;
    private final double timeout_factor = 0.95;
    private boolean timeout = false;

    private Thread thread;

    /**
     * @return optimal score from minimax tree
     */
    public Optional<Integer> getScore(){return this.score;}

    public Thread getThread(){
        return this.thread;
    }

    /**
     * Hash Table containing the score (if seen previously) for a given board
     */
    private Dictionary<Long, Optional<Integer>> table;

    /**
     * @param aiState
     * @param depth
     * @param alpha
     * @param beta
     * @return Pair: Boolean flag indicating if the move time limit was reached, Integer representing optimal score from minimax tree
     */
    private Pair<Boolean, Integer> minimax(AiState aiState, int depth, double alpha, double beta){
        /**
         * Returns score of current state if reached required depth or a terminatng game state
         */
        if(depth == 0 || aiState.isTerminal()){
            return Pair.pair(false, aiState.getScore());
        }
        int minMaxEval = 1000;
        if(aiState.isMaximizing()) minMaxEval = -minMaxEval;

        for(Action action : aiState.getActions()){
            /**
             * Returns if time limit reached
             */
            if(System.currentTimeMillis() - start >= (timeout_factor * limit)){
                if(aiState.isMaximizing()){
                    if(action.getNextState().getScore() > minMaxEval) return Pair.pair(true, aiState.getScore());
                    else return Pair.pair(true, minMaxEval);
                }else{
                    if(action.getNextState().getScore() < minMaxEval) return Pair.pair(true, aiState.getScore());
                    else return Pair.pair(true, minMaxEval);
                }
            }

            /**
             * Checks table to see if state already evaluated
             */
            Optional<Integer> tableEntry;
            try{
                tableEntry = table.get(action.getNextState().getUniqueKey());
            } catch(Exception e){
                tableEntry = Optional.empty();
            }

            Pair<Boolean, Integer> evalPair;

            if ( tableEntry == null || tableEntry.isEmpty()) {
                evalPair = minimax(action.getNextState(), depth - 1, alpha, beta);
                //table.put(action.getNextState().getUniqueKey(), Optional.of(evalPair.right()));

            } else {
                //System.out.println("TABLE");
                evalPair = Pair.pair(false, tableEntry.get());
            }
            /**
             * Gets state evaluation and prunes tree
             */
            int eval = evalPair.right();
            if (aiState.isMaximizing()) {
                if (eval > alpha) alpha = eval;
            } else {
                if (eval < beta) beta = eval;
            }

            if (beta <= alpha) {
                //System.out.println("prune");
                if(aiState.isMaximizing()){
                    if(action.getNextState().getScore() > minMaxEval) minMaxEval = aiState.getScore();
                }else{
                    if(action.getNextState().getScore() < minMaxEval) minMaxEval = aiState.getScore();
                }
                break;
            }

            if (aiState.isMaximizing()) {
                if (eval > minMaxEval) minMaxEval = eval;
            } else {
                if (eval < minMaxEval) minMaxEval = eval;
            }

            if (evalPair.left()) return Pair.pair(true, minMaxEval);
        }
        return Pair.pair(false, minMaxEval);
    }

    public void run(){
        synchronized (this){
            Pair<Boolean, Integer> pair = minimax(this.state, this.maxDepth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            this.timeout = pair.left();
            score = Optional.of(pair.right());
        }
    }

    public void start(){
        if(thread == null){
            thread = new Thread(this);
            thread.start();
        }
    }
    public MinimaxThread(AiState state, int maxDepth, Pair<Long, Long> timeout){
        this.start = timeout.left();
        this.limit = timeout.right();
        this.maxDepth = maxDepth;
        this.state = state;
        this.table = new Hashtable<>();
    }
}
