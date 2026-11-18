import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;

public class SweepY {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("SweepY", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);

                    Cell layArray = lib.findNodeProto("SRAM_Array_16x16{lay}");
                    Cell layMux = lib.findNodeProto("Col_Mux_Bank_16{lay}");

                    double[] ys = {-500, -400, -300, -250, -200, -150, -120, -100, -80, -70, -60};
                    for (double y : ys) {
                        Cell testLay = Cell.makeInstance(lib, "Test_Y_" + (int)(-y) + "{lay}");
                        NodeInst instArray = NodeInst.makeInstance(layArray, new Point2D.Double(0, 0), 0, 0, testLay);
                        NodeInst instMux = NodeInst.makeInstance(layMux, new Point2D.Double(0, y), 0, 0, testLay);

                        ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                        com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, testLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                        errLog.termLogging(true);
                        System.out.println("Y=" + y + " -> " + errLog.getNumErrors() + " errors");
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
