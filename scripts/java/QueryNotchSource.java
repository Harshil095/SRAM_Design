import java.io.File;
import java.net.URL;
import java.util.Iterator;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class QueryNotchSource {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("QueryNotch", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell dff = lib.findNodeProto("DFF{lay}");

                    System.out.println("--- In DFF near Y = 9.5 ---");
                    for (Iterator it = dff.getArcs(); it.hasNext(); ) {
                        ArcInst ai = (ArcInst) it.next();
                        double hy = ai.getHeadLocation().getY(), ty = ai.getTailLocation().getY();
                        if (Math.abs(hy - 9.5) < 5.0 || Math.abs(ty - 9.5) < 5.0) {
                            System.out.println("  " + ai.getProto().getName() + " from " + ai.getHeadLocation() + " to " + ai.getTailLocation());
                        }
                    }
                    for (Iterator it = dff.getNodes(); it.hasNext(); ) {
                        NodeInst ni = (NodeInst) it.next();
                        double y = ni.getAnchorCenter().getY();
                        if (Math.abs(y - 9.5) < 5.0) {
                            System.out.println("  Node: " + ni.getProto().getName() + " at " + ni.getAnchorCenter());
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
