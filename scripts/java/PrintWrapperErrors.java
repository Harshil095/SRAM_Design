import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;

public class PrintWrapperErrors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintWrapperErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
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
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, test, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        System.out.println(it.next().getMessageString());
                    }
                    test.kill();
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
