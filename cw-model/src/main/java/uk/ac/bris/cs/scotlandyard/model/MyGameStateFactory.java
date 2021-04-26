package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableDoubleArray;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

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

	private static boolean unoccupied(int location, List<Player> detectives){
		for(Player p : detectives){
			if(p.location() == location) return false;
		}
		return true;
	}

	private static ImmutableSet<Piece> getPieces(List<Player> players){
		Set<Piece> pieces = new HashSet<Piece>();
		for(Player p : players){
			pieces.add(p.piece());
		}
		return ImmutableSet.copyOf(pieces);
	}

	private static ImmutableSet<Integer> getDestinations(Move move){
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

	private static ImmutableList<LogEntry> updateLog(ImmutableList<LogEntry> log, GameSetup setup, Move move){
		List<LogEntry> logNew = new ArrayList<LogEntry>();

		for(LogEntry l : log){
			logNew.add(l);
		}

		for(int dest: getDestinations(move)){
			if(setup.rounds.get(logNew.size())) logNew.add(LogEntry.reveal(move.tickets().iterator().next(), dest));
			else logNew.add(LogEntry.hidden(move.tickets().iterator().next()));
		}

		return ImmutableList.copyOf(logNew);

	}

	private static Player updatePlayer(Player player, Move move){
		Player newPlayer = player.use(move.tickets());
		for(int dest : getDestinations(move)){
			newPlayer = newPlayer.at(dest);
		}
		return newPlayer;
	}

	private static ImmutableSet<Piece> fillRemaining(ImmutableSet<Piece> remaining, Player mrX, List<Player> detectives){
		Set<Piece> newRemaining = new HashSet<Piece>();
		newRemaining.add(mrX.piece());
		for(Player p : detectives){
			newRemaining.add(p.piece());
		}
		return ImmutableSet.copyOf(newRemaining);
	}

	private static boolean cannotMove(List<Player> players, List<Player>detectives, GameSetup setup){
		for(Player p : players){
			if(!getSingleMoves(setup, detectives, p, p.location()).isEmpty()) return false;
		}
		return true;
	}

	private static ImmutableSet<Move.SingleMove> getSingleMoves(
			GameSetup setup,
			List<Player> detectives,
			Player player,
			int source
	){
		List moves = new ArrayList<Move.SingleMove>();
		for (int destination : setup.graph.adjacentNodes(source)) {

			if (unoccupied(destination, detectives)) {
				for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if(player.has(t.requiredTicket())){
						moves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
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

	private static ImmutableSet<Move.DoubleMove> getDoubleMoves(
			GameSetup setup,
			List<Player> detectives,
			Player player,
			ImmutableSet<Move.SingleMove> singleMoves
	){
		List moves = new ArrayList<Move.DoubleMove>();
		for(Move.SingleMove singleMove : singleMoves){
			for(int destination: setup.graph.adjacentNodes(singleMove.destination)){
				if(unoccupied(destination, detectives)){
					for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(singleMove.destination, destination, ImmutableSet.of())) {
						if(player.use(singleMove.ticket).has(t.requiredTicket())){
							moves.add(new Move.DoubleMove(player.piece(), singleMove.source(), singleMove.ticket, singleMove.destination, t.requiredTicket(), destination));
						}

					}
					// TODO consider the rules of secret moves here
					//  add moves to the destination via a secret ticket if there are any left with the player
					if(player.use(singleMove.ticket).hasAtLeast(ScotlandYard.Ticket.SECRET, 1))  moves.add(
							new Move.DoubleMove(player.piece(), singleMove.source(), singleMove.ticket, singleMove.destination, ScotlandYard.Ticket.SECRET, destination));
				}
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
			if(remaining.contains(mrX.piece())){
				ImmutableSet<Move.SingleMove> mrXSingleMoves = getSingleMoves(this.setup, this.detectives, this.mrX, this.mrX.location());
				moves.addAll(mrXSingleMoves);
				if(this.mrX.has(ScotlandYard.Ticket.DOUBLE) && setup.rounds.size() - log.size() > 1) {
					moves.addAll(getDoubleMoves(setup, detectives, mrX, mrXSingleMoves));
				}
			} else{
				for (Player p : detectives) {
					if(remaining.contains(p.piece())) moves.addAll(getSingleMoves(this.setup, this.detectives, p, p.location()));
				}
			}
			return ImmutableSet.copyOf(moves);


		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			if(!this.getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: "+move);

			Set<Piece> newRemaining = new HashSet<Piece>();

			if(move.commencedBy() == mrX.piece()){
				log = updateLog(log, setup, move);
				mrX = updatePlayer(mrX, move);
			}
			else if(remaining.contains(mrX.piece())){
				newRemaining.add(mrX.piece());
			}

			List<Player> newDetectives = new ArrayList<Player>();

			for (Player p : detectives) {
				if (p.piece().equals(move.commencedBy())) {
					p = updatePlayer(p, move);
					mrX = mrX.give(move.tickets());
				}
				else if(remaining.contains(p.piece())){
					newRemaining.add(p.piece());
				}
				newDetectives.add(p);
			}

			return new MyGameState(setup, ImmutableSet.copyOf(newRemaining), log, mrX, newDetectives);
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

			if(this.remaining.isEmpty()) this.remaining = fillRemaining(this.remaining, this.mrX, this.detectives);

			if(!remaining.contains(mrX.piece()) && getSingleMoves(this.setup, this.detectives, mrX, mrX.location()).isEmpty()){
				winner = ImmutableSet.copyOf(getPieces(detectives));

			} else if(cannotMove(detectives, detectives, setup)){
				winner = ImmutableSet.of(mrX.piece());
				this.remaining = ImmutableSet.of();
			}

			//All rounds have been played
			if(log.size() == setup.rounds.size() && remaining.isEmpty()) {
				winner = ImmutableSet.of(mrX.piece());
				this.remaining = ImmutableSet.of();
			}

			//mrX cannot move and all players have played
			if(getSingleMoves(this.setup, this.detectives, mrX, mrX.location()).isEmpty() && remaining.size() == 0){
				winner = ImmutableSet.copyOf(getPieces(detectives));
				this.remaining = ImmutableSet.of();
			}

			for(Player p : detectives) {
				if (p.location() == mrX.location()) {
					winner = getPieces(detectives);
					this.remaining = ImmutableSet.of();
					break;
				}
			}


			if(!this.remaining.isEmpty() && getAvailableMoves().isEmpty() && !remaining.contains(mrX.piece())){
				this.remaining = fillRemaining(this.remaining, this.mrX, this.detectives);
			}


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