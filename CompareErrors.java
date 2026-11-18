import java.io.File;
import java.net.URL;
import java.util.*;
import java.awt.geom.Point2D;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class CompareErrors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("CompareErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);

                    Cell macroLay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    Cell chipLay = lib.findNodeProto("SRAM_Chip_Final{lay}");

                    DRC.resetDRCDates(true);
                    ErrorLogger macLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(macLog, macroLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    macLog.termLogging(true);

                    DRC.resetDRCDates(true);
                    ErrorLogger chipLog = DRC.getDRCErrorLogger(true, null);
                    com.sun.electric.tool.drc.Quick.checkDesignRules(chipLog, chipLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    chipLog.termLogging(true);

                    System.out.println("Macro errors: " + macLog.getNumErrors());
                    System.out.println("Chip errors: " + chipLog.getNumErrors());

                    Map<String, Integer> macMap = new HashMap<>();
                    for (Iterator it = macLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                        macMap.put(ml.getMessageString(), macMap.getOrDefault(ml.getMessageString(), 0) + 1);
                    }

                    Map<String, Integer> chipMap = new HashMap<>();
                    for (Iterator it = chipLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                        chipMap.put(ml.getMessageString(), chipMap.getOrDefault(ml.getMessageString(), 0) + 1);
                    }

                    System.out.println("\n=== Comparison by Category (Chip vs Macro) ===");
                    Set<String> allKeys = new TreeSet<>();
                    allKeys.addAll(macMap.keySet());
                    allKeys.addAll(chipMap.keySet());

                    for (String k : allKeys) {
                        int m = macMap.getOrDefault(k, 0);
                        int c = chipMap.getOrDefault(k, 0);
                        System.out.printf("%-60s | Macro: %3d | Chip: %3d | Diff: %+3d%n", k, m, c, (c - m));
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
