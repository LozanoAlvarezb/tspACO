import sim.field.network.Edge;

public class PathEdge extends Edge {
    double pheromone = 0;
    double edgeLength = 1.0;

    public PathEdge(GraphNode gn1, GraphNode gn2, double p, double d) {
        super(gn1, gn2, "" + 1.0);
        setPheromone(p);
        edgeLength=d;
    }

    public double getPheromone() {
        return pheromone;
    }

    public void setPheromone(double d) {
        pheromone = d;
        setInfo("" + String.format("%.3f", pheromone));
    }


    public double getLength() {
        return edgeLength;
    }
}
