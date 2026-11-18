import java.io.File;
import java.net.URL;
import java.util.Iterator;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class CompareSchematicVersions {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("Compare", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                        Cell c = it.next();
                        if (c.getName().equals("SRAM_Chip_Final") && c.isSchematic()) {
                            System.out.println("=== Version " + c.getVersion() + " (isNewest=" + (c == c.getNewestVersion()) + ") ===");
                            System.out.println("Nodes count: " + c.getNumNodes() + ", Arcs count: " + c.getNumArcs());
                            for (Iterator<NodeInst> nit = c.getNodes(); nit.hasNext(); ) {
                                NodeInst ni = nit.next();
                                System.out.println("  Node: " + ni.getProto().getName() + " at " + ni.getTrueCenter());
                            }
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
