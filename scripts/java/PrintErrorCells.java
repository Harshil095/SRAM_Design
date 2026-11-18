
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.util.*;

public class PrintErrorCells {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintErrorCells", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("SRAM_256_Macro{lay}");

                    DRC.resetDRCDates(true);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.ignoreAreaCheck = false;
                    dp.ignoreExtensionRuleChecking = false;
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;

                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    Quick.checkDesignRules(errLog, c, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    Map<String, Integer> cellCounts = new HashMap<String, Integer>();
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                            ErrorHighlight eh = hit.next();
                            Cell errCell = eh.getCell(c.getDatabase());
                            String cname = (errCell != null) ? errCell.getName() : "null";
                            cellCounts.put(cname, cellCounts.getOrDefault(cname, 0) + 1);
                        }
                    }
                    System.out.println("Error highlights by cell:");
                    for (Map.Entry<String, Integer> e : cellCounts.entrySet()) {
                        System.out.println("  " + e.getKey() + ": " + e.getValue());
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
