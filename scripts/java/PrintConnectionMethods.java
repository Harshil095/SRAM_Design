
import com.sun.electric.database.topology.Connection;
import java.lang.reflect.Method;

public class PrintConnectionMethods {
    public static void main(String[] args) {
        for (Method m : Connection.class.getMethods()) {
            System.out.println(m);
        }
    }
}
