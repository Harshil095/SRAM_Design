import java.io.File;
import java.net.URL;
import java.util.Iterator;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class PrintPadDetails {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintPadDetails", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    for (String name : new String[]{"Input_Pad{lay}", "Output_Pad{lay}"}) {
                        Cell c = lib.findNodeProto(name);
                        System.out.println("=== " + name + " ===");
                        System.out.println("Bounds: " + c.getBounds());
                        for (Iterator<Export> it = c.getExports(); it.hasNext(); ) {
                            Export ep = it.next();
                            System.out.println("  Export '" + ep.getName() + "' port center: " + ep.getOriginalPort().getCenter() + " node proto: " + ep.getOriginalPort().getNodeInst().getProto().getName());
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
