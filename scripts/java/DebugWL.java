import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.io.File;

public class DebugWL {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DebugWL", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    System.out.println("Step 1: Reading library");
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    System.out.println("Step 2: Library read");
                    Cell data = lib.findNodeProto("SRAM_Core_Datapath{lay}");
                    System.out.println("Step 3: Found datapath: " + data);
                    Export ex = data.findExport("WL[0]");
                    System.out.println("Step 4: Found WL[0] export: " + ex);
                    System.out.println("Step 5: Port center: " + ex.getOriginalPort().getCenter());
                } catch (Throwable e) {
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