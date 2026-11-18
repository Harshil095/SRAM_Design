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
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class LocateMacroErrors {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("LocateMacroErrors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
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

                    int count = 0;
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        count++;
                        if (count <= 40) {
                            System.out.println("#" + count + " " + ml.getMessageString());
                            for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                                ErrorHighlight eh = hit.next();
                                try {
                                    Method mGetObj = ErrorHighlight.class.getDeclaredMethod("getObject", EDatabase.class);
                                    mGetObj.setAccessible(true);
                                    Object obj = mGetObj.invoke(eh, c.getDatabase());
                                    System.out.println("   Obj: " + obj);
                                } catch (Exception ignored) {}
                                try {
                                    Field fPoint = eh.getClass().getDeclaredField("point");
                                    fPoint.setAccessible(true);
                                    System.out.println("   Point: " + fPoint.get(eh));
                                } catch (Exception ignored) {}
                                try {
                                    Field fP1 = eh.getClass().getDeclaredField("p1");
                                    fP1.setAccessible(true);
                                    Field fP2 = eh.getClass().getDeclaredField("p2");
                                    fP2.setAccessible(true);
                                    System.out.println("   Line: " + fP1.get(eh) + " -> " + fP2.get(eh));
                                } catch (Exception ignored) {}
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
        Thread.sleep(5000);
        System.exit(0);
    }
}