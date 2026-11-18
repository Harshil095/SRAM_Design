import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.util.*;

public class DeepAuditMacro {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DeepAuditMacro", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("SRAM_256_Macro{lay}");

                    System.out.println("=== AUDITING CONTACTS IN SRAM_256_Macro{lay} ===");
                    int totalContacts = 0;
                    int undersizedContacts = 0;
                    for (Iterator<NodeInst> it = c.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        String proto = ni.getProto().getName();
                        if (proto.contains("-Con")) {
                            totalContacts++;
                            if (ni.getXSize() < 4.0 || ni.getYSize() < 4.0) {
                                undersizedContacts++;
                                System.out.println("  UNDERSIZED: " + ni.getName() + " proto=" + proto + " size=(" + ni.getXSize() + ", " + ni.getYSize() + ")");
                            }
                        }
                    }
                    System.out.println("Total contacts in macro: " + totalContacts);
                    System.out.println("Undersized contacts (<4x4): " + undersizedContacts);

                    System.out.println("=== RUNNING FULL HIERARCHICAL DRC ON SRAM_256_Macro{lay} ===");
                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.ignoreAreaCheck = false;
                    dp.ignoreExtensionRuleChecking = false;
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    Quick.checkDesignRules(errLog, c, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, true);
                    errLog.termLogging(true);

                    System.out.println("DRC Result: " + errLog.getNumErrors() + " errors, " + errLog.getNumWarnings() + " warnings");
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        System.out.println("  " + it.next().getMessageString());
                    }
                } catch (Exception e) {
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