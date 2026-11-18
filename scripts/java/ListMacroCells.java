import java.io.File;
import java.net.URL;
import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class ListMacroCells {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ListMacroCells", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);

                    Cell macroLay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    System.out.println("=== Instances in SRAM_256_Macro{lay} ===");
                    for (Iterator it = macroLay.getNodes(); it.hasNext(); ) {
                        NodeInst ni = (NodeInst) it.next();
                        if (ni.isCellInstance()) {
                            System.out.printf("CellInst %s (%s) at (%.1f, %.1f) bounds %s%n", 
                                ni.getName(), ni.getProto().getName(), ni.getAnchorCenterX(), ni.getAnchorCenterY(), ni.getBounds());
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
