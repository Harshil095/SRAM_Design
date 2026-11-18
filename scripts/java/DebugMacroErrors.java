import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.io.File;

public class DebugMacroErrors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DebugMacroErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell lay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    if (lay == null) {
                        System.out.println("Macro lay is null");
                        return true;
                    }
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println("Errors: " + errLog.getNumErrors());
                    java.lang.reflect.Field fLines = Class.forName("com.sun.electric.tool.user.ErrorHighPoly").getDeclaredField("linesList");
                    fLines.setAccessible(true);
                    java.lang.reflect.Field fP1 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p1");
                    fP1.setAccessible(true);
                    java.lang.reflect.Field fP2 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p2");
                    fP2.setAccessible(true);

                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        System.out.println("DRC: " + ml.getMessageString());
                        for (Iterator hit = ml.getHighlights(); hit.hasNext(); ) {
                            Object h = hit.next();
                            try {
                                java.util.List lines = (java.util.List) fLines.get(h);
                                if (lines != null) {
                                    for (Object line : lines) {
                                        System.out.println("   Line: " + fP1.get(line) + " -> " + fP2.get(line));
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
    }
}
