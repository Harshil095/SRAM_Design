
import java.io.File;
import java.net.URL;
import java.awt.geom.Point2D;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class PrintDFFBox {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintDFFBox", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell dff = lib.findNodeProto("DFF{lay}");
                    System.out.println("DFF bounds: " + dff.getBounds());
                    for (java.util.Iterator it = dff.getNodes(); it.hasNext(); ) {
                        NodeInst ni = (NodeInst) it.next();
                        double y = ni.getAnchorCenter().getY();
                        if (y > 35.0 || y < -35.0) {
                            System.out.println("  Node: " + ni.getProto().getName() + " at " + ni.getAnchorCenter() + " size " + ni.getXSize() + "x" + ni.getYSize());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(3000);
    }
}
