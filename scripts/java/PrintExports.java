import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;

public class PrintExports {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintExports", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    
                    String[] cells = {"SRAM_Core_Datapath{lay}", "Decoder_4x16{lay}", "ShiftReg_8bit{lay}", "Div8_Counter{lay}", "sense_amplifier{lay}", "write_driver{lay}", "inverter{lay}", "nand4{lay}"};
                    for (String name : cells) {
                        Cell c = lib.findNodeProto(name);
                        if (c != null) {
                            System.out.println("=== " + name + " (Bounds: " + c.getBounds() + ") ===");
                            for (Iterator<Export> it = c.getExports(); it.hasNext(); ) {
                                Export exp = it.next();
                                PortInst pi = exp.getOriginalPort();
                                Point2D pt = pi.getCenter();
                                System.out.println("   " + exp.getName() + " -> " + pt + " (node: " + pi.getNodeInst().getProto().getName() + ")");
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
        Thread.sleep(3000);
    }
}
