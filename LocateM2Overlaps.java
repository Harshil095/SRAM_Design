import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.net.URL;

public class LocateM2Overlaps {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("LocateM2Overlaps", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell lay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    
                    Set<String> targets = new HashSet<>(Arrays.asList("net@446", "net@460", "contact@360", "net@503", "net@566", "net@510", "net@94", "net@414", "net@492"));
                    for (Iterator<NodeInst> it = lay.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        for (String t : targets) {
                            if (ni.toString().contains(t)) {
                                System.out.println("Node " + t + " is " + ni.getProto().getName() + " at " + ni.getTrueCenter());
                            }
                        }
                    }
                    for (Iterator<ArcInst> it = lay.getArcs(); it.hasNext(); ) {
                        ArcInst ai = it.next();
                        for (String t : targets) {
                            if (ai.toString().contains(t)) {
                                System.out.println("Arc " + t + " is " + ai.getProto().getName() + " from " + ai.getHeadPortInst().getNodeInst().getTrueCenter() + " to " + ai.getTailPortInst().getNodeInst().getTrueCenter());
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
