package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

public class Search {


    static int getIndexOfSmallest(Dictionary<Integer, Integer> d, Set<Integer> keys){
        int min = 1000;
        int key = 0;
        for(int i : keys){
            if(d.get(i) < 1000){
                min = d.get(i);
                key = i;
            }
        }
        return key;
    }

    static ImmutableList<Integer> getDetectiveLocations(Board board){
        List<Integer> detectiveLocations = new ArrayList<>();
        for(Piece p : board.getPlayers()){
            if(p.isDetective()){
                detectiveLocations.add(board.getDetectiveLocation((Piece.Detective)p).get());
            }
        }
        return ImmutableList.copyOf(detectiveLocations);
    }

    static double getAverageDistanceFromDetectives(Board board, int location, ImmutableList<Integer> locations){
        List<Integer> distances = shortestDistances(board.getSetup().graph, location, locations);
        int tot = 0;
        for(int d : distances){
            tot += d;
        }
        return tot / distances.size();
    }

    /** A* algorithm for shortest distance between two points **/
    static List<Integer> shortestPathFromSourceToDestination(
            ImmutableValueGraph<Integer, Integer> graph,
            Integer source,
            Integer destination) {

        Comparator<Integer> displacementComparator = (source1, source2) -> (destination - source2) - (destination - source1);
        PriorityQueue<Integer> openSet = new PriorityQueue<>(displacementComparator);
        openSet.add(source);

        Map<Integer, Integer> cameFrom = new TreeMap<Integer, Integer>();
        Map<Integer, Integer> gScore = new TreeMap<Integer, Integer>();
        gScore.put(source, 0);

        Map<Integer, Integer> fScore = new TreeMap<Integer, Integer>();
        fScore.putIfAbsent(source, destination - source); //h(source) = destination - source :: Heuristic function

        while (openSet.size() > 0) {
            int current = openSet.remove();
            if (current == destination) {
                List<Integer> total_path = new ArrayList<Integer>();
                total_path.add(current);
                while (cameFrom.containsKey(current)) {
                    current = cameFrom.get(current);
                    total_path.add(current);

                }
                return Lists.reverse(total_path);
            }

            for (int succ_node : graph.successors(current)) {
                int tentative_gScore = gScore.getOrDefault(current, Integer.MAX_VALUE) + graph.edgeValueOrDefault(current, succ_node, null);
                if (tentative_gScore < gScore.getOrDefault(succ_node, Integer.MAX_VALUE)) {
                    cameFrom.putIfAbsent(succ_node, current);
                    gScore.put(succ_node, tentative_gScore);
                    fScore.put(succ_node, gScore.get(succ_node) + (destination - succ_node));
                    if (!openSet.contains(succ_node)) openSet.add(succ_node);
                }
            }
        }
        return null;

    }

    /** Dijkstras for shortest distances between source and given set of destinations **/
    static List<Integer> shortestDistances(
            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
            Integer source,
            ImmutableList<Integer> destinations){

        Set<Integer> nodes = new HashSet<>(graph.nodes());
        Dictionary<Integer, Integer> d = new Hashtable<Integer, Integer>();
        List<Integer> distances = new ArrayList<>();
        for(int n : nodes){
            d.put(n, 1000);
        }
        d.put(source, 0);
        while(!nodes.isEmpty()){
            int u = getIndexOfSmallest(d, nodes);
            nodes.remove(u);
            for(int v: graph.adjacentNodes(u)){
                int alt = d.get(u) + 1; /** Default value, consider how costly it is to use required ticket */
                if(alt < d.get(v)) d.put(v, alt);
            }
            if(destinations.contains(u)) distances.add(d.get(u));
        }
        //System.out.println(distances);
        return distances;
    }

    static Move[] Merge(Board board, Move[] left, Move[] right){
        Move[] sorted = new Move[left.length + right.length];
        int lcount = 0, rcount = 0;
        while(lcount + rcount < sorted.length){
            if(lcount >= left.length){
                sorted[lcount + rcount] = right[rcount];
                rcount += 1;
            } else if (rcount >= right.length){
                sorted[lcount + rcount] = left[lcount];
                lcount += 1;
            } else{
                boolean MrX = board.getAvailableMoves().stream().anyMatch(move -> move.commencedBy().isMrX());
                double lScore = MyAi.score(board, MyAi.getMoveDestination(left[lcount]), MrX);
                double rScore = MyAi.score(board, MyAi.getMoveDestination(right[rcount]), MrX);
                if(MrX){
                    if(lScore >= rScore){
                        sorted[lcount + rcount] = left[lcount];
                        lcount += 1;
                    } else{
                        sorted[lcount + rcount] = right[rcount];
                        rcount += 1;
                    }
                } else{
                    if(lScore <= rScore){
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
   static Move[] mergeSort(Board board, Move[] moves){
        if(moves.length < 2) return moves;

        Move[] left = mergeSort(board, Arrays.copyOfRange(moves, 0, (int) Math.floor(moves.length / 2)));
        Move[] right = mergeSort(board, Arrays.copyOfRange(moves, (int) Math.floor(moves.length / 2), moves.length));

        return Merge(board, left, right);
    }
}