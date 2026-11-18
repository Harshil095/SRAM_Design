
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

public class QueryDFF24 {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("QueryDFF24", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell dff = lib.findNodeProto("DFF{lay}");

                    for (Iterator it = dff.getArcs(); it.hasNext(); ) {
                        ArcInst ai = (ArcInst) it.next();
                        double y1 = ai.getHeadLocation().getY();
                        double y2 = ai.getTailLocation().getY();
                        if (Math.abs(y1 - 24.0) < 5.0 || Math.abs(y2 - 24.0) < 5.0) {
                            System.out.println("Arc: " + ai.getProto().getName() + " from " + ai.getHeadLocation() + " to " + ai.getTailLocation() + " width: " + ai.getLambdaBaseWidth());
                        }
                    }
                    for (Iterator it = dff.getNodes(); it.hasNext(); ) {
                        NodeInst ni = (NodeInst) it.next();
                        double y = ni.getAnchorCenter().getY();
                        if (Math.abs(y - 24.0) < 5.0) {
                            System.out.println("Node: " + ni.getProto().getName() + " at " + ni.getAnchorCenter() + " size " + ni.getXSize() + "x" + ni.getYSize());
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
