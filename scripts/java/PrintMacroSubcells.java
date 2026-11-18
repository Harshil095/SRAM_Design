
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.util.*;

public class PrintMacroSubcells {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintMacroSubcells", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell macro = lib.findNodeProto("SRAM_256_Macro{lay}");

                    for (Iterator<NodeInst> it = macro.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getProto() instanceof Cell) {
                            System.out.println("Subcell: " + ni.getName() + " proto=" + ni.getProto().getName() + " center=" + ni.getTrueCenter() + " bounds=" + ni.getBounds());
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
        System.exit(0);
    }
}
