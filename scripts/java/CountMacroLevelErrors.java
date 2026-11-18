import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class CountMacroLevelErrors {
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("/Users/harshil_rathan/.gemini/antigravity/brain/6fad32de-18d9-4e3e-899c-dc163bc7a1db/.system_generated/steps/6371/output.txt"));
        String line;
        List<String> macroErrors = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            if (line.startsWith("DRC:")) {
                macroErrors.add(line);
            }
        }
        br.close();
        System.out.println("Total DRC error lines: " + macroErrors.size());
        Map<String, Integer> counts = new HashMap<>();
        for (String err : macroErrors) {
            String prefix = err.replaceAll("\\[[^\\]]+\\]", "").replaceAll("at \\([^\\)]+\\)", "");
            counts.put(prefix, counts.getOrDefault(prefix, 0) + 1);
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            System.out.println(e.getValue() + "x : " + e.getKey());
        }
    }
}
