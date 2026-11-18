import java.io.File;
import java.net.URL;
import java.util.*;
import java.awt.geom.Point2D;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class DetailedErrorInspect {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DetailedInspect", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell chipLay = lib.findNodeProto("SRAM_Chip_Final{lay}");

                    DRC.resetDRCDates(true);
                    ErrorLogger chipLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(chipLog, chipLay, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, false);
                    chipLog.termLogging(true);

                    java.lang.reflect.Field fLines = Class.forName("com.sun.electric.tool.user.ErrorHighPoly").getDeclaredField("linesList");
                    fLines.setAccessible(true);
                    java.lang.reflect.Field fP1 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p1");
                    fP1.setAccessible(true);
                    java.lang.reflect.Field fP2 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p2");
                    fP2.setAccessible(true);

                    for (Iterator it = chipLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                        String msg = ml.getMessageString();
                        for (Iterator hit = ml.getHighlights(); hit.hasNext(); ) {
                            Object eh = hit.next();
                            if (eh.getClass().getSimpleName().equals("ErrorHighPoly")) {
                                List lines = (List) fLines.get(eh);
                                for (Object line : lines) {
                                    Point2D p1 = (Point2D) fP1.get(line);
                                    Point2D p2 = (Point2D) fP2.get(line);
                                    if (p1.getX() < -1240 && p1.getX() > -1260 && p1.getY() > 2220 && p1.getY() < 2240) {
                                        System.out.println("MATCHING ERROR: " + msg);
                                        System.out.println("  Line: " + p1 + " to " + p2);
                                        System.out.println("  Full poly lines:");
                                        for (Object l2 : lines) {
                                            System.out.println("    " + fP1.get(l2) + " -> " + fP2.get(l2));
                                        }
                                    }
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
        Thread.sleep(5000);
    }
}
