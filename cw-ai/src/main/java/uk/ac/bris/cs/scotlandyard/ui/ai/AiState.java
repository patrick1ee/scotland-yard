package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static uk.ac.bris.cs.scotlandyard.ui.ai.Search.shortestDistances;

/**
 * Data structure representing a state that the game may be in
 * Contains more information useful to the AI agent
 */
public  class AiState implements Comparable<AiState> {
    private final Board.GameState gameState;
    private final long key;
    private final int score;
    private final int agentLocation;
    private final boolean maximizing;
    private final boolean terminal;
    private final ImmutableList<Move> availableMoves;

    /**
     * @param board
     * @returns list of all detective locations
     */
    private List<Integer> getDetectiveLocations(Board board){
        List<Integer> detectiveLocations = new ArrayList<>();
        for(Piece p : board.getPlayers()){
            if(p.isDetective()){
                detectiveLocations.add(board.getDetectiveLocation((Piece.Detective)p).get());
            }
        }
        return detectiveLocations;
    }

    /**
     * @return key, unique to board configuraiton (disregarding specific detectives), for use in hash table
     */
    public long getUniqueKey(){return key;}

    /**
     * @return score of this state
     */
    public int getScore(){ return score; }

    /**
     * @return location of AI agent on board (in this case MrX)
     */
    public int getAgentLocation(){return agentLocation;}

    /**
     * @return Action Set of actions available to agent in this state
     */
    public ActionSet getActions() {return new ActionSet(this, gameState.getAvailableMoves().asList(), maximizing);}

    /**
     * @param move
     * @return board object after being advanced by given move
     */
    public Board advance(Move move){ return this.gameState.advance(move); }

    /**
     * @return flag indicating if minimax should maximize the score for this state
     */
    public boolean isMaximizing(){return this.maximizing;}

    /**
     * @return flag indicating if this state is final state, i.e. the game is won or lost
     */
    public boolean isTerminal(){return this.terminal;}

    /**
     * @param that
     * @return comparison with other state based on score
     */
    @Override
    public int compareTo(AiState that) {
        return (this.getScore() - that.getScore());
    }

    public AiState(Board board, int agentLocation){
        this.gameState = (Board.GameState) board;
        this.availableMoves = gameState.getAvailableMoves().asList();
        this.agentLocation = agentLocation;
        this.maximizing = availableMoves.stream().anyMatch(move -> move.commencedBy().isMrX());;
        List<Integer> detectiveLocations = getDetectiveLocations(board);

        /**
         * Generates score for state
         */
        if(board.getWinner().contains(Piece.MrX.MRX)) score = 100;
        else if (board.getWinner().size() > 0) score = -100;
        else{
            List<Integer> distances = shortestDistances(board.getSetup().graph, agentLocation, detectiveLocations);
            int tot = 0;
            for(int d : distances){
                tot += d;
            }
            if(this.maximizing) score = (int)Math.floor(tot / distances.size()) + board.getAvailableMoves().size();
            else score = (int)Math.floor(tot / distances.size()) - board.getAvailableMoves().size();
        }


        /**
         * Generates hash key for state
         */
        Collections.sort(detectiveLocations);
        int nodeCount = board.getSetup().graph.nodes().size();
        long key = 0;
        for(int i : detectiveLocations){
            key *= nodeCount;
            key += i;
        }
        key *= nodeCount;
        key += agentLocation;
        this.key = key;

        this.terminal = board.getWinner().size() > 0;

        if(!this.maximizing && this.availableMoves.stream().anyMatch(move -> move.commencedBy().isMrX())) System.out.println("R");
    }
}
