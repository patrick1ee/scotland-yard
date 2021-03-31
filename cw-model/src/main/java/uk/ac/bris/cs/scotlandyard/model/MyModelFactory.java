package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private final class MyModel implements Model {

		private ImmutableSet<Observer> observers;

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return null;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if(observers.contains(observer)) throw new IllegalArgumentException("Cannot register observer more than once");
			Set<Observer> observersNew = new HashSet<Observer>();
			observersNew.addAll(observers);
			observersNew.add(observer);
			observers = ImmutableSet.copyOf(observersNew);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if(!observers.contains(observer)) throw new IllegalArgumentException("Cannot unregister an observer which has not seen previously registered");
			Set<Observer> observersNew = new HashSet<Observer>();
			observersNew.addAll(observers);
			observersNew.remove(observer);
			observers = ImmutableSet.copyOf(observersNew);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return this.observers;
		}

		@Override
		public void chooseMove(@Nonnull Move move) {

		}

		public MyModel(){
			this.observers = ImmutableSet.of();
		}
	}
	@Nonnull
	@Override
	public Model build(GameSetup setup,
					   Player mrX,
					   ImmutableList<Player> detectives) {

		return new MyModel();
	}
}
