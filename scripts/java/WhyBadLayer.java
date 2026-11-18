
import com.sun.electric.Main;
import com.sun.electric.database.geometry.bool.*;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;

public class WhyBadLayer {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("WhyBadLayer", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("Precharge_Bank_16{lay}");

                    // Let us check all nodes and arcs in pre_charge{lay} and Precharge_Bank_16{lay}
                    Cell pre = lib.findNodeProto("pre_charge{lay}");
                    LayoutMergerFactory factory = LayoutMergerFactory.getInstance();
                    Technology mocmos = Technology.findTechnology("mocmos");
                    Layer m1 = mocmos.findLayer("Metal-1");

                    System.out.println("canMerge pre_charge: " + factory.newMerger(pre).canMerge(m1));
                    System.out.println("canMerge Precharge_Bank_16: " + factory.newMerger(c).canMerge(m1));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(5000);
        System.exit(0);
    }
}
