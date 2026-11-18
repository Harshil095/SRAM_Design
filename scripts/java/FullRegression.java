import com.sun.electric.Main;
import com.sun.electric.database.Environment;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.URL;

public class FullRegression {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("FullRegressionJob", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    
                    Technology mocmos = Technology.getMocmosTechnology();
                    Setting s = mocmos.getScaleSetting();
                    Setting.SettingChangeBatch batch = new Setting.SettingChangeBatch();
                    batch.add(s, 90.0);
                    Environment env = EDatabase.serverDatabase().getEnvironment().withSettingChanges(batch);
                    EDatabase.serverDatabase().resize(env);
                    
                    if (EDatabase.clientDatabase() == null) {
                        EDatabase.setClientDatabase(EDatabase.serverDatabase());
                    } else {
                        EDatabase.clientDatabase().resize(env);
                    }
                    
                    String[] cells = {"tgate", "col_mux_1x", "write_driver", "sense_amplifier", "DFF"};
                    
                    System.out.println("=================================================");
                    System.out.println("          FULL SRAM REGRESSION REPORT            ");
                    System.out.println("=================================================");
                    
                    for (String name : cells) {
                        Cell lay = lib.findNodeProto(name + "{lay}");
                        Cell sch = lib.findNodeProto(name + "{sch}");
                        
                        System.out.println("\n-------------------------------------------------");
                        System.out.println("CELL: " + name);
                        System.out.println("-------------------------------------------------");
                        
                        if (lay == null) {
                            System.out.println("  Layout view: NOT FOUND");
                            continue;
                        }
                        
                        ERectangle b = lay.getBounds();
                        Rectangle2D r = b != null ? b.getBounds2D() : new Rectangle2D.Double(0,0,0,0);
                        System.out.printf("  Layout Dimensions: Width = %.1f lambda (%.2f um), Height = %.1f lambda (%.2f um)\n",
                                          r.getWidth(), r.getWidth() * 0.09, r.getHeight(), r.getHeight() * 0.09);
                        
                        ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                        com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                        errLog.termLogging(true);
                        System.out.println("  DRC Status: " + errLog.getNumErrors() + " errors, " + errLog.getNumWarnings() + " warnings");
                        
                        if (sch != null) {
                            NccOptions opts = new NccOptions();
                            opts.checkSizes = true;
                            NccResults res = Ncc.compare(sch, null, lay, null, opts);
                            System.out.println("  NCC LVS Status: " + (res.match() ? "PASS" : "FAIL") + " (" + res.summary(false) + ")");
                            System.out.println("    - Export Match: " + res.exportMatch());
                            System.out.println("    - Topology Match: " + res.topologyMatch());
                        } else {
                            System.out.println("  Schematic view: NOT FOUND");
                        }
                    }
                    System.out.println("\n=================================================");
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(3000);
    }
}
