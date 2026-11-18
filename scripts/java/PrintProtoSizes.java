
import java.io.File;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class PrintProtoSizes {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintProtoSizes", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    Technology mocmos = Technology.findTechnology("mocmos");
                    NodeProto via1 = mocmos.findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via23 = mocmos.findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto pcon = mocmos.findNodeProto("Metal-1-Polysilicon-1-Con");
                    NodeProto pin = mocmos.findNodeProto("Metal-1-Pin");
                    System.out.println("via1: " + via1.getDefWidth(ep) + " x " + via1.getDefHeight(ep));
                    System.out.println("via23: " + via23.getDefWidth(ep) + " x " + via23.getDefHeight(ep));
                    System.out.println("pcon: " + pcon.getDefWidth(ep) + " x " + pcon.getDefHeight(ep));
                    System.out.println("pin: " + pin.getDefWidth(ep) + " x " + pin.getDefHeight(ep));
                } catch (Exception e) { e.printStackTrace(); }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(2000);
    }
}
