
import com.sun.electric.Main;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.LibrarySave;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.util.*;

public class FixPrechargePin {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("FixPrechargePin", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell cell = lib.findNodeProto("Precharge_Bank_16{lay}");
                    
                    Technology mocmos = Technology.findTechnology("mocmos");
                    PrimitiveNode m1PinProto = (PrimitiveNode) mocmos.findNodeProto("Metal-1-Pin");
                    
                    NodeInst oldSubstr = null;
                    for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getName().equals("substr@0")) {
                            oldSubstr = ni;
                            break;
                        }
                    }
                    
                    if (oldSubstr != null) {
                        System.out.println("Found old substr@0 at " + oldSubstr.getTrueCenter());
                        
                        // Remove export gnd
                        Export gndExp = cell.findExport("gnd");
                        if (gndExp != null) {
                            System.out.println("Deleting export gnd");
                            gndExp.delete();
                        }
                        
                        // Delete oldSubstr
                        oldSubstr.delete();
                        
                        // Create new Metal-1-Pin at (-60.0, -20.0)
                        NodeInst newPin = NodeInst.makeInstance(m1PinProto, new EPoint(-60.0, -20.0), 3.0, 3.0, Orientation.IDENT, cell, "pin@0");
                        System.out.println("Created new pin@0: " + newPin.getName() + " at " + newPin.getTrueCenter());
                        
                        // Export gnd on newPin port metal-1
                        PortInst pi = newPin.getPortInst(m1PinProto.findPortProto("metal-1"));
                        Export newExp = Export.newInstance(cell, pi, "gnd");
                        System.out.println("Exported gnd: " + newExp);
                        
                        // Save library
                        LibrarySave.writeLibrary(lib, f, null, FileType.JELIB, false);
                        System.out.println("Successfully saved SRAM.jelib!");
                    } else {
                        System.out.println("substr@0 not found!");
                    }
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
