import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Iterator;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class DumpExactDrcErrors {

    public static void checkAndDump(Library lib, String cellName) {
        Cell cell = lib.findNodeProto(cellName);
        if (cell == null) {
            System.out.println("Cell " + cellName + " not found!");
            return;
        }

        System.out.println("==================================================");
        System.out.println("Running Full DRC on " + cellName);
        System.out.println("Technology: " + cell.getTechnology());

        ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
        dp.ignoreAreaCheck = false; // Area bit ON
        dp.ignoreExtensionRuleChecking = false; // Extension bit ON
        dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
        dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_DEFAULT;

        Quick.checkDesignRules(errLog, cell, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
        errLog.termLogging(true);

        int totalErrors = errLog.getNumErrors();
        int totalWarnings = errLog.getNumWarnings();
        System.out.println("Total Errors: " + totalErrors + ", Warnings: " + totalWarnings);

        int count = 0;
        for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
            ErrorLogger.MessageLog ml = it.next();
            count++;
            StringBuilder details = new StringBuilder();
            for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                ErrorHighlight eh = hit.next();
                try {
                    Method mGetObj = ErrorHighlight.class.getDeclaredMethod("getObject", com.sun.electric.database.hierarchy.EDatabase.class);
                    mGetObj.setAccessible(true);
                    Object obj = mGetObj.invoke(eh, cell.getDatabase());
                    if (obj instanceof NodeInst) {
                        NodeInst ni = (NodeInst) obj;
                        details.append(" Node[").append(ni.getProto().getName()).append(" at ").append(ni.getAnchorCenter()).append("]");
                    } else if (obj instanceof ArcInst) {
                        ArcInst ai = (ArcInst) obj;
                        details.append(" Arc[").append(ai.getProto().getName()).append(" (").append(ai.getHeadLocation()).append(" -> ").append(ai.getTailLocation()).append(")]");
                    }
                } catch (Exception ignored) {}

                try {
                    Field fPoint = eh.getClass().getDeclaredField("point");
                    fPoint.setAccessible(true);
                    details.append(" Point[").append(fPoint.get(eh)).append("]");
                } catch (Exception ignored) {}

                try {
                    Field fP1 = eh.getClass().getDeclaredField("p1");
                    fP1.setAccessible(true);
                    Field fP2 = eh.getClass().getDeclaredField("p2");
                    fP2.setAccessible(true);
                    details.append(" Line[").append(fP1.get(eh)).append(" -> ").append(fP2.get(eh)).append("]");
                } catch (Exception ignored) {}
            }
            if (count <= 25 || count > totalErrors - 5) {
                System.out.println("  #" + count + " " + ml.getMessageString() + " | Geoms:" + details);
            } else if (count == 26) {
                System.out.println("  ... (middle errors omitted for brevity) ...");
            }
        }
        System.out.println("==================================================");
    }

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DumpExactDrcErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    checkAndDump(lib, "SRAM_256_Macro{lay}");
                    checkAndDump(lib, "SRAM_Chip_Final{lay}");
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
