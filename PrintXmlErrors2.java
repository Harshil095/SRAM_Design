import java.io.File;
import java.net.URL;
import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.awt.geom.Point2D;

public class PrintXmlErrors2 {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintXmlErrors2", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell layData = lib.findNodeProto("SRAM_Core_Datapath{lay}");
                    EditingPreferences ep = getEditingPreferences();
                    
                    Cell test = Cell.makeInstance(lib, "TestOnlyDatapath{lay}");
                    NodeInst dInst = NodeInst.makeInstance(layData, new Point2D.Double(0.0, 0.0), 0, 0, test);

                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;

                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, test, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    int count = 0;
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        count++;
                        System.out.println("<!-- ERROR #" + count + ": " + ml.getMessageString() + " -->");
                        java.lang.reflect.Method m = ErrorLogger.MessageLog.class.getDeclaredMethod("writeXmlDescription", java.io.PrintStream.class);
                        m.setAccessible(true);
                        m.invoke(ml, System.out);
                        if (count >= 5) break;
                    }
                    test.kill();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
    }
}
