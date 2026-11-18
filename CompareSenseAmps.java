import java.io.File;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class CompareSenseAmps {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("CompSA", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String userPath = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795002561961_7f7dd016.jelib";
                    Library userLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(userPath).toURI().toURL(), null, FileType.JELIB, false);
                    Cell userSA = userLib.findNodeProto("sense_amplifier{lay}");

                    String friendPath = "/Users/harshil_rathan/Downloads/SRAM_Complete_V1_suraj_antigravity_BACKUP_DRC0_LVS0.jelib";
                    Library friendLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(friendPath).toURI().toURL(), null, FileType.JELIB, false);
                    Cell friendSA = friendLib.findNodeProto("sense_amplifier{lay}");

                    System.out.println("User SA nodes: " + userSA.getNumNodes() + ", arcs: " + userSA.getNumArcs() + ", bounds: " + userSA.getBounds());
                    System.out.println("Friend SA nodes: " + friendSA.getNumNodes() + ", arcs: " + friendSA.getNumArcs() + ", bounds: " + friendSA.getBounds());

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
    }
}
