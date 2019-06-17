import sim.engine.SimState;
import sim.display.Controller;
import sim.display.GUIState;
import sim.display.Display2D;
import javax.swing.JFrame;
import java.awt.Color;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;

public class SimUI extends GUIState{

    public Display2D display;
    public JFrame frame;

    NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();
    ContinuousPortrayal2D nodePortrayal = new ContinuousPortrayal2D();

    // Constructor. Creates the simulation to be represented by this class
    public SimUI(){super(new Sim(System.currentTimeMillis()));}

    public SimUI(SimState state){super(state);}

    public Object getSimulationInspectedObject(){return state;}

    public void start() {
        super.start();
        setupPortrayals();
    }

    public boolean step()
    {
        load(state);
        return super.step();
    }

    public void load(SimState state) {
        super.load(state);
        setupPortrayals();
    }

    public void quit()
    {
        super.quit();

        if (frame!=null)
            frame.dispose();
        frame = null;
        display = null;
    }

    public void init(final Controller c)
    {
        super.init(c);

        display = new Display2D(800,600,this);
        frame = display.createFrame();
        c.registerFrame(frame);
        frame.setVisible(true);

        display.setBackdrop(Color.white);

        display.attach(edgePortrayal, "Edges");
        display.attach(nodePortrayal, "Nodes");
    }

    //  Tell the portrayals what to portray and how to portray them
    public void setupPortrayals()
    {
        // If there is a local travel we display it
        if(((Sim)state).bestTravel!=null)
        {
            edgePortrayal.setField(new SpatialNetwork2D(((Sim)state).environment,((Sim)state).bestTravel));

            SimpleEdgePortrayal2D p = new SimpleEdgePortrayal2D();
            p.setBaseWidth(1);
            p.setAdjustsThickness(true);
            p.setShape(SimpleEdgePortrayal2D.SHAPE_LINE_ROUND_ENDS);
            nodePortrayal.setField(((Sim)state).environment);

            // reschedule the displayer
            display.reset();
            display.setBackdrop(Color.white);

            // redraw the display
            display.repaint();

        }
    }

    public static void main(String[] args){new SimUI().createController();}
}
