
import com.sun.electric.tool.user.ErrorHighlight;
import java.lang.reflect.Method;

public class PrintEHMethods {
    public static void main(String[] args) {
        for (Method m : ErrorHighlight.class.getMethods()) {
            System.out.println(m);
        }
    }
}
