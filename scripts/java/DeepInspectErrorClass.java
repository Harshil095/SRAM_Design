
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class DeepInspectErrorClass {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DeepInspectErrorClass", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("SRAM_Core_Datapath{lay}");
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.ignoreAreaCheck = false;
                    dp.ignoreExtensionRuleChecking = false;
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    Quick.checkDesignRules(errLog, c, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, true);
                    errLog.termLogging(true);

                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        System.out.println("ML: " + ml.getMessageString());
                        for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                            ErrorHighlight eh = hit.next();
                            System.out.println("  EH class: " + eh.getClass().getName());
                            for (Field fld : eh.getClass().getDeclaredFields()) {
                                fld.setAccessible(true);
                                System.out.println("    field: " + fld.getName() + " = " + fld.get(eh));
                            }
                        }
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
        System.exit(0);
    }
}
