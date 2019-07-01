import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.io.File;

import java.io.FileNotFoundException;


public class GraphLoad extends Throwable{

    public static ArrayList<int []> loadFromFile(String path)
    {
        ArrayList<int []> graph  = new ArrayList<>();
        int x,y;


        try {
            File F = new File(path);
            Scanner scan = new Scanner(F);

            while(scan.hasNext())
            {
                x=Integer.parseInt(scan.next());y=Integer.parseInt(scan.next());
                int[] point = new int[2];
                point[0]=x;point[1]=y;
                graph.add(point);
            }

        }catch (FileNotFoundException e){}

        return graph;
    }

    // Clockwise direction of 3 points
    private static boolean clock(GraphNode n1, GraphNode n2, GraphNode n3)
    {
        return((((n2.getPos()[0]-n1.getPos()[0])*(n3.getPos()[1]-n1.getPos()[1]))-((n2.getPos()[1]-n1.getPos()[1])*(n3.getPos()[0]-n1.getPos()[0])))/2.0)>0;
    }

    //  Edge intersect
    public boolean intersect(PathEdge e1, PathEdge e2)
    {
        GraphNode n1 = (GraphNode) e1.getFrom(); GraphNode n2 = (GraphNode) e1.getTo();
        GraphNode n3 = (GraphNode) e2.getFrom(); GraphNode n4 = (GraphNode) e2.getTo();
        if(n1 == n3 || n1 == n4 || n2 == n3 || n2 == n4) return false;
        else return (clock(n1,n3,n2) ^ clock(n1,n4,n2)) && (clock(n3,n1,n4) ^ clock(n3,n2,n4));
    }

    public static void main(String[] args){
//        for (int[] pos:
//        loadFromFile("/Users/borjalozanoalvarez/Projects/MASON/tspACO/data/xit1083")) {
//            System.out.println(pos[0]+ " : " + pos[1]);
//
//        }
        int[] pos1 = {1,5};int[] pos2 = {5,1};
        int[] pos3 = {1,1};int[] pos4 = {10,10};

        GraphNode n1 = new GraphNode(pos1); GraphNode n2 = new GraphNode(pos2);
        GraphNode n3 = new GraphNode(pos3); GraphNode n4 = new GraphNode(pos4);

        System.out.println((clock(n1,n3,n2) ^ clock(n1,n4,n2)) && (clock(n3,n1,n4) ^ clock(n3,n2,n4)));
    }
}
