package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	//Depth to search minimax tree to
	private static final int depth = 10;

	@Nonnull @Override public String name() { return "Name me!"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		long start = System.currentTimeMillis();
		int MrXLocation = board.getAvailableMoves().iterator().next().source();

		AiState currentAiState = new AiState(board, MrXLocation);

		/**
		 * Initialises thread for each move
		 */
		List<Pair<MinimaxThread, Action>> threads = new ArrayList<>();
		ActionSet actions = currentAiState.getActions();
		for(Action action : actions){
			MinimaxThread m = new MinimaxThread(action.getNextState(), depth, Pair.pair(start, timeoutPair.left() * 1000));
			m.start();
			threads.add(new Pair(m, action));
		}

		/**
		 * Waits for threads to finish executing
		 */
		while(threads.stream().anyMatch(t -> t.left().getThread().isAlive())){}
		System.out.println("Threading complete");

		/**
		 * Selects optimal move from minimax tree
		 */
		double max = Double.NEGATIVE_INFINITY;
		Move maxMove = null;
		for(Pair<MinimaxThread, Action> t : threads){
			System.out.println(t.left().getScore().get());
			if(t.left().getScore().get() > max){
				max = t.left().getScore().get();
				maxMove = t.right().getMove();
			}
		}

		return maxMove;
	}

}
