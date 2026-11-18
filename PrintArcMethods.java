
import com.sun.electric.database.topology.ArcInst;
import java.lang.reflect.Method;

public class PrintArcMethods {
    public static void main(String[] args) {
        for (Method m : ArcInst.class.getMethods()) {
            if (m.getName().toLowerCase().contains("width") || m.getName().toLowerCase().contains("size") || m.getName().toLowerCase().contains("thick")) {
                System.out.println(m);
            }
        }
    }
}
