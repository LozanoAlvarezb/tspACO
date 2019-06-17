import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;

import java.awt.geom.*;
import java.awt.*;

public class GraphNode extends SimplePortrayal2D implements Steppable {

    private int[] pos;

    public GraphNode(int pos[]){this.pos=pos;}

    public int[] getPos() {return pos;}

    public void step(final SimState state){}

    public final void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
        double diamx = info.draw.width*Sim.NODE_DIAMETER;
        double diamy = info.draw.height*Sim.NODE_DIAMETER;


        graphics.setColor(Color.black);
        graphics.fillOval((int)(info.draw.x-diamx/2),
                (int)(info.draw.y-diamy/2),
                (int)(diamx),(int)(diamy));
    }
}
