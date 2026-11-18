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
import java.util.Iterator;

public class VerifyDFF {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("VerifyDFFJob", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
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
                    
                    Cell lay = lib.findNodeProto("DFF{lay}");
                    Cell sch = lib.findNodeProto("DFF{sch}");
                    
                    if (lay == null) {
                        System.out.println("Cell DFF{lay} NOT FOUND");
                        return true;
                    }
                    
                    ERectangle b = lay.getBounds();
                    Rectangle2D r = b != null ? b.getBounds2D() : new Rectangle2D.Double(0,0,0,0);
                    System.out.println("=== DFF{lay} Bounds ===");
                    System.out.println("Bounds: " + r);
                    System.out.println("Width: " + r.getWidth() + " lambda (" + (r.getWidth() * 0.09) + " um)");
                    System.out.println("Height: " + r.getHeight() + " lambda (" + (r.getHeight() * 0.09) + " um)");
                    
                    System.out.println("\n=== DRC Check ===");
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println("DRC Errors: " + errLog.getNumErrors() + ", Warnings: " + errLog.getNumWarnings());
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog em = it.next();
                        System.out.println("DRC ERROR: " + em.getMessage());
                    }
                    
                    if (sch != null) {
                        System.out.println("\n=== NCC Check ===");
                        NccOptions opts = new NccOptions();
                        opts.checkSizes = true;
                        NccResults res = Ncc.compare(sch, null, lay, null, opts);
                        System.out.println("NCC match=" + res.match());
                        System.out.println("Summary: " + res.summary(false));
                        System.out.println("Export match=" + res.exportMatch());
                        System.out.println("Topology match=" + res.topologyMatch());
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(2500);
    }
}
