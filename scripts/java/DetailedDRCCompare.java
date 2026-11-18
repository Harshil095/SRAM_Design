import java.io.File;
import java.net.URL;
import java.util.*;
import java.lang.reflect.Method;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class DetailedDRCCompare {

    static int runDRC(Cell cell) {
        DRC.resetDRCDates(true);
        ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
        dp.ignoreAreaCheck = false;
        dp.ignoreExtensionRuleChecking = false;
        dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
        dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;

        try {
            Method mCheckNet = DRC.class.getDeclaredMethod("checkNetworks", ErrorLogger.class, Cell.class, boolean.class);
            mCheckNet.setAccessible(true);
            mCheckNet.invoke(null, errLog, cell, true);
        } catch (Exception ignored) {}

        Quick.checkDesignRules(errLog, cell, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
        errLog.termLogging(true);
        return errLog.getNumErrors();
    }

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DetailedDRCCompare", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String friendPath = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795001289210_12b2d076.jelib";
                    
                    System.out.println("=== 1. LOADING LIBRARIES ===");
                    Map<Setting, Object> friendSettings = new HashMap<>();
                    Library friendLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(friendPath).toURI().toURL(), null, FileType.JELIB, false, friendSettings);
                    
                    Map<Setting, Object> userSettings = new HashMap<>();
                    Library userV1Lib = LibraryFiles.readLibrary(getEditingPreferences(), new File("SRAM_Complete_V1.jelib").toURI().toURL(), null, FileType.JELIB, false, userSettings);

                    Technology mocmos = Technology.findTechnology("mocmos");
                    System.out.println("Active mocmos scale: " + mocmos.getScale());

                    System.out.println("\n=== 2. RUNNING CELL-BY-CELL DRC COMPARISON ===");
                    System.out.printf("%-30s | %-15s | %-15s%n", "Cell Name", "User V1 Errors", "Friend Errors");
                    System.out.println("----------------------------------------------------------------------");

                    List<String> layoutCellNames = new ArrayList<>();
                    for (Iterator<Cell> it = friendLib.getCells(); it.hasNext(); ) {
                        Cell c = it.next();
                        if (c.getView() == com.sun.electric.database.hierarchy.View.LAYOUT) {
                            layoutCellNames.add(c.getName());
                        }
                    }
                    Collections.sort(layoutCellNames);

                    for (String name : layoutCellNames) {
                        Cell fc = friendLib.findNodeProto(name + "{lay}");
                        Cell uc = userV1Lib.findNodeProto(name + "{lay}");

                        int fErr = (fc != null) ? runDRC(fc) : -1;
                        int uErr = (uc != null) ? runDRC(uc) : -1;

                        System.out.printf("%-30s | %-15s | %-15s%n", 
                            name, 
                            (uErr >= 0 ? String.valueOf(uErr) : "N/A"), 
                            (fErr >= 0 ? String.valueOf(fErr) : "N/A")
                        );
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(15000);
    }
}
