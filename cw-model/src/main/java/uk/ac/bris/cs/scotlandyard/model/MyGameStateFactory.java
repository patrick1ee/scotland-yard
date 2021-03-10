package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {

		private final class MyTicketBoard implements TicketBoard{

			private final ImmutableMap<ScotlandYard.Ticket, Integer> tickets;

			@Override
			public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
				return this.tickets.getOrDefault(ticket, 0);
			}

			private MyTicketBoard(ImmutableMap<ScotlandYard.Ticket, Integer> tickets){
				this.tickets = tickets;
			}
		}

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

		@Override public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player p : this.detectives){
				if(p.piece() == detective) return Optional.of(p.location());
			}
			return Optional.empty();
		}

		@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			for (Player p : this.everyone){
				if(p.piece() == piece) return Optional.of(new MyTicketBoard(p.tickets()));
			}
			return Optional.empty();
		}

		@Override public ImmutableList<LogEntry> getMrXTravelLog() { return this.log; }
		@Override public ImmutableSet<Piece> getWinner() { return this.winner; }
		@Override public ImmutableSet<Move> getAvailableMoves() { return this.moves; }
		@Override public GameState advance(Move move) {  return null;  }

		private boolean verifyDetectives(List<Player> detectives){
			Set<Integer> locs = new HashSet<>();
			Set<String> cols = new HashSet<>();
			for(Player p : detectives){
				if(!p.piece().isDetective()) return false;
				if(p.piece().webColour() == "#000") return false;
				locs.add(p.location());
				cols.add(p.piece().webColour());
			}
			return cols.size() == detectives.size() && locs.size() == detectives.size();


		}

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

			if(this.setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty");
			if(verifyDetectives(this.detectives)) throw new IllegalArgumentException("Invalid detectives");
			if(this.mrX.piece().webColour() != "#000") throw new IllegalArgumentException("MrX is not the black piece");
		}
	}

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

	}

}
