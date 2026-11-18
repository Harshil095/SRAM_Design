import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class AnalyzeErrors {
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("/Users/harshil_rathan/.gemini/antigravity/brain/6fad32de-18d9-4e3e-899c-dc163bc7a1db/.system_generated/steps/6209/output.txt"));
        String line;
        Map<String, Integer> cat = new HashMap<>();
        List<String> samples = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            if (line.startsWith("DRC:")) {
                // simplify line
                String clean = line.replaceAll("at \\([0-9.-]+, [0-9.-]+\\)", "")
                                   .replaceAll("\\[net@[0-9]+\\]", "")
                                   .replaceAll("\\[contact@[0-9]+\\]", "")
                                   .replaceAll("\\[via@[0-9]+\\]", "");
                cat.put(clean, cat.getOrDefault(clean, 0) + 1);
                if (samples.size() < 40) samples.add(line);
            }
        }
        br.close();
        System.out.println("=== ERROR SUMMARY (" + cat.values().stream().mapToInt(Integer::intValue).sum() + " total) ===");
        for (Map.Entry<String, Integer> e : cat.entrySet()) {
            System.out.println(e.getValue() + "x : " + e.getKey());
        }
    }
}
