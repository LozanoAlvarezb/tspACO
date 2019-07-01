import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Double2D;

import javax.vecmath.Tuple2d;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class Sim extends SimState {

    // File
    private String file = "data/xit1083";

    // Info for drawing frames
    public static double XMIN = -2, XMAX = 800;
    public static double YMIN = -2, YMAX = 600;
    public static int ROWS,COLS;
    public static double NODE_DIAMETER = 8;
    public static boolean main=true;

    // World structures
    public Continuous2D environment = null;
    public Network graph = null;
    public Network bestTravel = null;
    public double bestTravelDistance = 0;
    private ArrayList<PathEdge> bestPath = null;
    private ArrayList<ArrayList<PathEdge>> intersections = new ArrayList<>();


    // Ants and Edges
    private ArrayList<Ant> ants = null;
    private ArrayList<PathEdge> edges = null;

    // Parameters
    private int nAnts = 1083;
    private double history = 1;
    private double heuristic = 5;
    private double decayFactor = 0.02;
    private double pheromoneChangeFactor = 131;
    private double initialPheromone = 1.0;
    private int maxIter = 10;

    // Model
    private String model = "MMAS"; // Models = {"AS","EAS","ASrank","MMAS"}
    private double pheromoneMax = 10;
    private double pheromoneMin = 1.7;
    private int nRank = 3;
    private double elitist = 1;

    // Start node
    private GraphNode allStar = null;
    private GraphNode start = null;
    private GraphNode end = null;

    // Iterations and mode
    private int iterations = 0;
    private int localiter = 0;
    private int totalLocaliter = 0;
    private boolean lock = false;

    // Permutations for brute force
    private ArrayList<int[]> perm = new ArrayList<>();
    private int indexmin,indexmax; // Index of the smallest and biggest intersections
    private double bestLocalDistance=0;



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

    public void setEnd(GraphNode node){
        end = node;
        Ant.setEndNode(node);
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

    private void pathIntersect()
    {
        ArrayList<ArrayList<PathEdge>> aux = new ArrayList<>();

        for(PathEdge E:bestPath)
            for(PathEdge e:bestPath){
                if(E==e)continue;
                if(intersect(E,e)){
                    if(aux.contains(new ArrayList<PathEdge>(Arrays.asList(e,E)))) continue;
                    aux.add(new ArrayList<PathEdge>(Arrays.asList(E,e)));
                }
            }

        if(!intersections.isEmpty() && aux.contains(intersections.get(indexmax))) aux.remove(intersections.get(indexmax));
        intersections.clear();intersections=aux;

    }

    private  void permute(int[] arr){
        if(arr.length > 8)System.out.println("Brhu");
        perm.clear();
        permuteHelper(arr, 0);
    }

    private  void permuteHelper(int[] arr, int index){
        if(index >= arr.length - 1){ //If we are at the last element - nothing left to permute
//            System.out.println(Arrays.toString(arr));
            perm.add(arr.clone());
            return;
        }

        for(int i = index; i < arr.length; i++){ //For each index in the sub array arr[index...end]

            //Swap the elements at indices index and i
            int t = arr[index];
            arr[index] = arr[i];
            arr[i] = t;

            //Recurse on the sub array arr[index+1...end]
            permuteHelper(arr, index+1);

            //Swap the elements back
            t = arr[index];
            arr[index] = arr[i];
            arr[i] = t;
        }
    }


    // Return the smallest set of nodes affected by an intersection
    private ArrayList<ArrayList<GraphNode>> nodesAffected()
    {
        ArrayList<GraphNode> nodes;
        ArrayList<ArrayList<GraphNode>> internodes = new ArrayList<>();
        ArrayList<GraphNode> smallest = new ArrayList<>();
        ArrayList<GraphNode> biggest = new ArrayList<>();


        for(ArrayList<PathEdge> inter:intersections)
        {
            double distance = 0;
            nodes = new ArrayList<>();
            int i=0;
            GraphNode n1,n2=null;

            boolean aff = false;
            for (PathEdge edge: bestPath) {
                if(i==0){
                    if(edge.getFrom()==start){n1= (GraphNode) edge.getFrom();n2= (GraphNode) edge.getTo();}
                    else{n1= (GraphNode) edge.getTo();n2= (GraphNode) edge.getFrom();}
                }
                else
                {
                    if(((GraphNode) edge.getFrom()).getPos()==n2.getPos()){n1= (GraphNode) edge.getFrom();n2= (GraphNode) edge.getTo();}
                    else{n1= (GraphNode) edge.getTo();n2= (GraphNode) edge.getFrom();}
                }

                if(edge==inter.get(0))aff=true;
                if(aff)
                {
                    nodes.add(n1);
                    distance+=edge.getLength();
                }
                if(edge==inter.get(1)){
                    nodes.add(n2);
                    break;
                }
                i++;
            }
            if(smallest.isEmpty() || smallest.size()>nodes.size()) {
                indexmin=intersections.indexOf(inter);
                smallest = new ArrayList<>(nodes);
            }
            if((biggest.isEmpty() || (biggest.size()<nodes.size())) && nodes.size()<50)
            {
                indexmax=intersections.indexOf(inter);
                bestLocalDistance=distance;
                biggest = new ArrayList<>(nodes);
            }
            }

        internodes.add(smallest);
        internodes.add(biggest);
        return internodes;

    }

    // Print the best path yet
    private void printPath()
    {
        int i=0;
        GraphNode n1,n2=null;

        for (PathEdge edge: bestPath) {
            if(i==0){
                if(edge.getFrom()==allStar){n1= (GraphNode) edge.getFrom();n2= (GraphNode) edge.getTo();}
                else{n1= (GraphNode) edge.getTo();n2= (GraphNode) edge.getFrom();}
            }
            else
            {
                if(((GraphNode) edge.getFrom()).getPos()==n2.getPos()){n1= (GraphNode) edge.getFrom();n2= (GraphNode) edge.getTo();}
                else{n1= (GraphNode) edge.getTo();n2= (GraphNode) edge.getFrom();}
            }

            System.out.println(n1.getPos()[0] +"-"+ n1.getPos()[1]);
            i++;
        }
        System.out.println();

    }

    // Print intersections
    private void printInter()
    {
        for(ArrayList<PathEdge> edges:intersections){
            GraphNode n1 = (GraphNode) edges.get(0).getFrom(); GraphNode n2 = (GraphNode) edges.get(0).getTo();
            GraphNode n3 = (GraphNode) edges.get(1).getFrom(); GraphNode n4 = (GraphNode) edges.get(1).getTo();
            System.out.print("Intersecction found between edges [" + n1.getPos()[0] +"-"+ n1.getPos()[1] + " : " + n2.getPos()[0] +"-"+ n2.getPos()[1] + "]");
            System.out.println(" and " + " [" + n3.getPos()[0] +"-"+ n3.getPos()[1] + " : " + n4.getPos()[0] +"-"+ n4.getPos()[1]  + "]");
        }
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

    // Clockwise direction of 3 points
    public boolean clockw(GraphNode n1, GraphNode n2, GraphNode n3)
    {
        return((((n2.getPos()[0]-n1.getPos()[0])*(n3.getPos()[1]-n1.getPos()[1]))-((n2.getPos()[1]-n1.getPos()[1])*(n3.getPos()[0]-n1.getPos()[0])))/2.0)>0;
    }

    //  Edge intersect
    public boolean intersect(PathEdge e1, PathEdge e2)
    {
        GraphNode n1 = (GraphNode) e1.getFrom(); GraphNode n2 = (GraphNode) e1.getTo();
        GraphNode n3 = (GraphNode) e2.getFrom(); GraphNode n4 = (GraphNode) e2.getTo();
        if(n1 == n3 || n1 == n4 || n2 == n3 || n2 == n4) return false;
        else return (clockw(n1,n3,n2) ^ clockw(n1,n4,n2)) && (clockw(n3,n1,n4) ^ clockw(n3,n2,n4));
    }

    private void bruteForce(ArrayList<GraphNode> nodes)
    {
        double min = -1;
        int[] a = new int[nodes.size()-2];
        for (int i = 0; i < nodes.size()-2; ++i) {
            a[i] = i+1;
        }
        permute(a);
//        System.out.println("Number of perm: "+a.length);


        for(int[] permutation:perm)
        {
//            System.out.println("Perm: "+Arrays.toString(permutation));
            double distance = distance(nodes.get(0),nodes.get(permutation[0]));
            for(int i=0;i<permutation.length-1;i++)
            {
                distance+=distance(nodes.get(permutation[i]),nodes.get(permutation[i+1]));
            }
            distance+=distance(nodes.get(nodes.size()-1),nodes.get(permutation[permutation.length-1]));
            if(min<0 || distance<min){
                min=distance;
                a=permutation;
            }
        }

//        System.out.println("Min distance of the permutations: " + Arrays.toString(a));

        // Set new travel
        boolean local = false;
        int i=0;


        ArrayList<PathEdge> newBestPath = new ArrayList<PathEdge>();

        double distance = 0;

        for(PathEdge edge:bestPath){
            // The first edge starts the loop and the last one stops it
            if(intersections.get(indexmin).contains(edge)) {
                if(!local)newBestPath.add(new PathEdge(nodes.get(0), nodes.get(a[0]), getInitialPheromone(), distance(nodes.get(0), nodes.get(a[0]))));
                else newBestPath.add(new PathEdge(nodes.get(a[a.length-1]), nodes.get(nodes.size()-1), getInitialPheromone(), distance(nodes.get(a[a.length-1]), nodes.get(nodes.size()-1))));
                local=!local;
            }

            else {
                if (local) {
                    newBestPath.add(new PathEdge(nodes.get(a[i]), nodes.get(a[i + 1]), getInitialPheromone(), distance(nodes.get(a[i]), nodes.get(a[i + 1]))));
                    i++;
                } else
                    newBestPath.add(new PathEdge((GraphNode) edge.getFrom(), (GraphNode) edge.getTo(), edge.getPheromone(), edge.getLength()));

                distance += newBestPath.get(newBestPath.size() - 1).getLength();
            }
        }

        bestTravelDistance = distance;

        bestTravel = new Network();

        bestPath=newBestPath;
        for(PathEdge e:bestPath)
            bestTravel.addEdge(new PathEdge((GraphNode)e.getFrom(),(GraphNode)e.getTo(),e.getPheromone(),e.getLength()));
    }


    public void start(){
        super.start();  // clear out the schedule

        // load the graph from a file
        ArrayList<int []> nodesPos = GraphLoad.loadFromFile(file);

        // setup the world
        environment = new Continuous2D(nodesPos.size(),XMAX-(XMIN),YMAX-(YMIN));
        graph = new Network();
        ants = new ArrayList<>();
        edges = new ArrayList<>();

        for (int[] pos: nodesPos)
        {
            if(pos[0]>COLS)COLS=pos[0];
            if(pos[1]>ROWS)ROWS=pos[1];
        }

        COLS=COLS+4;ROWS=ROWS+4;

//        System.out.println(ROWS);
//        System.out.println(COLS);
        boolean started = false;
        // create the nodes and edges
        for(int[] pos:nodesPos)
        {
            GraphNode node = makeNode(pos);

            if(!started)
            {
                setStart(node);
                allStar=node;
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

//        if(end!=null)System.out.println("Checking if done");
        for(Ant ant:ants)
            if(!ant.isDone())
                return;

        Collections.sort(ants, Comparator.comparingDouble(Ant::getacumulatedDistance));


        Ant bestAnt = ants.get(0);

        if(localiter>=10)
        {
            end=null;
        }

        // First n iterations
        if(iterations<maxIter){
            System.out.println("Local min travel: " + bestAnt.getacumulatedDistance());
            if(bestTravelDistance==0 || bestAnt.getacumulatedDistance()<bestTravelDistance) {
                bestPath = new ArrayList<>(bestAnt.getCurrentPath());
                bestTravelDistance = bestAnt.getacumulatedDistance();
                bestTravel = new Network(); // Network to be represented
                for(PathEdge e:bestAnt.getCurrentPath())
                    bestTravel.addEdge(new PathEdge((GraphNode)e.getFrom(),(GraphNode)e.getTo(),e.getPheromone(),e.getLength()));

                System.out.println("New global min travel: " + bestTravelDistance);
//                printPath();// Print the best path
            }
        }
        else if(end==null)
        {
            main = false;
//            System.out.println("Final best travel:");
//            printPath();// Print the best path

            // Get all intersection in the best path
            pathIntersect();
            ArrayList<ArrayList<GraphNode>> internodes =nodesAffected();
            ArrayList<GraphNode> nodes = internodes.get(0);
            ArrayList<GraphNode> nodesBig = internodes.get(1);


            System.out.println("Number of intersections: " + intersections.size());
            if(intersections.size()==0){
                super.finish();
                return;
            }


            // If we have already search enough for smaller tsp inside the bigger graph or the biggest inter is >50
            if(nodesBig.size()<10 || totalLocaliter==100)
            {
                System.out.println();System.out.println();
                System.out.println("Brute forcing intersections smaller than 10");
                System.out.println("--------------------------------------------");
                // While there is a intersect that affects a small number of nodes -> Brute force
                while(nodes.size()<10)
                {
                    bruteForce(nodes);

                    System.out.println();
                    System.out.println("New global min travel: "+ bestTravelDistance);

                    pathIntersect();
                    System.out.println("New number of intersections: " + intersections.size());
                    if(intersections.size()==0){
                        super.finish();
                        return;
                    }
                    internodes.clear();
                    nodes.clear();
                    internodes =nodesAffected();
                    nodes = internodes.get(0);
                    System.out.println("Size of the smallest intersection: " + nodes.size());
                }

                super.finish();
                return;
            }
            // Setup the new iteration of ants
            else
            {
                System.out.println();System.out.println();
                System.out.println("Setting up the new local iteration");
                System.out.println("-----------------------------------");
                System.out.println("Init Local distance: " + bestLocalDistance);
                System.out.println("Number of nodes afected: " + nodesBig.size());
                System.out.println();


                localiter = 0;
                graph = new Network();
                edges = new ArrayList<>();


                // create the nodes and edges
                for(GraphNode nodex:nodesBig)
                {
                    GraphNode node = makeNode(nodex.getPos());
                    if(nodesBig.indexOf(nodex)==0)setStart(node);
                    if(nodesBig.indexOf(nodex)==nodesBig.size()-1)setEnd(node);
                    for(Object _node: graph.getAllNodes())
                        makeEdge(node,(GraphNode)_node,distance(node,(GraphNode)_node));
                }

                // Reset ants for the next iteration
                for (Ant ant:ants)
                    ant.reset();

                totalLocaliter++;

                return;
            }


        }
        else{

            if(bestAnt.getacumulatedDistance()<bestLocalDistance){
                System.out.println("New global min travel: " + bestAnt.getacumulatedDistance());
                bestLocalDistance=bestAnt.getacumulatedDistance();
                int i = 0,j=0;
                boolean local = false;
                ArrayList<PathEdge> newBestPath = new ArrayList<PathEdge>();
                ArrayList<PathEdge> antPath = bestAnt.getCurrentPath();

                GraphNode n1,n2=null;

                for (PathEdge edge: bestPath) {
                    if(i==0){
                        if(edge.getFrom()==allStar){n1= (GraphNode) edge.getFrom();n2= (GraphNode) edge.getTo();}
                        else{n1= (GraphNode) edge.getTo();n2= (GraphNode) edge.getFrom();}
                    }
                    else
                    {
                        if(((GraphNode) edge.getFrom()).getPos()==n2.getPos()){n1= (GraphNode) edge.getFrom();n2= (GraphNode) edge.getTo();}
                        else{n1= (GraphNode) edge.getTo();n2= (GraphNode) edge.getFrom();}
                    }

                    if(n1.getPos() == start.getPos())
                        local=true;

                    if(n1.getPos() == end.getPos())
                        local=false;

                    if(local) {
                        newBestPath.add((new PathEdge((GraphNode)antPath.get(j).getFrom(),(GraphNode)antPath.get(j).getTo(),antPath.get(j).getPheromone(),antPath.get(j).getLength())));
                        j++;
                    }
                    else newBestPath.add(new PathEdge((GraphNode) edge.getFrom(), (GraphNode) edge.getTo(), edge.getPheromone(), edge.getLength()));
                    i++;
                }
                System.out.println();
                bestTravel = new Network();

                bestPath=newBestPath;
                for(PathEdge e:bestPath)
                    bestTravel.addEdge(new PathEdge((GraphNode)e.getFrom(),(GraphNode)e.getTo(),e.getPheromone(),e.getLength()));

            }
            else System.out.println("Local min travel: " + bestAnt.getacumulatedDistance());
            System.out.println("End of local Iteration: " + localiter); localiter++;

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
                        deltaPheromone += elitist * (1/bestAnt.getacumulatedDistance());
                    for(Ant ant:ants) {
                        if (ant.pathContainsEdge(edge))
                            deltaPheromone += (pheromoneChangeFactor /
                                    ant.getacumulatedDistance());
                    }

                    double newPheromone = (1-decayFactor)*edge.getPheromone()+
                            deltaPheromone;
                    edge.setPheromone(newPheromone);
                }
                break;

            case "ASrank":
                // update pheromone of each edge
                for(PathEdge edge:edges)
                {
                    int i=0;
                    double deltaPheromone = 0;
                    if(bestAnt.pathContainsEdge(edge))
                        deltaPheromone += nRank * bestAnt.getacumulatedDistance();
                    for(Ant ant:ants) {
                        if (ant.pathContainsEdge(edge))
                            deltaPheromone += (1 /
                                    ant.getacumulatedDistance());
                        if(i>=nRank)
                            break;
                        i++;
                    }

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
