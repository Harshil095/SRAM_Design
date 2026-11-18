import java.io.File;
import java.util.*;
import java.lang.reflect.Method;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class Print528Errors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("Print528Errors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795002561961_7f7dd016.jelib";
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p).toURI().toURL(), null, FileType.JELIB, false);
                    Cell chip = lib.findNodeProto("SRAM_Chip_Final{lay}");

                    Method m = DRC.class.getDeclaredMethod("checkDRCHierarchically", DRC.DRCPreferences.class, Cell.class, List.class, java.awt.geom.Rectangle2D.class, GeometryHandler.GHMode.class, boolean.class);
                    m.setAccessible(true);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    m.invoke(null, dp, chip, null, null, GeometryHandler.GHMode.ALGO_SWEEP, false);

                    // Wait for child jobs to complete
                    Thread.sleep(4000);

                    ErrorLogger errLog = DRC.getDRCErrorLogger(false, null);
                    System.out.println(">>> TOTAL LOGGED ERRORS: " + errLog.getNumErrors());
                    Map<String, Integer> counts = new HashMap<>();
                    List<String> samples = new ArrayList<>();
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        String msg = it.next().getMessageString();
                        counts.put(msg, counts.getOrDefault(msg, 0) + 1);
                        if (samples.size() < 40) samples.add(msg);
                    }
                    System.out.println("=== ERROR SUMMARY (" + counts.size() + " types) ===");
                    for (String k : counts.keySet()) {
                        System.out.println("  " + counts.get(k) + "x : " + k);
                    }
                    System.out.println("\n=== SAMPLES ===");
                    for (int i = 0; i < samples.size(); i++) {
                        System.out.println((i+1) + ". " + samples.get(i));
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
