import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.*;

public class SweepMuxDRC {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("SweepMuxDRC", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    Technology mocmos = Technology.findTechnology("mocmos");
                    Cell tgateLay = lib.findNodeProto("tgate{lay}");

                    for (double sep = 32.0; sep <= 42.0; sep += 1.0) {
                        Cell mux = Cell.makeInstance(lib, "test_mux_" + ((int)sep) + "{lay}");
                        double xL = -sep / 2.0;
                        double xR = sep / 2.0;
                        NodeInst tgL = NodeInst.makeInstance(tgateLay, new Point2D.Double(xL, 0.0), 0, 0, mux);
                        NodeInst tgR = NodeInst.makeInstance(tgateLay, new Point2D.Double(xR, 0.0), 0, 0, mux);

                        ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                        dp.ignoreAreaCheck = false;
                        dp.ignoreExtensionRuleChecking = false;
                        dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                        Quick.checkDesignRules(errLog, mux, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, false);
                        errLog.termLogging(true);
                        System.out.println("sep=" + sep + " (xL=" + xL + ", xR=" + xR + "): " + errLog.getNumErrors() + " errors");
                        if (errLog.getNumErrors() > 0 && errLog.getNumErrors() <= 4) {
                            for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                                System.out.println("   " + it.next().getMessageString());
                            }
                        }
                        mux.kill();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(5000);
        System.exit(0);
    }
}