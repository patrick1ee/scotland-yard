package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

/**
 * Iterable data structure, contains the set of available actions to the AI, automatically sorted based on
 * heuristic representing likelihood of a good score
 */

public class ActionSet implements Iterable<Action>{

    protected Action[] actions;

    /**
     * @param maximizing
     * @param left
     * @param right
     * @return Sorted array of actions (order depending on whether maximizing MrX or detectives
     */
    private Action[] Merge(boolean maximizing, Action[] left, Action[] right){
        Action[] sorted = new Action[left.length + right.length];
        int lcount = 0, rcount = 0;
        while(lcount + rcount < sorted.length){
            if(lcount >= left.length){
                sorted[lcount + rcount] = right[rcount];
                rcount += 1;
            } else if (rcount >= right.length){
                sorted[lcount + rcount] = left[lcount];
                lcount += 1;
            } else{
                if(maximizing){
                    if(left[lcount].compareTo(right[rcount]) >= 0){
                        sorted[lcount + rcount] = left[lcount];
                        lcount += 1;
                    } else{
                        sorted[lcount + rcount] = right[rcount];
                        rcount += 1;
                    }
                } else{
                    if(left[lcount].compareTo(right[rcount]) <= 0){
                        sorted[lcount + rcount] = left[lcount];
                        lcount += 1;
                    } else{
                        sorted[lcount + rcount] = right[rcount];
                        rcount += 1;
                    }
                }

            }

        }
        return sorted;
    }
    private Action[] mergeSort(boolean maximizing, Action[] actions){
        if(actions.length < 2) return actions;

        Action[] left = mergeSort(maximizing, Arrays.copyOfRange(actions, 0, (int) Math.floor(actions.length / 2)));
        Action[] right = mergeSort(maximizing, Arrays.copyOfRange(actions, (int) Math.floor(actions.length / 2), actions.length));

        return Merge(maximizing, left, right);
    }

    /**
     * @param move
     * @return cost of move calculated based on in-game frequency of tickets
     */
    private int moveCost(Move move){
        int cost = 0;
        for(ScotlandYard.Ticket t : move.tickets()){
            switch(t){
                case TAXI: cost += 1;
                case BUS: cost += 2;
                case UNDERGROUND: cost += 3;
                case SECRET: cost += 4;
                case DOUBLE: cost += 5;
            }
        }
        return cost;
    }

    /**
     * Visitor pattern
     * @return destination of a given move (single or double)
     */
    public static int getDestination(Move move){
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
     * @param moves
     * @return list of moves containing specific one move per destination
     */
    private  ImmutableList<Move> filterMoves(ImmutableList<Move> moves){
        if(moves.size() == 0) return ImmutableList.of();//throw new IllegalArgumentException("Moves array cannot be empty!");
        Dictionary<Integer, Integer> dict = new Hashtable<Integer, Integer>();
        Dictionary<Integer, Move> map = new Hashtable<Integer, Move>();
        List<Move> filtered = new ArrayList<>();
        for(Move m : moves){
            int dest = getDestination(m);
            if(dict.get(dest) == null){
                dict.put(dest, moveCost(m));
                map.put(dest, m);
            } else{
                int cost = moveCost(m);
                if(cost <= dict.get(dest)){
                    dict.put(dest, cost);
                    map.put(dest, m);
                }
            }
        }
        Enumeration keys = map.keys();
        while(keys.hasMoreElements()){
            filtered.add(map.get(keys.nextElement()));
        }
        //System.out.println(moves.size() + ", " + filtered.size());
        return ImmutableList.copyOf(filtered);
    }

    /**
     * @return iterator for this set of actions
     */
    @Override
    public Iterator<Action> iterator() {
        return new Iterator<Action>() {
            private int index = 0;
            @Override
            public boolean hasNext() {
                return (index < actions.length);
            }

            @Override
            public Action next() {
                Action action = actions[index];
                index += 1;
                return action;
            }
        };
    }

    public ActionSet(AiState aiState, ImmutableList<Move> moves, boolean maximizing){
        moves = filterMoves(moves);
        Action[] actions = new Action[moves.size()];
        for(int i = 0; i < moves.size(); i++){
            actions[i] = new Action(aiState, moves.get(i));
        }
        this.actions = mergeSort(maximizing, actions);
    }
}