package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

public class Action implements Comparable<Action> {
    private Move move;
    private AiState nextAiState;

    public int getScore() {return nextAiState.getScore();}

    @Override
    public int compareTo(Action that) {
        return (this.nextAiState.compareTo(that.nextAiState));
    }

    public Move getMove(){
        return this.move;
    }

    public AiState getNextState(){
        return this.nextAiState;
    }

    public int getDestination(){
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

    public Action(AiState currentAiState, Move move){
        this.move = move;
        if(move.commencedBy().isMrX()) nextAiState = new AiState(currentAiState.advance(move), this.getDestination());
        else nextAiState = new AiState(currentAiState.advance(move), currentAiState.getAgentLocation());
    }

}

