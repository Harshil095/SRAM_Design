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

public class QueryDFFLeft {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("QueryDFFLeft", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell dff = lib.findNodeProto("DFF{lay}");

                    System.out.println("--- All Arcs between X = -30 and X = 80 in DFF ---");
                    for (Iterator it = dff.getArcs(); it.hasNext(); ) {
                        ArcInst ai = (ArcInst) it.next();
                        double hx = ai.getHeadLocation().getX(), tx = ai.getTailLocation().getX();
                        if ((hx >= -30.0 && hx <= 80.0) || (tx >= -30.0 && tx <= 80.0)) {
                            System.out.println("  " + ai.getProto().getName() + " from (" + hx + "," + ai.getHeadLocation().getY() + ") to (" + tx + "," + ai.getTailLocation().getY() + ")");
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
