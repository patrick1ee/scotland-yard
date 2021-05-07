package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	private static final double correction = 2;
	private static final double discount = 0.9;
	private static final int depth = 1;
	private static final int maxDepth = 10;

	private Dictionary<Object, Optional<Double>> table;

	private static int moveCost(Move move){
		int cost = 0;
		for(ScotlandYard.Ticket t : move.tickets()){
			switch(t){
				case TAXI: cost += 1;
				case BUS: cost += 2;
				case UNDERGROUND: cost += 3;
				case SECRET: cost += 4;
				case DOUBLE: cost += 5;
			}
		}
		return cost;
	}

	private static ImmutableList<Move> filterMoves(ImmutableList<Move> moves){
		if(moves.size() == 0) return ImmutableList.of();//throw new IllegalArgumentException("Moves array cannot be empty!");
		Dictionary<Integer, Integer> dict = new Hashtable<Integer, Integer>();
		Dictionary<Integer, Move> map = new Hashtable<Integer, Move>();
		List<Move> filtered = new ArrayList<>();
		for(Move m : moves){
			int dest = getMoveDestination(m);
			if(dict.get(dest) == null){
				dict.put(dest, moveCost(m));
				map.put(dest, m);
			} else{
				int cost = moveCost(m);
				if(cost <= dict.get(dest)){
					dict.put(dest, cost);
					map.put(dest, m);
				}
			}
		}
		Enumeration keys = map.keys();
		while(keys.hasMoreElements()){
			filtered.add(map.get(keys.nextElement()));
		}
		//System.out.println(moves.size() + ", " + filtered.size());
		return ImmutableList.copyOf(filtered);
	}

	static int getMoveDestination(Move move){
		return move.visit(new Move.Visitor<Integer>() {
			@Override
			public Integer visit(Move.SingleMove move) {
				return move.destination;
			}

			@Override
			public Integer visit(Move.DoubleMove move) {
				return move.destination2;
			}
		});
	}



	static double score(Board board, int location, boolean mrX){
		if(board.getWinner().contains(Piece.MrX.MRX)) return 100;
		else if(board.getWinner().isEmpty()) {
			var moves = board.getAvailableMoves().asList();
			if(mrX) return correction * (Search.getAverageDistanceFromDetectives(board, location, Search.getDetectiveLocations(board)) + moves.size());
			else return correction * (Search.getAverageDistanceFromDetectives(board, location, Search.getDetectiveLocations(board)) - moves.size());
		}
		else return -100;
	}

	private Pair<Boolean, Double> minimax(Board board, int depth, double alpha, double beta, int location, Pair<Long, Long> timeout){

		var moves = board.getAvailableMoves().asList();
		moves = filterMoves(moves);
		Move[] movesArray = new Move[moves.size()];
		moves.toArray(movesArray);
		movesArray = Search.mergeSort(board, movesArray);
		moves = ImmutableList.copyOf(movesArray);

		boolean maximizingMrX = moves.stream().anyMatch(move -> move.commencedBy().isMrX());
		if(depth == 0 || !board.getWinner().isEmpty()) return Pair.pair(false, score(board, location, maximizingMrX));
		if(maximizingMrX){
			//System.out.println("x");
			double maxEval = -1000;
			for(Move move : moves){
				if(System.currentTimeMillis() - timeout.left() >= (0.9 * timeout.right() * 1000)) return Pair.pair(true, maxEval);
				Board nextState = ((Board.GameState)board).advance(move);
				Optional<Double> score = this.table.get(nextState);
				double eval;
				if(score.isEmpty()){
					eval = discount * minimax(((Board.GameState)board).advance(move), depth - 1, alpha, beta, getMoveDestination(move), timeout).right();
				}
				else{
					eval = score.get();
				}
				if(eval >= alpha) alpha = eval;
				if(beta <= alpha){
					System.out.println("prune");
					break;
				}
				if(eval > maxEval) maxEval = eval;
			}
			return Pair.pair(false, maxEval + score(board, location, true));

		} else{
			//System.out.println("d");
			double minEval = 1000;
			for(Move move : moves){
				if(System.currentTimeMillis() - timeout.left() >= (0.9 * timeout.right() * 1000)) return Pair.pair(true, minEval);
				double eval = discount * minimax(((Board.GameState)board).advance(move), depth - 1, alpha, beta, location, timeout).right();
				if(eval <= beta) beta = eval;
				if(beta <= alpha){
					System.out.println("prune");
					break;
				}
				if(eval < minEval) minEval = eval;
			}
			return Pair.pair(true, minEval + score(board, location, false));
		}
	}

	@Nonnull @Override public String name() { return "Name me!"; }

	public void onStart(){
		this.table = new Hashtable<>();
	}

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		long start = System.currentTimeMillis();
		var moves = board.getAvailableMoves().asList();
		moves = filterMoves(moves);
		Move[] movesArray = new Move[moves.size()];
		moves.toArray(movesArray);
		movesArray = Search.mergeSort(board, movesArray);
		moves = ImmutableList.copyOf(movesArray);

		Board b = (((Board.GameState)board).advance(moves.get(0)));

		double max = Double.NEGATIVE_INFINITY;
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
		System.out.println(maxMove);
		return maxMove;
	}

}
