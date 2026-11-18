import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.*;

public class DumpAllMacroErrors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DumpAllMacroErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("SRAM_256_Macro{lay}");

                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.ignoreAreaCheck = false;
                    dp.ignoreExtensionRuleChecking = false;
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    Quick.checkDesignRules(errLog, c, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, true);
                    errLog.termLogging(true);

                    Method mXml = ErrorHighlight.class.getDeclaredMethod("writeXmlDescription", String.class, PrintStream.class, EDatabase.class);
                    mXml.setAccessible(true);

                    FileWriter fw = new FileWriter("all_macro_errors.txt");
                    int count = 0;
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        count++;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintStream ps = new PrintStream(baos);
                        for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                            ErrorHighlight eh = hit.next();
                            mXml.invoke(eh, "  ", ps, c.getDatabase());
                        }
                        fw.write("#" + count + " " + ml.getMessageString() + "\n" + baos.toString() + "\n");
                    }
                    fw.close();
                    System.out.println("Wrote " + count + " errors to all_macro_errors.txt");
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
