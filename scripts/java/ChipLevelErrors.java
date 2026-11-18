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

public class ChipLevelErrors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ChipLevelErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
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

                    java.lang.reflect.Field fLines = Class.forName("com.sun.electric.tool.user.ErrorHighPoly").getDeclaredField("linesList");
                    fLines.setAccessible(true);
                    java.lang.reflect.Field fP1 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p1");
                    fP1.setAccessible(true);
                    java.lang.reflect.Field fP2 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p2");
                    fP2.setAccessible(true);

                    int total = errLog.getNumErrors();
                    System.out.println("Total errors: " + total);

                    List<String> chipLevelErrors = new ArrayList<>();
                    List<String> macroLevelErrors = new ArrayList<>();

                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        String msg = ml.getMessageString();
                        
                        // Extract points from ErrorHighPoly
                        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
                        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
                        
                        for (Iterator hit = ml.getHighlights(); hit.hasNext(); ) {
                            Object eh = hit.next();
                            if (eh.getClass().getSimpleName().equals("ErrorHighPoly")) {
                                List lines = (List) fLines.get(eh);
                                if (lines != null) {
                                    for (Object l : lines) {
                                        Point2D p1 = (Point2D) fP1.get(l);
                                        Point2D p2 = (Point2D) fP2.get(l);
                                        minX = Math.min(minX, Math.min(p1.getX(), p2.getX()));
                                        maxX = Math.max(maxX, Math.max(p1.getX(), p2.getX()));
                                        minY = Math.min(minY, Math.min(p1.getY(), p2.getY()));
                                        maxY = Math.max(maxY, Math.max(p1.getY(), p2.getY()));
                                    }
                                }
                            }
                        }

                        String desc = String.format("%s at [%.1f, %.1f] to [%.1f, %.1f]", msg, minX, minY, maxX, maxY);
                        // Check if within chip peripherals (X < -700, or Y < -650, or Y > 2400, or X > 4300)
                        if (minX < -750 || minY < -700 || maxY > 2350 || maxX > 4250) {
                            chipLevelErrors.add(desc);
                        } else {
                            macroLevelErrors.add(desc);
                        }
                    }

                    System.out.println(">>> Chip-level peripheral errors: " + chipLevelErrors.size() + " <<<");
                    for (int i = 0; i < Math.min(chipLevelErrors.size(), 30); i++) {
                        System.out.println("   " + chipLevelErrors.get(i));
                    }

                    System.out.println("\n>>> Macro internal errors: " + macroLevelErrors.size() + " <<<");
                    for (int i = 0; i < Math.min(macroLevelErrors.size(), 10); i++) {
                        System.out.println("   " + macroLevelErrors.get(i));
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
