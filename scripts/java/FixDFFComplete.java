import java.io.File;
import java.net.URL;
import java.util.Iterator;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class FixDFFComplete {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("FixDFFComplete", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell dffLay = lib.findNodeProto("DFF{lay}");
                    Cell dffSch = lib.findNodeProto("DFF{sch}");

                    int fixed = 0;
                    for (Iterator it = dffLay.getArcs(); it.hasNext(); ) {
                        ArcInst ai = (ArcInst) it.next();
                        if (ai.getProto().getName().equals("Metal-1")) {
                            double y1 = ai.getHeadLocation().getY();
                            double y2 = ai.getTailLocation().getY();
                            if (Math.abs(y1 - 10.0) < 3.0 || Math.abs(y2 - 10.0) < 3.0) {
                                ai.setLambdaBaseWidth(5.0);
                                fixed++;
                            }
                            if (Math.abs(y1 - 22.0) < 1.0 && Math.abs(y2 - 22.0) < 1.0) {
                                ai.setLambdaBaseWidth(5.0);
                                fixed++;
                            }
                        }
                    }
                    System.out.println("Updated " + fixed + " arcs in DFF{lay} to width 5.0");

                    // Check DRC on DFF
                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, dffLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println(">>> DFF{lay} DRC Errors: " + errLog.getNumErrors() + " <<<");
                    for (Iterator it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                        System.out.println("  ERROR: " + ml.getMessageString());
                    }

                    // Check NCC on DFF
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults nr = Ncc.compare(dffSch, null, dffLay, null, opts);
                    System.out.println(">>> DFF NCC: " + (nr != null && nr.match() ? "PASS" : "FAIL") + " <<<");

                    if (errLog.getNumErrors() == 0 && nr != null && nr.match()) {
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("SUCCESSFULLY saved DFF{lay} in SRAM.jelib!");
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
