package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		@Override public GameSetup getSetup() {  return this.setup; }
		@Override public ImmutableSet<Piece> getPlayers() { return this.remaining; }
		@Override public Optional<Integer> getDetectiveLocation(Piece.Detective detective) { return null; }
		@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) { return null; }
		@Override public ImmutableList<LogEntry> getMrXTravelLog() { return this.log; }
		@Override public ImmutableSet<Piece> getWinner() { return null; }
		@Override public ImmutableSet<Move> getAvailableMoves() { return this.moves; }
		@Override public GameState advance(Move move) {  return null;  }

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives){

			this.setup = Objects.requireNonNull(setup, "setup must not be null");
			this.remaining = Objects.requireNonNull(remaining, "remaining must not be null");
			this.log = Objects.requireNonNull(log, "log must not be null");
			this.mrX = Objects.requireNonNull(mrX, "mrX must not be null");
			this.detectives = Objects.requireNonNull(detectives, "detectives must not be null");
		}
	}

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

	}

}
