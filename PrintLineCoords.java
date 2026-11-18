
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.util.*;
import java.lang.reflect.Field;

public class PrintLineCoords {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintLineCoords", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("SRAM_Core_Datapath{lay}");

                    DRC.resetDRCDates(true);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.ignoreAreaCheck = false;
                    dp.ignoreExtensionRuleChecking = false;
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;

                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    Quick.checkDesignRules(errLog, c, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    Class<?> ehlClass = Class.forName("com.sun.electric.tool.user.ErrorHighLine");
                    Field fP1 = ehlClass.getDeclaredField("p1"); fP1.setAccessible(true);
                    Field fP2 = ehlClass.getDeclaredField("p2"); fP2.setAccessible(true);

                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        System.out.println("  MSG: " + ml.getMessageString());
                        for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                            ErrorHighlight eh = hit.next();
                            if (eh.getClass().getName().contains("ErrorHighPoly")) {
                                Field fLines = eh.getClass().getDeclaredField("linesList");
                                fLines.setAccessible(true);
                                List<?> lines = (List<?>) fLines.get(eh);
                                for (Object line : lines) {
                                    System.out.println("     Line: " + fP1.get(line) + " -> " + fP2.get(line));
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
        System.exit(0);
    }
}
