import java.io.File;
import java.util.*;
import java.lang.reflect.Method;
import java.awt.geom.Point2D;
import com.sun.electric.Main;
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

public class LocateNotches {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("LocateNotches", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795002561961_7f7dd016.jelib";
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p).toURI().toURL(), null, FileType.JELIB, false);
                    Cell chip = lib.findNodeProto("SRAM_Chip_Final{lay}");

                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    // Run Quick
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, chip, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    System.out.println("Quick errors: " + errLog.getNumErrors());
                    Map<String, Integer> areaCounts = new HashMap<>();
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                            ErrorHighlight eh = hit.next();
                            try {
                                Method mGetObj = ErrorHighlight.class.getDeclaredMethod("getObject", EDatabase.class);
                                mGetObj.setAccessible(true);
                                Object obj = mGetObj.invoke(eh, chip.getDatabase());
                                if (obj != null) {
                                    String type = obj.getClass().getSimpleName();
                                    areaCounts.put(type, areaCounts.getOrDefault(type, 0) + 1);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    System.out.println("Objects involved in errors: " + areaCounts);

                    // Print first 5 highlights
                    int count = 0;
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        count++;
                        if (count <= 10) {
                            System.out.println("Error " + count + ": " + ml.getMessageString());
                            for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                                ErrorHighlight eh = hit.next();
                                try {
                                    Method m = eh.getClass().getMethod("getPoly");
                                    System.out.println("   poly: " + m.invoke(eh));
                                } catch (Exception e) {
                                    System.out.println("   eh: " + eh);
                                }
                            }
                        }
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
