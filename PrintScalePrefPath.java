
import com.sun.electric.Main;
import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

public class PrintScalePrefPath {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintScalePrefPath", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                Technology mocmos = Technology.findTechnology("mocmos");
                Setting s = mocmos.getScaleSetting();
                System.out.println("prefPath: " + s.getPrefPath());
                System.out.println("prefName: " + s.getPrefName());
                System.out.println("xmlPath:  " + s.getXmlPath());
                return true;
            }
        };
        j.startJob();
        Thread.sleep(1000);
    }
}
