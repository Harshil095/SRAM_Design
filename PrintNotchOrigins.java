import java.io.File;
import java.util.*;
import java.lang.reflect.*;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class PrintNotchOrigins {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintNotchOrigins", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795002561961_7f7dd016.jelib";
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p).toURI().toURL(), null, FileType.JELIB, false);
                    Cell chip = lib.findNodeProto("SRAM_Chip_Final{lay}");

                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, chip, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    Field fLines = Class.forName("com.sun.electric.tool.user.ErrorHighPoly").getDeclaredField("linesList");
                    fLines.setAccessible(true);
                    Method mOrig = Class.forName("com.sun.electric.tool.user.ErrorHighPoly").getDeclaredMethod("getOrigCell", EDatabase.class);
                    mOrig.setAccessible(true);

                    Map<String, Integer> cellCounts = new HashMap<>();
                    List<String> coordSamples = new ArrayList<>();

                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                            ErrorHighlight eh = hit.next();
                            Cell orig = (Cell) mOrig.invoke(eh, chip.getDatabase());
                            String cName = (orig != null ? orig.getName() : "null");
                            cellCounts.put(cName, cellCounts.getOrDefault(cName, 0) + 1);

                            List lines = (List) fLines.get(eh);
                            if (coordSamples.size() < 10) {
                                coordSamples.add(ml.getMessageString() + " in " + cName + " -> lines: " + lines);
                            }
                        }
                    }

                    System.out.println("=== NOTCH CELL ORIGINS ===");
                    for (String k : cellCounts.keySet()) {
                        System.out.println("  " + k + ": " + cellCounts.get(k));
                    }
                    System.out.println("\n=== SAMPLES ===");
                    for (String s : coordSamples) {
                        System.out.println("  " + s);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(6000);
    }
}
