import java.util.*;
import java.lang.reflect.Field;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.net.URL;

public class PrintErrorPoints {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintErrorPoints", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell lay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_DEFAULT;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    
                    Field fLines = Class.forName("com.sun.electric.tool.user.ErrorHighPoly").getDeclaredField("linesList");
                    fLines.setAccessible(true);
                    Field fP1 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p1");
                    fP1.setAccessible(true);
                    Field fP2 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p2");
                    fP2.setAccessible(true);
                    
                    Map<String, Integer> seen = new HashMap<>();
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        String msg = ml.getMessageString();
                        String cat = msg.split(";")[0].split(":")[0];
                        if (seen.getOrDefault(cat, 0) < 3) {
                            seen.put(cat, seen.getOrDefault(cat, 0) + 1);
                            System.out.println("MSG: " + msg);
                            for (Iterator hit = ml.getHighlights(); hit.hasNext(); ) {
                                Object h = hit.next();
                                if (h.getClass().getName().contains("ErrorHighPoly")) {
                                    List lines = (List)fLines.get(h);
                                    if (lines != null && !lines.isEmpty()) {
                                        Object l0 = lines.get(0);
                                        System.out.println("   Coord: " + fP1.get(l0) + " -> " + fP2.get(l0));
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
        Thread.sleep(6000);
    }
}
