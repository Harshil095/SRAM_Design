import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.awt.geom.Point2D;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class VerifyRealGUI_DRC {

    public static void auditCellDRC(Library lib, String cellName) {
        Cell cell = lib.findNodeProto(cellName);
        if (cell == null) {
            System.out.println("ERROR: Cell not found in library: " + cellName);
            return;
        }

        Technology mocmos = Technology.findTechnology("mocmos");
        System.out.println("================================================================================");
        System.out.println("DRC AUDIT FOR CELL: " + cell.getName() + " [Version: " + cell.getVersion() + ", View: " + cell.getView() + "]");
        System.out.println("Active mocmos Scale: " + mocmos.getScale() + " nm (Lambda = 90.0nm)");
        System.out.println("Database Scale Setting: " + mocmos.getScaleSetting().getDouble() + " nm");

        // Force full polygon DRC evaluation by resetting any in-memory dates
        DRC.resetDRCDates(true);

        // Instantiate ErrorLogger
        ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);

        // Configure exact GUI DRC preferences:
        // Area Bit ON, Extension Bit ON, MOSIS Bit ON (Exhaustive mode)
        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
        dp.ignoreAreaCheck = false;                             // Area bit ON
        dp.ignoreExtensionRuleChecking = false;                 // Extension bit ON
        dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE; // MOSIS bit ON
        dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;

        // 1. Run Network Short/Topology checks as GUI does
        try {
            Method mCheckNet = DRC.class.getDeclaredMethod("checkNetworks", ErrorLogger.class, Cell.class, boolean.class);
            mCheckNet.setAccessible(true);
            mCheckNet.invoke(null, errLog, cell, true);
        } catch (Exception e) {
            System.out.println("Note: checkNetworks reflection: " + e.getMessage());
        }

        // 2. Run Geometric DRC engine
        Quick.checkDesignRules(errLog, cell, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
        errLog.termLogging(true);

        int totalErrors = errLog.getNumErrors();
        int totalWarnings = errLog.getNumWarnings();
        System.out.println(">>> DRC SUMMARY for " + cellName + ": " + totalErrors + " ERRORS, " + totalWarnings + " WARNINGS <<<");

        if (totalErrors == 0 && totalWarnings == 0) {
            System.out.println("STATUS: CLEAN! Zero DRC violations detected.");
        } else {
            System.out.println("--- DETAILED ERROR LOG (Extracted from ErrorLogger) ---");
            int idx = 0;
            for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                ErrorLogger.MessageLog ml = it.next();
                idx++;
                StringBuilder geoDetails = new StringBuilder();

                for (Iterator<ErrorHighlight> hit = ml.getHighlights(); hit.hasNext(); ) {
                    ErrorHighlight eh = hit.next();
                    try {
                        Method mGetObj = ErrorHighlight.class.getDeclaredMethod("getObject", EDatabase.class);
                        mGetObj.setAccessible(true);
                        Object obj = mGetObj.invoke(eh, cell.getDatabase());
                        if (obj instanceof NodeInst) {
                            NodeInst ni = (NodeInst) obj;
                            geoDetails.append(" Node[").append(ni.getProto().getName()).append(" @ ").append(ni.getAnchorCenter()).append("]");
                        } else if (obj instanceof ArcInst) {
                            ArcInst ai = (ArcInst) obj;
                            geoDetails.append(" Arc[").append(ai.getProto().getName()).append(" (").append(ai.getHeadLocation()).append("->").append(ai.getTailLocation()).append(")]");
                        }
                    } catch (Exception ignored) {}

                    try {
                        Field fPoint = eh.getClass().getDeclaredField("point");
                        fPoint.setAccessible(true);
                        geoDetails.append(" Point[").append(fPoint.get(eh)).append("]");
                    } catch (Exception ignored) {}

                    try {
                        Field fP1 = eh.getClass().getDeclaredField("p1");
                        fP1.setAccessible(true);
                        Field fP2 = eh.getClass().getDeclaredField("p2");
                        fP2.setAccessible(true);
                        geoDetails.append(" Line[").append(fP1.get(eh)).append("->").append(fP2.get(eh)).append("]");
                    } catch (Exception ignored) {}
                }

                System.out.println("  #" + idx + " [Group " + ml.getSortKey() + "] " + ml.getMessageString() + " | Targets:" + geoDetails);
            }
        }
        System.out.println("================================================================================\n");
    }

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("VerifyRealGUI_DRC", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Map<Setting, Object> settings = new HashMap<Setting, Object>();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false, settings);

                    System.out.println("=== 1. Library Scale & Settings Verification ===");
                    System.out.println("Library URL: " + url);
                    System.out.println("Settings read directly from jelib: " + settings);

                    EDatabase db = lib.getDatabase();
                    Setting.SettingChangeBatch scb = new Setting.SettingChangeBatch();
                    Technology mocmos = Technology.findTechnology("mocmos");
                    scb.add(mocmos.getScaleSetting(), Double.valueOf(90.0));
                    db.implementSettingChanges(scb);

                    System.out.println("Active Technology mocmos Scale: " + mocmos.getScale());

                    System.out.println("\n=== 2. Cell Revision and DRC Cache Verification ===");
                    int totalDrcVars = 0;
                    int totalCells = 0;
                    for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); ) {
                        Cell c = cit.next();
                        totalCells++;
                        if (c.getVersion() != 1) {
                            System.out.println("WARNING: Cell " + c.getName() + " has version " + c.getVersion() + " (expected 1)");
                        }
                        for (Iterator<Variable> vit = c.getVariables(); vit.hasNext(); ) {
                            Variable v = vit.next();
                            if (v.getKey().getName().contains("DRC") || v.getKey().getName().contains("drc")) {
                                totalDrcVars++;
                            }
                        }
                    }
                    System.out.println("Total Cells in Library: " + totalCells);
                    System.out.println("Total Cached DRC Variables in Library: " + totalDrcVars);

                    System.out.println("\n=== 3. Running Ground-Truth DRC Audits ===");
                    auditCellDRC(lib, "SRAM_256_Macro{lay}");
                    auditCellDRC(lib, "SRAM_Chip_Final{lay}");

                    System.out.println("=== 4. Running Hierarchical NCC Verification ===");
                    Cell chipSch = lib.findNodeProto("SRAM_Chip_Final{sch}");
                    Cell chipLay = lib.findNodeProto("SRAM_Chip_Final{lay}");
                    if (chipSch != null && chipLay != null) {
                        NccOptions opts = new NccOptions();
                        opts.operation = NccOptions.HIER_EACH_CELL;
                        NccResults res = Ncc.compare(chipSch, null, chipLay, null, opts);
                        System.out.println("NCC Comparison: SRAM_Chip_Final{sch} vs SRAM_Chip_Final{lay}");
                        System.out.println("  Overall Match:   " + res.match());
                        System.out.println("  Exports Match:   " + res.exportMatch());
                        System.out.println("  Topology Match:  " + res.topologyMatch());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(10000);
    }
}
