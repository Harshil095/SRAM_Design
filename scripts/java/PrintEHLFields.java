
import java.lang.reflect.Field;

public class PrintEHLFields {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("com.sun.electric.tool.user.ErrorHighLine");
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            System.out.println(f);
        }
    }
}
