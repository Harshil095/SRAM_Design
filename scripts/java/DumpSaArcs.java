import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class DumpSaArcs {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DumpSaArcs", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), new File("SRAM.jelib").toURI().toURL(), null, FileType.JELIB, false);
                    Cell sa = lib.findNodeProto("sense_amplifier{lay}");
                    PrintWriter pw = new PrintWriter("sa_arcs.txt");
                    pw.println("SA bounds: " + sa.getBounds());
                    for (Iterator<ArcInst> it = sa.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getProto().getName().contains("Metal")) {
                            pw.println("Arc: " + a.getName() + " layer=" + a.getProto().getName() + " head=" + a.getHeadLocation() + " tail=" + a.getTailLocation() + " w=" + a.getLambdaBaseWidth());
                        }
                    }
                    pw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(8000);
    }
}
