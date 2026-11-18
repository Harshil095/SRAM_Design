import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.NodeProto;
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

public class ReconnectDatapathGnd {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ReconnectDatapathGnd", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("SRAM_Core_Datapath{lay}");
                    NodeInst pre = null;
                    NodeInst con191 = null;
                    for (Iterator<NodeInst> it = c.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getName().equals("Precharg@0")) pre = ni;
                        if (ni.getName().equals("contact@191")) con191 = ni;
                    }
                    Technology mocmos = Technology.findTechnology("mocmos");
                    ArcProto m1 = mocmos.findArcProto("Metal-1");
                    NodeProto m1Pin = mocmos.findNodeProto("Metal-1-Pin");

                    // Precharg@0.gnd is at global (433.5, 1366.5)
                    // contact@191 is at (-75.0, 1375.0)
                    // Route Manhattan via a pin at (-75.0, 1366.5)
                    NodeInst corner = NodeInst.makeInstance(m1Pin, new Point2D.Double(-75.0, 1366.5), 3.0, 3.0, c);
                    ArcInst a1 = ArcInst.makeInstance(m1, getEditingPreferences(), pre.findPortInstFromProto(((Cell)pre.getProto()).findExport("gnd")), corner.getPortInst(0));
                    a1.setLambdaBaseWidth(3.0);
                    ArcInst a2 = ArcInst.makeInstance(m1, getEditingPreferences(), corner.getPortInst(0), con191.getPortInst(0));
                    a2.setLambdaBaseWidth(3.0);

                    Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                    System.out.println("Reconnected datapath gnd with Manhattan routing and saved!");
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