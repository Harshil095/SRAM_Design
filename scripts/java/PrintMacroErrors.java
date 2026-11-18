import java.io.File;
import java.net.URL;
import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class PrintMacroErrors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintMacroErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell macroLay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    DRC.resetDRCDates(true);

                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, macroLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    System.out.println(">>> Errors in existing SRAM_256_Macro (" + errLog.getNumErrors() + "): <<<");
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        System.out.println(it.next().getMessageString());
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
