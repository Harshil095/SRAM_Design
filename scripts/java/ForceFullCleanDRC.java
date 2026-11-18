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

public class ForceFullCleanDRC {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ForceFullCleanDRC", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    
                    // Purge ALL DRC variables from ALL cells
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

                    String[] testCells = {"DFF{lay}", "ShiftReg_8bit{lay}", "Div8_Counter{lay}", "Decoder_4x16{lay}", "SRAM_Array_16x16{lay}", "SRAM_Core_Datapath{lay}"};
                    for (String name : testCells) {
                        Cell c = lib.findNodeProto(name);
                        ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                        dp.ignoreAreaCheck = false;
                        dp.ignoreExtensionRuleChecking = false;
                        dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                        Quick.checkDesignRules(errLog, c, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, true);
                        errLog.termLogging(true);
                        System.out.println(name + " Truly Forced DRC: " + errLog.getNumErrors() + " errors, " + errLog.getNumWarnings() + " warnings");
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
