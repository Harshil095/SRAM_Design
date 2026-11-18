
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.projectSettings.ProjSettings;

public class ExportProjSettings {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ExportProjSettings", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Map<Setting, Object> settings = new HashMap<Setting, Object>();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false, settings);
                    
                    EDatabase db = lib.getDatabase();
                    Setting.SettingChangeBatch scb = new Setting.SettingChangeBatch();
                    Technology mocmos = Technology.findTechnology("mocmos");
                    scb.add(mocmos.getScaleSetting(), Double.valueOf(90.0));
                    db.implementSettingChanges(scb);

                    ProjSettings.exportSettings();
                    System.out.println("Exported projsettings.xml!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(2000);
    }
}
