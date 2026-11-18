import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.JELIB;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class VerifySRAMFinal {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("VerifySRAMFinal", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM_final.jelib");
                    System.out.println("Reading library: " + f.getAbsolutePath());

                    // Apply project settings saved in the JELIB header
                    Map<Setting, Object> settings = JELIB.readProjectSettings(f.toURI().toURL(), FileType.JELIB, TechPool.getThreadTechPool(), null);
                    if (settings != null && !settings.isEmpty()) {
                        Setting.SettingChangeBatch scb = new Setting.SettingChangeBatch();
                        for (Map.Entry<Setting, Object> e : settings.entrySet()) {
                            scb.add(e.getKey(), e.getValue());
                            System.out.println("Applying file setting: " + e.getKey().getPrefName() + " = " + e.getValue());
                        }
                        EDatabase.currentDatabase().implementSettingChanges(scb);
                    }

                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    System.out.println("Loaded library name: " + lib.getName());

                    Technology mocmos = Technology.findTechnology("mocmos");
                    double scale = mocmos.getScale();
                    System.out.println("Technology scale in database: " + scale + " nm");

                    // Check external libraries
                    Set<String> extLibNames = new TreeSet<>();
                    for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                        Cell c = it.next();
                        for (Iterator<NodeInst> nit = c.getNodes(); nit.hasNext(); ) {
                            NodeInst ni = nit.next();
                            if (ni.getProto() instanceof Cell) {
                                Cell sub = (Cell) ni.getProto();
                                if (sub.getLibrary() != lib) {
                                    extLibNames.add(sub.getLibrary().getName());
                                }
                            }
                        }
                    }
                    System.out.println("External libraries referenced: " + extLibNames.size());
                    for (String el : extLibNames) {
                        System.out.println("  EXTERNAL: " + el);
                    }

                    // Run DRC on SRAM_Chip_Final and SRAM_256_Macro
                    Method m = DRC.class.getDeclaredMethod("checkDRCHierarchically", DRC.DRCPreferences.class, Cell.class, List.class, java.awt.geom.Rectangle2D.class, GeometryHandler.GHMode.class, boolean.class);
                    m.setAccessible(true);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;

                    Cell chip = lib.findNodeProto("SRAM_Chip_Final{lay}");
                    System.out.println("\nChecking DRC on SRAM_Chip_Final{lay} (Sweep Mode)...");
                    ErrorLogger errLogBefore = DRC.getDRCErrorLogger(false, null);
                    int before = (errLogBefore != null) ? errLogBefore.getNumErrors() : 0;
                    m.invoke(null, dp, chip, null, null, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    Thread.sleep(4000);
                    ErrorLogger errLogAfter = DRC.getDRCErrorLogger(false, null);
                    int after = (errLogAfter != null) ? errLogAfter.getNumErrors() : 0;
                    System.out.println(">>> SRAM_Chip_Final DRC Errors: " + (after - before));

                    Cell macro = lib.findNodeProto("SRAM_256_Macro{lay}");
                    System.out.println("\nChecking DRC on SRAM_256_Macro{lay} (Sweep Mode)...");
                    before = after;
                    m.invoke(null, dp, macro, null, null, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    Thread.sleep(4000);
                    errLogAfter = DRC.getDRCErrorLogger(false, null);
                    after = (errLogAfter != null) ? errLogAfter.getNumErrors() : 0;
                    System.out.println(">>> SRAM_256_Macro DRC Errors: " + (after - before));

                    // NCC Check
                    System.out.println("\nRunning Hierarchical NCC Checks...");
                    String[] cellsToNcc = {"6T", "Precharge_Bank_16", "Col_Mux_Bank_16", "Decoder_4x16", "Div8_Counter", "ShiftReg_8bit", "SRAM_Array_16x16", "SRAM_Core_Datapath", "SRAM_256_Macro", "SRAM_Chip_Final"};
                    boolean allMatch = true;
                    for (String cname : cellsToNcc) {
                        Cell lay = lib.findNodeProto(cname + "{lay}");
                        Cell sch = lib.findNodeProto(cname + "{sch}");
                        if (lay == null || sch == null) {
                            System.out.println("Cell " + cname + " missing layout or schematic!");
                            allMatch = false;
                            continue;
                        }
                        NccResults res = Ncc.compare(lay, null, sch, null, new NccOptions());
                        boolean match = res.match();
                        System.out.println("NCC " + (match ? "MATCH: " : "MISMATCH: ") + cname);
                        if (!match) allMatch = false;
                    }
                    System.out.println("\n>>> ALL NCC CHECKS PASS: " + allMatch);

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
