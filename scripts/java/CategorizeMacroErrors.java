
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
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

public class CategorizeMacroErrors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("CategorizeMacroErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("SRAM_256_Macro{lay}");

                    for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); ) {
                        Cell ac = cit.next();
                        List<Variable> toRemove = new ArrayList<Variable>();
                        for (Iterator<Variable> vit = ac.getVariables(); vit.hasNext(); ) {
                            Variable v = vit.next();
                            if (v.getKey().getName().toUpperCase().contains("DRC")) toRemove.add(v);
                        }
                        for (Variable v : toRemove) ac.delVar(v.getKey());
                    }
                    DRC.resetDRCDates(true);

                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.ignoreAreaCheck = false;
                    dp.ignoreExtensionRuleChecking = false;
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;

                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    Quick.checkDesignRules(errLog, c, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    Map<String, Integer> counts = new HashMap<String, Integer>();
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        String msg = it.next().getMessageString();
                        // group message
                        String key = msg;
                        int idx = msg.indexOf(":");
                        if (idx > 0) key = msg.substring(0, idx) + " -> " + msg.substring(idx+1).replaceAll("at .*$", "").trim();
                        counts.put(key, counts.getOrDefault(key, 0) + 1);
                    }
                    System.out.println("Macro DRC Error Categories (" + errLog.getNumErrors() + " total):");
                    for (Map.Entry<String, Integer> e : counts.entrySet()) {
                        System.out.println("  [" + e.getValue() + "x] " + e.getKey());
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
