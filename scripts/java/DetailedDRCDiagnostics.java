import java.io.File;
import java.net.URL;
import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class DetailedDRCDiagnostics {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DetailedDRCDiagnostics", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);

                    Cell chipLay = lib.findNodeProto("SRAM_Chip_Final{lay}");
                    DRC.resetDRCDates(true);

                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;

                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, chipLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    System.out.println("Total errors: " + errLog.getNumErrors());

                    int count = 0;
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        count++;
                        System.out.println("----------------------------------------------------------------");
                        System.out.printf("[%03d] %s%n", count, ml.getMessageString());
                        System.out.println("      Cell: " + ml.getCell());
                        for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                            ErrorHighlight eh = hit.next();
                            System.out.println("      Highlight: " + eh.getClass().getSimpleName() + " -> " + eh.toString());
                        }
                        if (count >= 50) {
                            System.out.println("... stopping preview after 50 items ...");
                            break;
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
