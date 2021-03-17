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

	private static final List<Integer> revealRounds = List.of(3, 8, 13, 18, 24);

	private static boolean verifyDetectives(List<Player> detectives){
		List<Integer> locs = new ArrayList<Integer>();
		List<Piece> pcs = new ArrayList<Piece>();
		for(Player p : detectives){
			if(!p.piece().isDetective()) return false;
			if(p.has(ScotlandYard.Ticket.DOUBLE)) return false;
			if(p.has(ScotlandYard.Ticket.SECRET)) return false;
			if(locs.contains(p.location())) return false;
			if(pcs.contains(p.piece())) return false;
			locs.add(p.location());
			pcs.add(p.piece());

		}
		return true;


	}

	public static ImmutableSet<Integer> getDestinations(Move move){
		return move.visit(new Move.Visitor<ImmutableSet<Integer>>() {
			@Override
			public ImmutableSet<Integer> visit(Move.SingleMove move) {
				return ImmutableSet.of(move.destination);
			}

			@Override
			public ImmutableSet<Integer> visit(Move.DoubleMove move) {
				return ImmutableSet.of(move.destination1, move.destination2);
			}
		});
	}

	private static ImmutableSet<Move> makeMoves(
			GameSetup setup,
			List<Player> detectives,
			Player player,
			int source,
			boolean firstMove) {
		final var moves = new ArrayList<Move>();
		for (int destination : setup.graph.adjacentNodes(source)) {
			boolean occupied = false;
			for (Player p : detectives) {
				if (p.location() == destination) {
					occupied = true;
					break;
				}
			}
			if (!occupied) {
				for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if(player.hasAtLeast(t.requiredTicket(), 1)){
						moves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
						if(firstMove && player.hasAtLeast(ScotlandYard.Ticket.DOUBLE, 1)){
							ImmutableSet<Move> subsequentMoves = makeMoves(setup, detectives, player.use(t.requiredTicket()), destination, false);
							for(Move m : subsequentMoves){
								moves.add(new Move.DoubleMove(
										player.piece(), source, t.requiredTicket(), destination, m.tickets().iterator().next(), ((Move.SingleMove)m).destination));
							}
						}
					}

				}
				// TODO consider the rules of secret moves here
				//  add moves to the destination via a secret ticket if there are any left with the player
				if(player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1))  moves.add(
						new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
			}
		}
		return ImmutableSet.copyOf(moves);
	}

	private final class MyGameState implements GameState {

		private final class MyTicketBoard implements TicketBoard {

			private final ImmutableMap<ScotlandYard.Ticket, Integer> tickets;

			@Override
			public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
				return this.tickets.getOrDefault(ticket, 0);
			}

			private MyTicketBoard(ImmutableMap<ScotlandYard.Ticket, Integer> tickets) {
				this.tickets = tickets;
			}
		}

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Piece> winner;


		@Override
		public GameSetup getSetup() {
			return this.setup;
		}

		@Override
		public ImmutableSet<Piece> getPlayers() {
			return this.remaining;
		}

		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player p : this.detectives) {
				if (p.piece() == detective) return Optional.of(p.location());
			}
			return Optional.empty();
		}

		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if(piece.isMrX()) return Optional.of(new MyTicketBoard(mrX.tickets()));
			for (Player p : this.detectives) {
				if (p.piece() == piece) return Optional.of(new MyTicketBoard(p.tickets()));
			}
			return Optional.empty();
		}

		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return this.log;
		}

		@Override
		public ImmutableSet<Piece> getWinner() {
			return this.winner;
		}

		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			final var moves = new ArrayList<Move>();
			for (Player p : detectives) {
				moves.addAll(makeMoves(this.setup, this.detectives, p, p.location(), false));
			}
			moves.addAll(makeMoves(this.setup, this.detectives, this.mrX, this.mrX.location(), true));
			return ImmutableSet.copyOf(moves);
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			if(!this.getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: "+move);

			Player mrXNew = null;
			ImmutableList<LogEntry> logNew = ImmutableList.copyOf(log);
			if(move.commencedBy().isMrX()){
				mrXNew = mrX.use(move.tickets());

				for(int dest: getDestinations(move)){
					if(revealRounds.contains(log.size())) logNew.add(LogEntry.reveal(move.tickets().iterator().next(), dest));
					else logNew.add(LogEntry.hidden(move.tickets().iterator().next()));
					mrXNew = mrXNew.at(dest);
				}

			}

			List<Player> detectivesNew = ImmutableList.copyOf(detectives);

			for (Player p : detectivesNew) {
				if (p.piece().equals(move.commencedBy())) {
					p = p.use(move.tickets()).at(getDestinations(move).iterator().next());
					mrXNew = mrX.give(move.tickets());
				}
			}

			return new MyGameState(setup, remaining, logNew, mrXNew, detectivesNew);
		}

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {

			this.setup = Objects.requireNonNull(setup, "setup must not be null");
			this.remaining = Objects.requireNonNull(remaining, "remaining must not be null");
			this.log = Objects.requireNonNull(log, "log must not be null");
			this.mrX = Objects.requireNonNull(mrX, "mrX must not be null");
			this.detectives = Objects.requireNonNull(detectives, "detectives must not be null");
			this.winner = ImmutableSet.of();

			if (this.setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty");
			if(this.setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty");
			if (!verifyDetectives(this.detectives)) throw new IllegalArgumentException("Invalid detectives");
			if (this.mrX.piece().webColour() != "#000")
				throw new IllegalArgumentException("MrX is not the black piece");

		}
	}


	@Nonnull
	@Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		Set s = new HashSet<Piece>();
		s.add(mrX.piece());
		for(Player p : detectives){
			s.add(p.piece());
		}
			return new MyGameState(setup, ImmutableSet.copyOf(s), ImmutableList.of(), mrX, detectives);

	}
}
