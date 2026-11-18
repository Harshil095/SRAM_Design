
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import com.sun.electric.tool.drc.Quick;

public class Sig2 {
    public static void main(String[] args) {
        for (Method m : Quick.class.getDeclaredMethods()) {
            if (m.getName().equals("checkDesignRules")) {
                System.out.println(m.getName() + ":");
                for (Class<?> p : m.getParameterTypes()) {
                    System.out.println("   " + p.getName());
                }
            }
        }
    }
}
