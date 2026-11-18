import java.io.File;
import java.net.URL;
import java.util.*;
import java.lang.reflect.Method;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class CreateSRAMFinal {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("CreateSRAMFinal", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String userPath = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795002561961_7f7dd016.jelib";
                    String friendPath = "/Users/harshil_rathan/Downloads/SRAM_Complete_V1_suraj_antigravity_BACKUP_DRC0_LVS0.jelib";

                    System.out.println("=== 1. LOADING LIBRARIES ===");
                    Map<Setting, Object> userSettings = new HashMap<>();
                    Library userLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(userPath).toURI().toURL(), null, FileType.JELIB, false, userSettings);

                    Map<Setting, Object> friendSettings = new HashMap<>();
                    Library friendLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(friendPath).toURI().toURL(), null, FileType.JELIB, false, friendSettings);

                    // Create new library SRAM_final
                    EDatabase db = userLib.getDatabase();
                    Library finalLib = Library.newInstance(db, "SRAM_final", null);

                    // Set mocmos scale to 90.0 nm
                    Setting.SettingChangeBatch scb = new Setting.SettingChangeBatch();
                    Technology mocmos = Technology.findTechnology("mocmos");
                    scb.add(mocmos.getScaleSetting(), Double.valueOf(90.0));
                    db.implementSettingChanges(scb);

                    System.out.println("=== 2. COPYING ALL USER CELLS TO SRAM_final ===");
                    // Copy all cells from userLib into finalLib
                    for (Iterator<Cell> it = userLib.getCells(); it.hasNext(); ) {
                        Cell c = it.next();
                        Cell copy = c.duplicate(finalLib, c.getName());
                    }

                    // Fix orphan pin in Precharge_Bank_16{lay}
                    Cell pre = finalLib.findNodeProto("Precharge_Bank_16{lay}");
                    if (pre != null) {
                        List<NodeInst> toRemove = new ArrayList<>();
                        for (Iterator<NodeInst> it = pre.getNodes(); it.hasNext(); ) {
                            NodeInst ni = it.next();
                            if (ni.getProto().getName().contains("Pin") && ni.getNumConnections() == 0) {
                                toRemove.add(ni);
                            }
                        }
                        for (NodeInst ni : toRemove) {
                            System.out.println("Removing orphan pin: " + ni.getName() + " from Precharge_Bank_16{lay}");
                            ni.kill();
                        }
                    }

                    // Save library to SRAM_final.jelib
                    File outFile = new File("SRAM_final.jelib");
                    finalLib.setFile(outFile.toURI().toURL(), FileType.JELIB);
                    Output.save(finalLib, getEditingPreferences(), null);
                    System.out.println("Successfully created and saved: " + outFile.getAbsolutePath());

                    // Audit SRAM_final.jelib
                    System.out.println("\n=== 3. AUDITING SRAM_final.jelib ===");
                    Cell chip = finalLib.findNodeProto("SRAM_Chip_Final{lay}");
                    if (chip != null) {
                        DRC.resetDRCDates(true);
                        ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                        dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
                        dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                        Quick.checkDesignRules(errLog, chip, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                        errLog.termLogging(true);
                        System.out.println("SRAM_Chip_Final Errors in SRAM_final: " + errLog.getNumErrors() + ", Warnings: " + errLog.getNumWarnings());
                    }

                    Cell macro = finalLib.findNodeProto("SRAM_256_Macro{lay}");
                    if (macro != null) {
                        DRC.resetDRCDates(true);
                        ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                        dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
                        dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                        Quick.checkDesignRules(errLog, macro, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                        errLog.termLogging(true);
                        System.out.println("SRAM_256_Macro Errors in SRAM_final: " + errLog.getNumErrors() + ", Warnings: " + errLog.getNumWarnings());
                    }

                    // Run NCC
                    Cell chipSch = finalLib.findNodeProto("SRAM_Chip_Final{sch}");
                    Cell chipLay = finalLib.findNodeProto("SRAM_Chip_Final{lay}");
                    if (chipSch != null && chipLay != null) {
                        NccOptions opts = new NccOptions();
                        opts.operation = NccOptions.HIER_EACH_CELL;
                        NccResults res = Ncc.compare(chipSch, null, chipLay, null, opts);
                        System.out.println("SRAM_Chip_Final NCC Match: " + res.match());
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
