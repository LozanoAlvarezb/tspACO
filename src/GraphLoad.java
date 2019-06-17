import java.util.ArrayList;
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

    public static void main(String[] args){
        /*for (int[] pos:
        loadFromFile("/home/borja_lozano/Projects/Multiagent/mason_tsp/data/xqf131.tsp")) {
            System.out.println(pos[0]+ " : " + pos[1]);

        }*/
    }
}
