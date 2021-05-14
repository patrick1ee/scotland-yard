package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

/**
 * Represents action which can be made by an AI
 * Relates one-to-one to a ScotlandYard Move
 */

public class Action implements Comparable<Action> {
    private Move move;
    private AiState nextAiState;

    /**
     * Returns score of the state that this action leads to
     */
    public int getScore() {return nextAiState.getScore();}

    /**
     * @param that (Move to compare to)
     * @return Integer
     */
    @Override
    public int compareTo(Action that) {
        return (this.nextAiState.compareTo(that.nextAiState));
    }


    /**
     * @return move that this action represents
     */
    public Move getMove(){
        return this.move;
    }

    /**
     * @return state that this action leads to
     */
    public AiState getNextState(){
        return this.nextAiState;
    }

    /**
     * Visitor pattern
     * @return destination of the move that this action represents (single or double)
     */
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

    /**
     * @param currentAiState
     * @param move
     * Initialises the resultant state from this action and passes a new location if the relating move is commenced by MrX
     */
    public Action(AiState currentAiState, Move move){
        this.move = move;
        if(move.commencedBy().isMrX()) nextAiState = new AiState(currentAiState.advance(move), this.getDestination());
        else nextAiState = new AiState(currentAiState.advance(move), currentAiState.getAgentLocation());
    }

}

