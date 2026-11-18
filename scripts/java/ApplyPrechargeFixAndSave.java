
import com.sun.electric.Main;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.User;
import com.sun.electric.util.math.Orientation;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.*;

public class ApplyPrechargeFixAndSave {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ApplyPrechargeFixAndSave", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("Precharge_Bank_16{lay}");

                    NodeInst oldSub = null;
                    for (Iterator<NodeInst> it = c.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getName().equals("substr@0") || ni.getName().equals("pin@0")) {
                            oldSub = ni;
                            break;
                        }
                    }

                    for (Iterator<Export> it = c.getExports(); it.hasNext(); ) {
                        Export exp = it.next();
                        if (exp.getName().equals("gnd")) {
                            exp.kill();
                            break;
                        }
                    }
                    if (oldSub != null) oldSub.kill();

                    PrimitiveNode proto = (PrimitiveNode) c.getTechnology().findNodeProto("Metal-1-Pin");
                    Point2D pt = new Point2D.Double(-60.0, -20.0);
                    NodeInst newPin = NodeInst.makeInstance(proto, getEditingPreferences(), pt, 3.0, 3.0, c, Orientation.IDENT, "pin@0");
                    PortInst pInst = newPin.getPortInst(0);
                    Export.newInstanceNoIcon(c, pInst, "gnd", getEditingPreferences(), com.sun.electric.database.prototype.PortCharacteristic.GND);

                    System.out.println("Precharge_Bank_16 pin@0 replaced successfully!");

                    // Save library
                    Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                    System.out.println("Output.writeLibrary completed successfully!");

                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
        System.exit(0);
    }
}
