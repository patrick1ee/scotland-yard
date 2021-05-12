package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.glassfish.grizzly.utils.ArraySet;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

public class Action implements Comparable<Action> {
    private Move move;
    private State nextState;

    public int getScore() {return nextState.getScore();}

    @Override
    public int compareTo(Action that) {
        return (this.nextState.compareTo(that.nextState));
    }

    public Move getMove(){
        return this.move;
    }

    public State getNextState(){
        return this.nextState;
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

    public Action(State currentState, Move move){
        this.move = move;
        if(move.commencedBy().isMrX()) nextState = new State(currentState.advance(move), ActionSet.getDestination(move));
        else nextState = new State(currentState.advance(move), currentState.getAgentLocation());
    }

}

