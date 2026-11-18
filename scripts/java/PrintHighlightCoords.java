import java.io.File;
import java.util.*;
import java.lang.reflect.Field;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class PrintHighlightCoords {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintHighlightCoords", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p1 = "/Users/harshil_rathan/Documents/BTech IITH/SEM5/VLSI/SRAM_Complete_V1_suraj.jelib";
                    Library uLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p1).toURI().toURL(), null, FileType.JELIB, false);
                    Cell top = uLib.findNodeProto("sram_edgetriggered{lay}");

                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.ignoreAreaCheck = false;
                    dp.ignoreExtensionRuleChecking = false;
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;

                    Quick.checkDesignRules(errLog, top, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    Field fP1 = null, fP2 = null, fPoint = null, fObj = null;
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        String msg = ml.getMessageString();
                        if (msg.contains("OVERLAPS") && (msg.contains("ADDR_0") || msg.contains("CLK") || msg.contains("row_decoder"))) {
                            System.out.println("ERROR: " + msg);
                            for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                                ErrorHighlight eh = hit.next();
                                if (fP1 == null) {
                                    for (Class c = eh.getClass(); c != null; c = c.getSuperclass()) {
                                        for (Field f : c.getDeclaredFields()) {
                                            f.setAccessible(true);
                                            if (f.getName().equals("p1")) fP1 = f;
                                            if (f.getName().equals("p2")) fP2 = f;
                                            if (f.getName().equals("point")) fPoint = f;
                                        }
                                    }
                                }
                                System.out.println("   p1=" + (fP1 != null ? fP1.get(eh) : null) + ", p2=" + (fP2 != null ? fP2.get(eh) : null) + ", pt=" + (fPoint != null ? fPoint.get(eh) : null));
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
