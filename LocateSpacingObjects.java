import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.net.URL;

public class LocateSpacingObjects {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("LocateSpacingObjects", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
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
                    
                    Set<String> targetNames = new HashSet<>(Arrays.asList("contact@282", "contact@387", "contact@395", "contact@396", "net@448", "net@526", "net@486", "contact@388"));
                    for (Iterator<NodeInst> it = lay.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        for (String t : targetNames) {
                            if (ni.toString().contains(t)) {
                                System.out.println("Node " + t + " is " + ni.getProto().getName() + " at " + ni.getTrueCenter());
                            }
                        }
                    }
                    for (Iterator<ArcInst> it = lay.getArcs(); it.hasNext(); ) {
                        ArcInst ai = it.next();
                        for (String t : targetNames) {
                            if (ai.toString().contains(t)) {
                                System.out.println("Arc " + t + " is " + ai.getProto().getName() + " from " + ai.getHeadPortInst().getNodeInst().getTrueCenter() + " to " + ai.getTailPortInst().getNodeInst().getTrueCenter());
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
        Thread.sleep(3000);
    }
}
