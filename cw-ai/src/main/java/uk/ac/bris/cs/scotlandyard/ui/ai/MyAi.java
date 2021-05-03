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
	private static final int depth = 5;
	private static final double breadth = 1.0;

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
			if(mrX) return correction * Search.getAverageDistanceFromDetectives(board, location, Search.getDetectiveLocations(board)) * moves.size();
			else return (correction / Search.getAverageDistanceFromDetectives(board, location, Search.getDetectiveLocations(board))) / moves.size();
		}
		else return 0;
	}

	private static double minimax(Board board, int depth, double alpha, double beta, int location){

		var moves = board.getAvailableMoves().asList();
		moves = filterMoves(moves);
		Move[] movesArray = new Move[moves.size()];
		moves.toArray(movesArray);
		movesArray = Search.mergeSort(board, movesArray);
		moves = ImmutableList.copyOf(movesArray);

		boolean maximizingMrX = moves.stream().anyMatch(move -> move.commencedBy().isMrX());
		if(depth == 0 || !board.getWinner().isEmpty()) return score(board, location, maximizingMrX);
		if(maximizingMrX){
			//System.out.println("x");
			double maxEval = -1000;
			for(Move move : moves.subList(0, (int)Math.floor(moves.size() * breadth))){
				double eval = /**getAverageDistanceFromDetectives(board, location) +*/
						discount * minimax(((Board.GameState)board).advance(move), depth - 1, alpha, beta, getMoveDestination(move));
				if(eval >= alpha) alpha = eval;
				if(beta <= alpha){
					System.out.println("prune");
					break;
				}
				if(eval > maxEval) maxEval = eval;
			}
			return maxEval;

		} else{
			//System.out.println("d");
			double minEval = 1000;
			for(Move move : moves.subList(0, (int)Math.floor(moves.size() * breadth))){
				double eval = /**getAverageDistanceFromDetectives(board, location) +*/
						discount * minimax(((Board.GameState)board).advance(move), depth - 1, alpha, beta, location);
				if(eval <= beta) beta = eval;
				if(beta <= alpha){
					System.out.println("prune");
					break;
				}
				if(eval < minEval) minEval = eval;
			}
			return minEval;
		}
	}

	@Nonnull @Override public String name() { return "Name me!"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		var moves = board.getAvailableMoves().asList();
		moves = filterMoves(moves);
		Move[] movesArray = new Move[moves.size()];
		moves.toArray(movesArray);
		movesArray = Search.mergeSort(board, movesArray);
		moves = ImmutableList.copyOf(movesArray);
		double max = Double.NEGATIVE_INFINITY;
		Move maxMove = null;
		//System.out.println(moves);
		int limit = (int)Math.floor(moves.size() * breadth);
		if(limit == 0) limit = 1;
		for(Move move : moves.subList(0, limit)){
			double score = minimax(((Board.GameState)board).advance(move), depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, getMoveDestination(move));
			System.out.println(score);
			if(score > max){
				max = score;
				maxMove = move;
			}
		}
		System.out.println(maxMove);
		return maxMove;
	}

}
