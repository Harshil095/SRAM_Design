import java.util.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
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

public class DiagnoseChipDRC {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DiagnoseChipDRC", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();

                    Cell chipLay = lib.findNodeProto("SRAM_Chip_Final{lay}");
                    if (chipLay == null) {
                        System.out.println("Cell SRAM_Chip_Final{lay} not found!");
                        return true;
                    }

                    System.out.println("=== 1. Running DRC on SRAM_Chip_Final{lay} ===");
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, chipLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    int numErrors = errLog.getNumErrors();
                    int numWarnings = errLog.getNumWarnings();
                    System.out.println(">>> Total DRC Errors: " + numErrors + ", Warnings: " + numWarnings + " <<<");

                    Map<String, Integer> categoryCounts = new LinkedHashMap<>();
                    Map<String, List<String>> categorySamples = new LinkedHashMap<>();

                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        String msg = ml.getMessageString();
                        categoryCounts.put(msg, categoryCounts.getOrDefault(msg, 0) + 1);
                        
                        List<String> samples = categorySamples.computeIfAbsent(msg, k -> new ArrayList<>());
                        if (samples.size() < 3) {
                            samples.add(ml.toString());
                        }
                    }

                    System.out.println("\n=== Categorized DRC Error Summary ===");
                    for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                        System.out.printf("%-60s : %4d errors%n", entry.getKey(), entry.getValue());
                    }

                    System.out.println("\n=== Detailed Samples per Category ===");
                    for (Map.Entry<String, List<String>> entry : categorySamples.entrySet()) {
                        System.out.println("--- Category: " + entry.getKey() + " ---");
                        for (String sample : entry.getValue()) {
                            System.out.println("    " + sample);
                        }
                    }

                    // Also check hierarchical DRC as GUI does
                    System.out.println("\n=== 2. Running DRC with Hierarchical Flag ===");
                    ErrorLogger hierLog = DRC.getDRCErrorLogger(true, null);
                    com.sun.electric.tool.drc.Quick.checkDesignRules(hierLog, chipLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, true);
                    hierLog.termLogging(true);
                    System.out.println(">>> Hierarchical DRC Errors: " + hierLog.getNumErrors() + ", Warnings: " + hierLog.getNumWarnings() + " <<<");
                    Map<String, Integer> hierCounts = new LinkedHashMap<>();
                    for (Iterator<ErrorLogger.MessageLog> it = hierLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        String msg = ml.getMessageString();
                        hierCounts.put(msg, hierCounts.getOrDefault(msg, 0) + 1);
                    }
                    for (Map.Entry<String, Integer> entry : hierCounts.entrySet()) {
                        System.out.printf("%-60s : %4d errors%n", entry.getKey(), entry.getValue());
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
