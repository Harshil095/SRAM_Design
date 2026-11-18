
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
import java.lang.reflect.Method;
import java.util.*;

public class DeepInspectDatapathErrors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DeepInspectDatapathErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    
                    for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); ) {
                        Cell c = cit.next();
                        List<Variable> toRemove = new ArrayList<Variable>();
                        for (Iterator<Variable> vit = c.getVariables(); vit.hasNext(); ) {
                            Variable v = vit.next();
                            if (v.getKey().getName().toUpperCase().contains("DRC")) toRemove.add(v);
                        }
                        for (Variable v : toRemove) c.delVar(v.getKey());
                    }
                    DRC.resetDRCDates(true);

                    Cell c = lib.findNodeProto("SRAM_Core_Datapath{lay}");
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.ignoreAreaCheck = false;
                    dp.ignoreExtensionRuleChecking = false;
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    Quick.checkDesignRules(errLog, c, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, true);
                    errLog.termLogging(true);

                    Method mGetObj = ErrorHighlight.class.getDeclaredMethod("getObject", EDatabase.class);
                    mGetObj.setAccessible(true);

                    int count = 0;
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        count++;
                        System.out.println("Error #" + count + ": " + ml.getMessageString());
                        for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                            ErrorHighlight eh = hit.next();
                            Object obj = mGetObj.invoke(eh, c.getDatabase());
                            Cell objCell = eh.getCell(c.getDatabase());
                            System.out.println("   Cell: " + (objCell != null ? objCell.describe(false) : "null") + 
                                               " | Obj: " + (obj != null ? obj.getClass().getName() + " -> " + obj.toString() : "null"));
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
        System.exit(0);
    }
}
