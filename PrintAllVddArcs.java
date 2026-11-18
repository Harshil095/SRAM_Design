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

public class PrintAllVddArcs {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintAllVddArcs", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p1 = "/Users/harshil_rathan/Documents/BTech IITH/SEM5/VLSI/SRAM_Complete_V1_suraj.jelib";
                    String p2 = "/Users/harshil_rathan/Downloads/SRAM_Complete_V1_suraj_antigravity_BACKUP_DRC0_LVS0.jelib";

                    Library uLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p1).toURI().toURL(), null, FileType.JELIB, false);
                    Library fLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p2).toURI().toURL(), null, FileType.JELIB, false);

                    Cell uc = uLib.findNodeProto("sram_edgetriggered{lay}");
                    Cell fc = fLib.findNodeProto("sram_edgetriggered{lay}");

                    List<String> uVdd = new ArrayList<>();
                    for (Iterator<ArcInst> it = uc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().equals("VDD")) {
                            uVdd.add(a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }
                    Collections.sort(uVdd);

                    List<String> fVdd = new ArrayList<>();
                    for (Iterator<ArcInst> it = fc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().equals("VDD")) {
                            fVdd.add(a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }
                    Collections.sort(fVdd);

                    System.out.println("=== VDD ARCS IN USER ONLY ===");
                    for (String s : uVdd) {
                        if (!fVdd.contains(s)) System.out.println("  " + s);
                    }

                    System.out.println("=== VDD ARCS IN FRIEND ONLY ===");
                    for (String s : fVdd) {
                        if (!uVdd.contains(s)) System.out.println("  " + s);
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
