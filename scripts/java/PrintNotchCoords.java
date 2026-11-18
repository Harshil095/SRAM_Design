import java.util.*;
import java.lang.reflect.*;
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

public class PrintNotchCoords {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintNotchCoords", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell lay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    
                    Class<?> hpClass = Class.forName("com.sun.electric.tool.user.ErrorHighPoly");
                    Field fLines = hpClass.getDeclaredField("linesList");
                    fLines.setAccessible(true);
                    
                    int count = 0;
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog msg = it.next();
                        if (msg.getMessageString().contains("Notch") && count < 5) {
                            System.out.println("MSG: " + msg.getMessageString());
                            for (Iterator hit = msg.getHighlights(); hit.hasNext(); ) {
                                Object h = hit.next();
                                if (hpClass.isInstance(h)) {
                                    List lines = (List) fLines.get(h);
                                    Class hlClass = Class.forName("com.sun.electric.tool.user.ErrorHighLine");
                                    Field fP1 = hlClass.getDeclaredField("p1");
                                    Field fP2 = hlClass.getDeclaredField("p2");
                                    fP1.setAccessible(true);
                                    fP2.setAccessible(true);
                                    for (Object line : lines) {
                                        System.out.println("   line: " + fP1.get(line) + " -> " + fP2.get(line));
                                    }
                                }
                            }
                            count++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(1500);
    }
}
