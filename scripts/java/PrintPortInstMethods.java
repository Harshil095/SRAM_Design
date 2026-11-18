
import com.sun.electric.database.topology.PortInst;
import java.lang.reflect.Method;

public class PrintPortInstMethods {
    public static void main(String[] args) {
        for (Method m : PortInst.class.getMethods()) {
            System.out.println(m);
        }
    }
}
