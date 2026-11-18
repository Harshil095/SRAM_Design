import java.util.*;
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

public class CategorizeMacroDRC {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("CategorizeMacroDRC", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
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
                    
                    Map<String, Integer> counts = new HashMap<>();
                    Map<String, List<String>> samples = new HashMap<>();
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        String msg = it.next().getMessageString();
                        String prefix = msg.split(":")[0];
                        if (msg.contains("rule")) {
                            prefix = msg.substring(0, msg.indexOf("[rule"));
                        }
                        // simplify prefix
                        String cat = prefix.replaceAll("arc Metal-[123]\\['[^']*'\\]", "arc M")
                                           .replaceAll("node [^']*\\['[^']*'\\]", "node Contact")
                                           .replaceAll("cell '[^']*'", "cell")
                                           .trim();
                        counts.put(cat, counts.getOrDefault(cat, 0) + 1);
                        samples.computeIfAbsent(cat, k -> new ArrayList<>()).add(msg);
                    }
                    
                    System.out.println("Total Errors: " + errLog.getNumErrors());
                    for (Map.Entry<String, Integer> e : counts.entrySet()) {
                        System.out.println("--- Count: " + e.getValue() + " | Category: " + e.getKey());
                        for (int i = 0; i < Math.min(3, e.getValue()); i++) {
                            System.out.println("     " + samples.get(e.getKey()).get(i));
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
