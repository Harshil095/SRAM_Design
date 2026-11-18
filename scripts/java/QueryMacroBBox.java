import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.URL;

public class QueryMacroBBox {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("QueryMacroBBox", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("SRAM_256_Macro{lay}");
                    if (c != null) {
                        Rectangle2D bb = c.getBounds();
                        System.out.printf("SRAM_256_Macro{lay} BBox: [minX=%.1f, maxX=%.1f, minY=%.1f, maxY=%.1f]%n",
                            bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY());
                        System.out.printf("Width = %.1f lambda, Height = %.1f lambda, Area = %.2f M lambda^2%n",
                            bb.getWidth(), bb.getHeight(), (bb.getWidth() * bb.getHeight()) / 1e6);
                    } else {
                        System.out.println("Cell SRAM_256_Macro{lay} not found!");
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
