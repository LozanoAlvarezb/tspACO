import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.field.network.Network;
import sim.util.Double2D;

import java.util.ArrayList;

public class Sim extends SimState {

    // Info for drawing frames
    public static double XMIN = -2, XMAX = 800;
    public static double YMIN = -2, YMAX = 600;
    public static double ROWS,COLS;
    public static double NODE_DIAMETER = 8;

    // World structures
    public Continuous2D environment = null;
    public Network graph = null;
    public Network bestTravel = null;
    public double bestTravelDistance = 0;

    // Ants and Edges
    private ArrayList<Ant> ants = null;
    private ArrayList<PathEdge> edges = null;

    // Parameters
    private int nAnts = 131;
    private double history = 5;
    private double heuristic = 5;
    private double decayFactor = 0.1;
    private double pheromoneChangeFactor = 1;
    private double initialPheromone = 1.0;

    // Model
    private String model = "MMAX"; // Default parameter. Models = {"AS","EAS","ASrank","MMAS"}
    private double pheromoneMax = 10;
    private double pheromoneMin = 1.7;
    private double elitist = 1;

    // Start node
    private GraphNode start = null;

    // Iterations
    private int iterations = 1;

    // Getters
    public int getnAnts(){return nAnts;}

    public double getHistory(){return history;}

    public double getHeuristic(){return heuristic;}

    public double getInitialPheromone(){return initialPheromone;}

    public double getPheromoneMax(){return pheromoneMax;}

    public double getPheromoneMin(){return pheromoneMin;}


    // Setters
    public void setStart(GraphNode node){
        start = node;
        Ant.setStartNode(node);
    }

    // Constructor
    public Sim(long seed){super(seed);}

    // Create node and add it to the world structures
    private GraphNode makeNode(int[] pos)
    {
        GraphNode node = new GraphNode(pos);

        environment.setObjectLocation(node,
                new Double2D(((XMAX-XMIN) / (COLS +1) * pos[0] )+4/*x*/,
                        ((YMAX-YMIN) / (ROWS+1) * pos[1])+4/*y*/));
        graph.addNode(node);

        return node;
    }

    // Create edge and add it to the world structures
    private void makeEdge(GraphNode n1, GraphNode n2, double distance)
    {
        PathEdge newEdge=new PathEdge(n1,n2,getInitialPheromone(),distance);
        graph.addEdge(newEdge);
        edges.add(newEdge);
    }

    // Distance between two nodes
    public double distance(GraphNode n1, GraphNode n2){
        int[] a = n1.getPos(); int[] b = n2.getPos();
        return Math.sqrt(Math.pow(a[0]-b[0],2) + Math.pow(a[1]-b[1],2));
    }

    public void start(){
        super.start();  // clear out the schedule

        // load the graph from a file
        ArrayList<int []> nodesPos = GraphLoad.loadFromFile("/home/borja_lozano/Projects/Multiagent/mason_tsp/data/xqf131.tsp");

        // setup the world
        environment = new Continuous2D(nodesPos.size(),XMAX-(XMIN),YMAX-(YMIN));
        graph = new Network();
        ants = new ArrayList<>();
        edges = new ArrayList<>();

        for (int[] pos: nodesPos)
        {
            if(pos[0]>COLS)COLS=pos[0]+4;
            if(pos[1]>ROWS)ROWS=pos[0]+4;
        }

        boolean started = false;
        // create the nodes and edges
        for(int[] pos:nodesPos)
        {
            GraphNode node = makeNode(pos);

            if(!started)
            {
                setStart(node);
                started=true;
            }

            for(Object _node: graph.getAllNodes())
                makeEdge(node,(GraphNode)_node,distance(node,(GraphNode)_node));
        }


        int i;
        for(i=0;i<nAnts;i++)
        {
            Ant ant = new Ant();
            ants.add(ant);
            schedule.scheduleRepeating(ant);
        }


    }

    public void registerCompletion(){

        for(Ant ant:ants)
            if(!ant.isDone())
                return;

        Ant bestAnt = ants.get(0);

        for (Ant ant:ants)
            if(bestAnt.getacumulatedDistance() > ant.getacumulatedDistance())
                bestAnt=ant;

        System.out.println("Local min travel: " + bestAnt.getacumulatedDistance());
        if(bestTravelDistance==0 || bestAnt.getacumulatedDistance()<bestTravelDistance) {
            bestTravelDistance = bestAnt.getacumulatedDistance();
            bestTravel = new Network();
            for(PathEdge e:bestAnt.getCurrentPath())
                bestTravel.addEdge(new PathEdge((GraphNode)e.getFrom(),(GraphNode)e.getTo(),e.getPheromone(),e.getLength()));

            System.out.println("New global min travel: " + bestTravelDistance);
        }

        switch (model)
        {
            // Ant System
            case "AS":
                // update pheromone of each edge
                for (PathEdge edge:edges)
                {
                    double deltaPheromone = 0;
                    for (Ant ant:ants)
                        if (ant.pathContainsEdge(edge))
                            deltaPheromone += (pheromoneChangeFactor/
                                    ant.getacumulatedDistance());

                    double newPheromone = (1-decayFactor)*edge.getPheromone()+
                            deltaPheromone;
                    edge.setPheromone(newPheromone);
                }
                break;

            // Elitist Ant System
            case "EAS":
                // update pheromone of each edge
                for(PathEdge edge:edges)
                {
                    double deltaPheromone = 0;
                    if(bestAnt.pathContainsEdge(edge))
                        deltaPheromone += decayFactor * bestAnt.getacumulatedDistance();
                    for(Ant ant:ants)
                        if(ant.pathContainsEdge(edge))
                            deltaPheromone += (pheromoneChangeFactor/
                                    ant.getacumulatedDistance());

                    double newPheromone = (1-decayFactor)*edge.getPheromone()+
                            deltaPheromone;
                    edge.setPheromone(newPheromone);
                }
                break;

            // Min-Max
            case "MMAX":
                // update pheromone of each edge
                for(PathEdge edge:edges)
                {
                    double deltaPheromone = 0;
                    if(bestAnt.pathContainsEdge(edge))
                        deltaPheromone += (1/ bestAnt.getacumulatedDistance());

                    double newPheromone = (1-decayFactor)*edge.getPheromone()+
                            deltaPheromone;

                    if (newPheromone > getPheromoneMax())
                        newPheromone = getPheromoneMin();
                    else if (newPheromone < getPheromoneMin())
                        newPheromone = getPheromoneMin();

                    edge.setPheromone(newPheromone);
                }

        }
        System.out.println("End of iteration " + iterations); iterations++;
        System.out.println();

        // Reset ants for the next iteration
        for (Ant ant:ants)
            ant.reset();
    }

    // Main
    public static void main(String[] args){
        doLoop(Sim.class,args);
        System.exit(0);
    }

}
