package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static uk.ac.bris.cs.scotlandyard.ui.ai.Search.shortestDistances;

public class State implements Comparable<State> {
    private Board.GameState gameState;
    private long key;
    private int score;
    private int agentLocation;
    private boolean maximizing;
    private boolean terminal;
    private ImmutableList<Move> availableMoves;

    private List<Integer> getDetectiveLocations(Board board){
        List<Integer> detectiveLocations = new ArrayList<>();
        for(Piece p : board.getPlayers()){
            if(p.isDetective()){
                detectiveLocations.add(board.getDetectiveLocation((Piece.Detective)p).get());
            }
        }
        return detectiveLocations;
    }

    public long getUniqueKey(){return key;}
    public int getScore(){ return score; }
    public int getAgentLocation(){return agentLocation;}
    public ActionSet getActions() {return new ActionSet(this, gameState.getAvailableMoves().asList(), maximizing);}
    public Board advance(Move move){ return this.gameState.advance(move); }
    public boolean isMaximizing(){return this.maximizing;}
    public boolean isTerminal(){return this.terminal;}

    @Override
    public int compareTo(State that) {
        return (this.getScore() - that.getScore());
    }

    public State(Board board, int agentLocation){
        this.gameState = (Board.GameState) board;
        this.availableMoves = gameState.getAvailableMoves().asList();
        this.agentLocation = agentLocation;
        this.maximizing = availableMoves.stream().anyMatch(move -> move.commencedBy().isMrX());;
        List<Integer> detectiveLocations = getDetectiveLocations(board);

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
    }
}
