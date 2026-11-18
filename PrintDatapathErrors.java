import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.util.*;

public class PrintDatapathErrors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintDatapathErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    
                    for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); ) {
                        Cell c = cit.next();
                        List<Variable> toRemove = new ArrayList<Variable>();
                        for (Iterator<Variable> vit = c.getVariables(); vit.hasNext(); ) {
                            Variable v = vit.next();
                            if (v.getKey().getName().toUpperCase().contains("DRC")) {
                                toRemove.add(v);
                            }
                        }
                        for (Variable v : toRemove) {
                            c.delVar(v.getKey());
                        }
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
                    System.out.println("SRAM_Core_Datapath DRC: " + errLog.getNumErrors() + " errors");
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        System.out.println("  " + it.next().getMessageString());
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
