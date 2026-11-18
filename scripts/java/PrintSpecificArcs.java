import java.io.File;
import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class PrintSpecificArcs {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintSpecificArcs", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p1 = "/Users/harshil_rathan/Documents/BTech IITH/SEM5/VLSI/SRAM_Complete_V1_suraj.jelib";
                    String p2 = "/Users/harshil_rathan/Downloads/SRAM_Complete_V1_suraj_antigravity_BACKUP_DRC0_LVS0.jelib";

                    Library uLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p1).toURI().toURL(), null, FileType.JELIB, false);
                    Library fLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p2).toURI().toURL(), null, FileType.JELIB, false);

                    Cell uc = uLib.findNodeProto("sram_edgetriggered{lay}");
                    Cell fc = fLib.findNodeProto("sram_edgetriggered{lay}");

                    System.out.println("=== USER ARCS for ADDR_IN ===");
                    for (Iterator<ArcInst> it = uc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().equals("ADDR_IN")) {
                            System.out.println("  User ADDR_IN: " + a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }

                    System.out.println("=== FRIEND ARCS for ADDR_IN ===");
                    for (Iterator<ArcInst> it = fc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().equals("ADDR_IN")) {
                            System.out.println("  Friend ADDR_IN: " + a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }

                    System.out.println("\n=== USER ARCS for CLK ===");
                    for (Iterator<ArcInst> it = uc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().equals("CLK")) {
                            System.out.println("  User CLK: " + a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }

                    System.out.println("=== FRIEND ARCS for CLK ===");
                    for (Iterator<ArcInst> it = fc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().equals("CLK")) {
                            System.out.println("  Friend CLK: " + a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(5000);
    }
}
