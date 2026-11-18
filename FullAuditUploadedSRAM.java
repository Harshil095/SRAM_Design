import java.io.File;
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
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class FullAuditUploadedSRAM {

    static void audit(Cell cell) {
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

        int errs = errLog.getNumErrors();
        int warns = errLog.getNumWarnings();
        System.out.printf("%-25s : %2d errors, %2d warnings%n", cell.getName(), errs, warns);
        if (errs > 0 || warns > 0) {
            for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                System.out.println("     -> " + it.next().getMessageString());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("FullAuditUploadedSRAM", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795002561961_7f7dd016.jelib";
                    Map<Setting, Object> s = new HashMap<>();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p).toURI().toURL(), null, FileType.JELIB, false, s);

                    EDatabase db = lib.getDatabase();
                    Setting.SettingChangeBatch scb = new Setting.SettingChangeBatch();
                    Technology mocmos = Technology.findTechnology("mocmos");
                    scb.add(mocmos.getScaleSetting(), Double.valueOf(90.0));
                    db.implementSettingChanges(scb);

                    System.out.println("=== AUDIT AT SCALE 90.0 nm (MOSIS 180nm) ===");
                    List<String> cellNames = new ArrayList<>();
                    for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                        Cell c = it.next();
                        if (c.getView() == com.sun.electric.database.hierarchy.View.LAYOUT) {
                            cellNames.add(c.getName());
                        }
                    }
                    Collections.sort(cellNames);
                    for (String name : cellNames) {
                        Cell c = lib.findNodeProto(name + "{lay}");
                        audit(c);
                    }

                    System.out.println("\n=== HIERARCHICAL LVS / NCC AUDIT ===");
                    Cell chipSch = lib.findNodeProto("SRAM_Chip_Final{sch}");
                    Cell chipLay = lib.findNodeProto("SRAM_Chip_Final{lay}");
                    if (chipSch != null && chipLay != null) {
                        NccOptions opts = new NccOptions();
                        opts.operation = NccOptions.HIER_EACH_CELL;
                        NccResults res = Ncc.compare(chipSch, null, chipLay, null, opts);
                        System.out.println("SRAM_Chip_Final NCC match: " + res.match());
                        System.out.println("  exportsMatch: " + res.exportMatch());
                        System.out.println("  topologyMatch: " + res.topologyMatch());
                    }

                    Cell macroSch = lib.findNodeProto("SRAM_256_Macro{sch}");
                    Cell macroLay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    if (macroSch != null && macroLay != null) {
                        NccOptions opts = new NccOptions();
                        opts.operation = NccOptions.HIER_EACH_CELL;
                        NccResults res = Ncc.compare(macroSch, null, macroLay, null, opts);
                        System.out.println("SRAM_256_Macro NCC match: " + res.match());
                        System.out.println("  exportsMatch: " + res.exportMatch());
                        System.out.println("  topologyMatch: " + res.topologyMatch());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(8000);
    }
}
