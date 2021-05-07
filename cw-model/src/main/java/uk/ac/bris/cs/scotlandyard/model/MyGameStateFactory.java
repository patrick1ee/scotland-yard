package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;


public final class MyGameStateFactory implements Factory<GameState> {

	/**
	 *
	 * @param detectives
	 * @return boolean verifying that detectives all have correct properties
	 */
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

	/**
	 * @param location
	 * @param detectives
	 * @return boolean verifying if a particular location is unoccupied by a detective
	 */
	private static boolean unoccupied(int location, List<Player> detectives){
		for(Player p : detectives){
			if(p.location() == location) return false;
		}
		return true;
	}

	/**
	 *
	 * @param players
	 * @return Immutable Set of pieces which correspond to a given list of players
	 */
	private static ImmutableSet<Piece> getPieces(List<Player> players){
		Set<Piece> pieces = new HashSet<Piece>();
		for(Player p : players){
			pieces.add(p.piece());
		}
		return ImmutableSet.copyOf(pieces);
	}

	/**
	 *
	 * @param move
	 * @return Uses double dispatch to pattern to get all destinatotions reached in a given move
	 *
	 * VISITOR PATTERN
	 */
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

	private static Integer getDestination(Move move){
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

	/**
	 *
	 * @param log
	 * @param setup
	 * @param move
	 * @return Log updated after mrX move
	 */
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

	/**
	 *
	 * @param player
	 * @param move
	 * @return Player updated after given move
	 */
	private static Player updatePlayer(Player player, Move move){
		Player newPlayer = player.use(move.tickets());
		for(int dest : getDestinations(move)){
			newPlayer = newPlayer.at(dest);
		}
		return newPlayer;
	}

	/**
	 *
	 * @param setup
	 * @param detectives
	 * @param player
	 * @param source
	 * @param permitDoubleMove (Allows for use of recursion to create a set of double moves off the back of a single move)
	 * @return Set of all possible moves the given player can make
	 */
	private static ImmutableSet<Move> getPlayerMoves(GameSetup setup, List<Player> detectives, Player player, int source, boolean permitDoubleMove) {
		Set moves = new HashSet<Move>();
		for (int destination : setup.graph.adjacentNodes(source)) {
			if (unoccupied(destination, detectives)) {
				for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if(player.has(t.requiredTicket())){
						moves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));

						/** extends from given single move to include set of possible double moves from this **/
						if(permitDoubleMove){
							ImmutableSet<Move> doubles = getPlayerMoves(setup, detectives, player.use(t.requiredTicket()), destination, false);
							for(Move m : doubles){
								moves.add(new Move.DoubleMove(player.piece(), source, t.requiredTicket(), destination, m.tickets().iterator().next(), getDestination(m)));
							}
						}
					}
				}
				/** Same as in loop, but accounts for secret moves **/
				if(player.has(ScotlandYard.Ticket.SECRET)){
					moves.add(
							new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
					if(permitDoubleMove){
						ImmutableSet<Move> doubles = getPlayerMoves(setup, detectives, player.use(ScotlandYard.Ticket.SECRET), destination, false);
						for(Move m : doubles){
							moves.add(new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination, m.tickets().iterator().next(), getDestination(m)));
						}
					}
				}
			}
		}
		return ImmutableSet.copyOf(moves);
	}

	/**
	 *
	 * @param players
	 * @param detectives
	 * @param setup
	 * @return boolean indicating if all given players are stuck
	 */
	private  static boolean cannotMove(List<Player> players, List<Player>detectives, GameSetup setup){
		for(Player p : players){
			if(!getPlayerMoves(setup, detectives, p, p.location(), true).isEmpty()) return false;
		}
		return true;
	}



	private final class MyGameState implements GameState {

		/**
		 * Ticket board containing the tickets in play on the game board
		 */
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
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		/**
		 * Called by constructor to check for win and populate 'winner' set and adjust 'remaining' set appropriately
		 */
		private void checkWin(){
			if(getPlayerMoves(this.setup, this.detectives, mrX, mrX.location(),
					mrX.has(ScotlandYard.Ticket.DOUBLE) && setup.rounds.size() - log.size() > 1).isEmpty()) {
				winner = getPieces(detectives);
			}
			else if(!unoccupied(mrX.location(), detectives)){
				winner = getPieces(detectives);
				this.remaining = ImmutableSet.of();

			} else if(cannotMove(detectives, detectives, setup)){
				winner = ImmutableSet.of(mrX.piece());
				this.remaining = ImmutableSet.of();
			}

			//All rounds have been played
			if(log.size() == setup.rounds.size() && remaining.contains(mrX.piece())) {
				winner = ImmutableSet.of(mrX.piece());
				this.remaining = ImmutableSet.of();
			}
		}

		/**
		 * Iterates through each remaining player and gets the available moves for them
		 * Also ensures game isn't over if some detectives are stuck
		 */
		private void makeMoves(){
			Set<Move> moves = new HashSet<>();
			for(Player p : everyone){
				if(this.remaining.contains(p.piece())) {
					moves.addAll(getPlayerMoves(setup, detectives, p, p.location(),
							p.has(ScotlandYard.Ticket.DOUBLE) && setup.rounds.size() - log.size() > 1));
				}
			}
			if(moves.isEmpty() && !remaining.isEmpty() && winner.isEmpty()) {
				if (remaining.contains(mrX.piece())) {
					this.remaining = getPieces(detectives);
				} else {
					this.remaining = ImmutableSet.of(mrX.piece());
				}
			}
			for(Player p : everyone){
				if(this.remaining.contains(p.piece())) {
					moves.addAll(getPlayerMoves(setup, detectives, p, p.location(),
							p.has(ScotlandYard.Ticket.DOUBLE) && setup.rounds.size() - log.size() > 1));
				}
			}
			this.moves = ImmutableSet.copyOf(moves);
		}


		@Override
		public GameSetup getSetup() {
			return this.setup;
		}

		@Override
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> pieces = new HashSet<>();
			for(Player p: everyone){
				pieces.add(p.piece());
			}
			return ImmutableSet.copyOf(pieces);
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
			return this.moves;
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			if (!this.getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

			if (move.commencedBy() == mrX.piece()) {
				return new MyGameState(setup, getPieces(detectives), updateLog(log, setup, move), updatePlayer(mrX, move), detectives);
			}
			else
				{
				Set<Piece> newRemaining = new HashSet<Piece>();
				List<Player> newDetectives = new ArrayList<Player>();
				for (Player p : detectives) {
					if (p.piece().equals(move.commencedBy())) {
						p = updatePlayer(p, move);
						mrX = mrX.give(move.tickets());
					} else if (remaining.contains(p.piece())){
						newRemaining.add(p.piece());
					}
					newDetectives.add(p);
				}

				return new MyGameState(setup, ImmutableSet.copyOf(newRemaining), log, mrX, newDetectives);
			}
		}

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {

			this.setup = Objects.requireNonNull(setup, "setup must not be null");
			this.log = Objects.requireNonNull(log, "log must not be null");
			this.mrX = Objects.requireNonNull(mrX, "mrX must not be null");
			this.detectives = Objects.requireNonNull(detectives, "detectives must not be null");
			this.remaining = Objects.requireNonNull(remaining, "remaining must not be null");
			this.winner = ImmutableSet.of();

			if (this.setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty");
			if(this.setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty");
			if (!verifyDetectives(this.detectives)) throw new IllegalArgumentException("Invalid detectives");
			if (this.mrX.piece().webColour() != "#000") throw new IllegalArgumentException("MrX is not the black piece");

			List<Player> all = new ArrayList<>(detectives);
			all.add(mrX);
			this.everyone = ImmutableList.copyOf(all);

			if(remaining.isEmpty()) this.remaining = ImmutableSet.of(mrX.piece());

			this.checkWin();
			this.makeMoves();

		}
	}


	@Nonnull
	@Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

	}
}