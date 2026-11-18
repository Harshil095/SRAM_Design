import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import java.util.*;

public class ListMocmosProtos {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ListMocmosProtos", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    Technology mocmos = Technology.findTechnology("mocmos");
                    System.out.println("ArcProtos in mocmos:");
                    for (Iterator<ArcProto> it = mocmos.getArcs(); it.hasNext(); ) {
                        System.out.println("  ArcProto: " + it.next().getName());
                    }
                    System.out.println("NodeProtos in mocmos:");
                    for (Iterator<? extends NodeProto> it = mocmos.getNodes(); it.hasNext(); ) {
                        NodeProto np = it.next();
                        if (np.getName().contains("Well") || np.getName().contains("Substr") || np.getName().contains("Pin")) {
                            System.out.println("  NodeProto: " + np.getName());
                        }
                    }
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