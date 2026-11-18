import java.io.File;
import java.util.*;
import java.lang.reflect.Method;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class CategorizeTopErrors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("CategorizeTopErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p1 = "/Users/harshil_rathan/Documents/BTech IITH/SEM5/VLSI/SRAM_Complete_V1_suraj.jelib";
                    Library uLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p1).toURI().toURL(), null, FileType.JELIB, false);
                    Cell top = uLib.findNodeProto("sram_edgetriggered{lay}");

                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.ignoreAreaCheck = false;
                    dp.ignoreExtensionRuleChecking = false;
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;

                    Quick.checkDesignRules(errLog, top, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    System.out.println("Total errors in sram_edgetriggered: " + errLog.getNumErrors());
                    
                    int internalToSubcells = 0;
                    int topLevelOnly = 0;
                    List<String> topErrors = new ArrayList<>();
                    
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        String msg = ml.getMessageString();
                        if (msg.contains("cell 'SRAM_Complete_V1_suraj:wl_driver_x16{lay}'") ||
                            msg.contains("cell 'SRAM_Complete_V1_suraj:wl_driver_gate{lay}'") ||
                            msg.contains("cell 'SRAM_Complete_V1_suraj:sense_amplifier{lay}'")) {
                            internalToSubcells++;
                        } else {
                            topLevelOnly++;
                            topErrors.add(msg);
                        }
                    }

                    System.out.println("Errors originating inside subcells (wl_driver_x16, wl_driver_gate, sense_amplifier): " + internalToSubcells);
                    System.out.println("Errors at top level (interconnect / routing / placement): " + topLevelOnly);
                    System.out.println("\n--- TOP LEVEL ERRORS (" + topErrors.size() + ") ---");
                    for (int i = 0; i < topErrors.size(); i++) {
                        System.out.println((i+1) + ". " + topErrors.get(i));
                    }

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
