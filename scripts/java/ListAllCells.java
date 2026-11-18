import java.io.File;
import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class ListAllCells {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ListCells", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String userPath = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795002561961_7f7dd016.jelib";
                    Library userLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(userPath).toURI().toURL(), null, FileType.JELIB, false);
                    System.out.println("=== USER CELLS (" + userLib.getName() + ") ===");
                    Set<String> userCellNames = new TreeSet<>();
                    for (Iterator<Cell> it = userLib.getCells(); it.hasNext(); ) {
                        Cell c = it.next();
                        userCellNames.add(c.getName() + " {" + c.getView().getAbbreviation() + "}");
                    }
                    for (String name : userCellNames) {
                        System.out.println("  " + name);
                    }

                    String friendPath = "/Users/harshil_rathan/Downloads/SRAM_Complete_V1_suraj_antigravity_BACKUP_DRC0_LVS0.jelib";
                    Library friendLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(friendPath).toURI().toURL(), null, FileType.JELIB, false);
                    System.out.println("\n=== FRIEND CELLS (" + friendLib.getName() + ") ===");
                    Set<String> friendCellNames = new TreeSet<>();
                    for (Iterator<Cell> it = friendLib.getCells(); it.hasNext(); ) {
                        Cell c = it.next();
                        friendCellNames.add(c.getName() + " {" + c.getView().getAbbreviation() + "}");
                    }
                    for (String name : friendCellNames) {
                        System.out.println("  " + name);
                    }
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
