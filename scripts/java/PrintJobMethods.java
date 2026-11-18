
import com.sun.electric.tool.Job;
import java.lang.reflect.Method;

public class PrintJobMethods {
    public static void main(String[] args) {
        for (Method m : Job.class.getMethods()) {
            if (m.getName().toLowerCase().contains("wait")) {
                System.out.println(m);
            }
        }
    }
}
