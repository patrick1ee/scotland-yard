package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	private static final int depth = 10;
	private static final double timeout_factor = 0.95;
    private static final double correction = 2;
    private static final double discount = 0.9;
	private static final int maxDepth = 10;

	private Dictionary<Long, Optional<Integer>> table;

	/*private Pair<Double, Boolean> minimax(State state, int depth, double alpha, double beta, int location, Pair<Long, Long> timeout){

		var moves = board.getAvailableMoves().asList();
		if(moves.size() == 0) return Pair.pair(false, 0.0);
		moves = filterMoves(moves);
		Move[] movesArray = new Move[moves.size()];
		moves.toArray(movesArray);
		movesArray = Search.mergeSort(board, movesArray);
		moves = ImmutableList.copyOf(movesArray);

		boolean maximizingMrX = moves.stream().anyMatch(move -> move.commencedBy().isMrX());
		double minMaxEval = 1000;
		if(depth == 0 || !board.getWinner().isEmpty()){
		    return Pair.pair(false, score(board, location, maximizingMrX));
        }
		else if(maximizingMrX){
		    minMaxEval = -minMaxEval;
        }

        for(Move move : moves) {

            if (System.currentTimeMillis() - timeout.left() >= (timeout_factor * timeout.right() * 1000))
                return Pair.pair(true, minMaxEval);

            Board nextState = ((Board.GameState) board).advance(move);
            long key = hashBoard(nextState, location);
            Optional<Double> tableEntry = table.get(key);
            double eval;

            if (tableEntry == null) {
                Pair<Boolean, Double> scorePair = minimax(nextState, depth - 1, alpha, beta, getMoveDestination(move), timeout);
                if (scorePair.left()) return Pair.pair(true, scorePair.right());
                else {
                    eval = scorePair.right();
                    table.put(key, Optional.of(eval));
                }

            } else {
                eval = tableEntry.get();
            }


            if (maximizingMrX) {
                if (eval >= alpha) alpha = eval;
            } else {
                if (eval <= beta) beta = eval;
            }

            if (beta <= alpha) {
                System.out.println("prune");
                break;
            }

            if (maximizingMrX) {
                if (eval > minMaxEval) minMaxEval = eval;
            } else {
                if (eval < minMaxEval) minMaxEval = eval;
            }
        }
        return Pair.pair(false, minMaxEval + score(board, location, false));

		}*/

	@Nonnull @Override public String name() { return "Name me!"; }

	private Pair<Boolean, Integer> minimax(State state, int depth, double alpha, double beta, Pair<Long, Long> timeout){
		if(depth == 0 || state.isTerminal()){
			return Pair.pair(false, state.getScore());
		}
		int minMaxEval = 1000;
		if(state.isMaximizing()) minMaxEval = -minMaxEval;
		for(Action action : state.getActions()){
			//System.out.println(System.currentTimeMillis() - timeout.left());
			if(System.currentTimeMillis() - timeout.left() >= (timeout_factor * timeout.right())){
				System.out.println("T");
				return Pair.pair(true, minMaxEval);
			}
			Optional<Integer> tableEntry = table.get(state.getUniqueKey());
			int eval;

			if (tableEntry == null) {
				Pair<Boolean, Integer> evalPair = minimax(action.getNextState(), depth - 1, alpha, beta, timeout);
				if (evalPair.left()) return Pair.pair(true, evalPair.right());
				else {
					eval = evalPair.right();
					table.put(state.getUniqueKey(), Optional.of(eval));
				}

			} else {
				eval = tableEntry.get();
			}
			if (state.isMaximizing()) {
				if (eval >= alpha) alpha = eval;
			} else {
				if (eval <= beta) beta = eval;
			}

			if (beta <= alpha) {
				System.out.println("prune");
				break;
			}

			if (state.isMaximizing()) {
				if (eval > minMaxEval) minMaxEval = eval;
			} else {
				if (eval < minMaxEval) minMaxEval = eval;
			}
		}
		return Pair.pair(false, minMaxEval);
	}

	public void onStart(){
		this.table = new Hashtable<>();
	}

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		long start = System.currentTimeMillis();
		int MrXLocation = board.getAvailableMoves().iterator().next().source();

		State currentState = new State(board, MrXLocation);
		/*double max = Double.NEGATIVE_INFINITY;
		Move maxMove = null;
		System.out.println(timeoutPair);
		for(int i = 0; i < maxDepth; i++){
			if(System.currentTimeMillis() - start >= (0.9 * timeoutPair.left() * 1000)) break;
			double subMax = Double.NEGATIVE_INFINITY;
			Move subMaxMove = null;
			for(Move move : moves){
				if(System.currentTimeMillis() - start >= (0.9 * timeoutPair.left() * 1000)) break;
				Pair<Boolean, Double> scorePair = minimax(((Board.GameState)board).advance(move), i, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, getMoveDestination(move), Pair.pair(start, timeoutPair.left()));
				if(scorePair.left()){
					i = maxDepth;
					break;
				}
				double score = scorePair.right();
				System.out.println(score);
				if(score > subMax){
					subMax = score;
					subMaxMove = move;
				}
			}
			if(subMax > max){
				max = subMax;
				maxMove = subMaxMove;
			}
		}
		System.out.println(maxMove);*/
		double max = Double.NEGATIVE_INFINITY;
		Move maxMove = null;
		for(Action action : currentState.getActions()){
			if(System.currentTimeMillis() - start >= (timeout_factor * timeoutPair.left() * 1000)) break;

			Pair<Boolean, Integer> scoreTime = minimax(action.getNextState(), depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Pair.pair(start, timeoutPair.left() * 1000));
			double score = scoreTime.right();
			System.out.println(score);
			if(score > max){
				max = score;
				maxMove = action.getMove();
			}
			if(scoreTime.left()) break;
		}
		System.out.println(max + ", " + maxMove);
		return maxMove;
	}

}
