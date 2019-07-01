import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import java.util.ArrayList;

public class Ant implements Steppable
{
    public static GraphNode startNode = null;
    public static GraphNode endNode = null;

    private ArrayList<PathEdge> currentPath = new ArrayList<>();
    private ArrayList<GraphNode> visitedNodes = new ArrayList<>();
    private double acumulatedDistance;
    private boolean done = false;
    private GraphNode currentNode = null;

    public boolean isDone(){return done;}
    public void setDone(boolean done) {this.done=done;}
    public static void setStartNode(GraphNode gn) {startNode = gn;}
    public static void setEndNode(GraphNode gn) {endNode = gn;}

    public double getacumulatedDistance(){return acumulatedDistance;}

    public ArrayList<PathEdge> getCurrentPath(){return currentPath;}

    public boolean pathContainsEdge(PathEdge e) {return currentPath.contains(e);}

    // Constructor
    public Ant()
    {
        reset();
    }

    // Get the node connected to the current node by and edge
    public GraphNode getEndNode(PathEdge e)
    {
        if(e.getFrom() == currentNode)
            return (GraphNode) (e.getTo());
        else
            return (GraphNode) (e.getFrom());
    }

    // Calculate the probability of following the edge
    private double calcProbHelper(double pheromone, double length,
                                  double historyCoefficient,
                                  double heuristicCoefficient)
    {
        return Math.pow(pheromone,historyCoefficient)*
                Math.pow(1/length,heuristicCoefficient);
    }

    // What the ant does at each step
    public void step(SimState state)
    {
        Bag feasibleEdges = new Bag();
        ((Sim) state).graph.getEdges(currentNode,feasibleEdges);
        ArrayList<PathEdge> okEdges = new ArrayList<>();

        if(endNode!=null && !visitedNodes.contains(endNode)) visitedNodes.add(endNode);


        // List of edges that lead to not visited neighbours
        for(Object o: feasibleEdges)
        {
            if(!visitedNodes.contains(getEndNode((PathEdge) o)))
                okEdges.add((PathEdge)o);
        }
//        if(endNode!=null) System.out.println("Feasible: " + feasibleEdges.size());
//        if(endNode!=null) System.out.println("Ok: " + okEdges.size());

        // No more neighbours to visit
        if(okEdges.size()==0)
        {
            for(Object o:feasibleEdges)
            {
                // Unless there is an error, there should always be a edge between the current and the start
                if(endNode== null &&startNode==getEndNode((PathEdge)o))
                {
                    acumulatedDistance += ((PathEdge) o).getLength();
                    setDone(true);
                    ((Sim)state).registerCompletion();
                    return;
                }
                else if (endNode==getEndNode((PathEdge)o))
                {
                    acumulatedDistance += ((PathEdge) o).getLength();
                    currentPath.add(new PathEdge(currentNode,endNode,0,0));
                    setDone(true);
                    ((Sim)state).registerCompletion();
                    return;
                }
            }
            reset();
            return;
        }
        else // Decide which edge to follow
        {
            double totalPheromone = 0;
            for (PathEdge pe:okEdges)
                totalPheromone += calcProbHelper(pe.getPheromone(), pe.getLength(),
                        ((Sim)state).getHistory(),
                        ((Sim)state).getHeuristic());

            double chance = ((Sim)state).random.nextDouble()*totalPheromone;

            double lastBorder=0;
            double nextBorder=0;
            for (PathEdge pe:okEdges) {
                lastBorder = nextBorder;
                nextBorder += calcProbHelper(pe.getPheromone(), pe.getLength(),
                        ((Sim) state).getHistory(),
                        ((Sim) state).getHeuristic());

                if (chance >= lastBorder && chance <= nextBorder) {
                    currentPath.add(pe);
                    acumulatedDistance += pe.getLength();
                    GraphNode nextNode = getEndNode(pe);
                    currentNode = nextNode;
                    visitedNodes.add(nextNode);
                    return;
                }
            }
        }

        System.out.println("ERROR");
    }

    // Reset ant
    public void reset()
    {
        setDone(false);
        currentPath.clear();
        visitedNodes.clear();
        acumulatedDistance = 0;
        currentNode = startNode;
        visitedNodes.add(currentNode);
    }
}
