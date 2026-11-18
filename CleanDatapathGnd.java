import com.sun.electric.technology.Layer;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.bool.*;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.User;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.*;

public class CleanDatapathGnd {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("CleanDatapathGnd", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("SRAM_Core_Datapath{lay}");

                    NodeInst pre = null;
                    NodeInst con191 = null;
                    NodeInst pin4 = null;
                    for (Iterator<NodeInst> it = c.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getName().equals("Precharg@0")) pre = ni;
                        if (ni.getName().equals("contact@191")) con191 = ni;
                        if (ni.getName().equals("pin@4")) pin4 = ni;
                    }
                    if (pin4 != null) pin4.kill();

                    Technology mocmos = Technology.findTechnology("mocmos");
                    ArcProto m1 = mocmos.findArcProto("Metal-1");

                    ArcInst a = ArcInst.makeInstance(m1, getEditingPreferences(),
                        pre.findPortInstFromProto(((Cell)pre.getProto()).findExport("gnd")),
                        con191.getPortInst(0));
                    a.setLambdaBaseWidth(3.0);

                    Point2D pHead = a.getHeadLocation();
                    Point2D pTail = a.getTailLocation();
                    System.out.println("New arc head: " + pHead + " tail: " + pTail);

                    LayoutMergerFactory factory = LayoutMergerFactory.getInstance();
                    Layer m1Layer = mocmos.findLayer("Metal-1");
                    System.out.println("canMerge(Metal-1) in Datapath: " + factory.newMerger(c).canMerge(m1Layer));

                    Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                    System.out.println("Saved SRAM.jelib!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(5000);
        System.exit(0);
    }
}