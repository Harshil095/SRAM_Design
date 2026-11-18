
import com.sun.electric.database.topology.NodeInst;
import java.lang.reflect.Method;

public class PrintNodeMethods {
    public static void main(String[] args) {
        for (Method m : NodeInst.class.getMethods()) {
            if (m.getName().toLowerCase().contains("arc") || m.getName().toLowerCase().contains("con")) {
                System.out.println(m);
            }
        }
    }
}
