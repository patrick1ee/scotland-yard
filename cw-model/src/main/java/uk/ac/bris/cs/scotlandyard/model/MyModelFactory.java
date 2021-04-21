package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private MyGameStateFactory gameStateFactory = new MyGameStateFactory();

	private final class MyModel implements Model {

		private List<Observer> observers;
		private Board.GameState game;

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return this.game;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if(observer.equals(null)) throw new NullPointerException();
			if(observers.contains(observer)) throw new IllegalArgumentException("Cannot register observer more than once");
			this.observers.add(observer);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if(observer.equals(null)) throw new NullPointerException();
			if(!observers.contains(observer)) throw new IllegalArgumentException("Cannot unregister an observer which has not seen previously registered");
			this.observers.remove(observer);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(this.observers);
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			this.game = this.game.advance(move);
			Observer.Event e;

			if(this.game.getWinner().isEmpty()) e = Observer.Event.MOVE_MADE;
			else e = Observer.Event.GAME_OVER;

			for(Observer o : this.observers){
				o.onModelChanged(this.game, e);
			}
		}

		public MyModel(Board.GameState game){
			this.observers = new ArrayList<Observer>();
			this.game = game;
		}
	}
	@Nonnull
	@Override
	public Model build(GameSetup setup,
					   Player mrX,
					   ImmutableList<Player> detectives) {

		return new MyModel(gameStateFactory.build(setup, mrX, detectives));
	}
}
