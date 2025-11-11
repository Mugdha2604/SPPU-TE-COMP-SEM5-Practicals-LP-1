//package passTwo;

import java.io.*;
import java.util.*;

public class macroPass2 {

    static class MNTEntry {
        String name;
        int ppCount;
        int kpCount;
        int mdtIndex;
        int kpdIndex;

        MNTEntry(String name, int ppCount, int kpCount, int mdtIndex, int kpdIndex) {
            this.name = name;
            this.ppCount = ppCount;
            this.kpCount = kpCount;
            this.mdtIndex = mdtIndex;
            this.kpdIndex = kpdIndex;
        }
    }

    static Map<String, MNTEntry> mnt = new HashMap<>();
    static Map<Integer, String> mdt = new HashMap<>();
    static List<String[]> kpdt = new ArrayList<>();

    public static void main(String[] args) {
        try {
            loadMNT("mnt.txt");
            loadMDT("mdt.txt");
            loadKPDTAB("kpdtab.txt");
            processIntermediateCode("input.asm");
            System.out.println("Macro expansion completed. Output written to output.asm");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    static void loadMNT(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 5) continue;
            String name = parts[0];
            int ppCount = Integer.parseInt(parts[1]);
            int kpCount = Integer.parseInt(parts[2]);
            int mdtIndex = Integer.parseInt(parts[3]);
            int kpdIndex = Integer.parseInt(parts[4]);
            mnt.put(name, new MNTEntry(name, ppCount, kpCount, mdtIndex, kpdIndex));
        }
        br.close();
    }

    static void loadMDT(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        int index = 0;
        while ((line = br.readLine()) != null) {
            mdt.put(index++, line.trim());
        }
        br.close();
    }

    static void loadKPDTAB(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            kpdt.add(line.trim().split("\\s+")); // [paramName, defaultValue]
        }
        br.close();
    }

    static void processIntermediateCode(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        BufferedWriter bw = new BufferedWriter(new FileWriter("output.asm"));
        String line;

        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] tokens = line.trim().split("\\s+");
            String macroName = tokens[0];

            if (mnt.containsKey(macroName)) {
                String[] args = tokens.length > 1 ? line.substring(macroName.length()).trim().split(",") : new String[0];
                expandMacro(macroName, args, bw);
            } else {
                bw.write(line + "\n");
            }
        }

        br.close();
        bw.close();
    }

    static void expandMacro(String macroName, String[] args, BufferedWriter bw) throws IOException {
        MNTEntry entry = mnt.get(macroName);

        LinkedHashMap<String, String> paramMap = new LinkedHashMap<>();

        // Load keyword param defaults
        for (int i = 0; i < entry.kpCount; i++) {
            String[] kp = kpdt.get(entry.kpdIndex + i);
            paramMap.put(kp[0], kp[1]);
        }

        // Fill positional params (use names P1, P2, etc.)
        int posIndex = 0;
        for (int i = 0; i < Math.min(args.length, entry.ppCount); i++) {
            paramMap.put("P" + (i + 1), args[i].trim());
            posIndex++;
        }

        // Handle keyword params after positional
        for (int i = posIndex; i < args.length; i++) {
            String arg = args[i].trim();
            if (arg.contains("=")) {
                String[] kv = arg.split("=");
                if (kv.length == 2)
                    paramMap.put(kv[0].trim(), kv[1].trim());
            }
        }

        /// --- Print ALA/APDTAB for this macro call in proper order ---
        System.out.println("----- Macro call: " + macroName + " -----");
        System.out.println("Parameter List (ALA):");

        // 1. Positional parameters: P1, P2, ...
        for (int i = 1; i <= entry.ppCount; i++) {
            String key = "P" + i;
            String val = paramMap.get(key);
            if (val != null) {
                System.out.printf("  %s = %s%n", key, val);
            }
        }

        // 2. Keyword parameters in order from KPDTAB
        for (int i = 0; i < entry.kpCount; i++) {
            String key = kpdt.get(entry.kpdIndex + i)[0];
            String val = paramMap.get(key);
            if (val != null) {
                System.out.printf("  %s = %s%n", key, val);
            }
        }

        System.out.println("----------------------------");

        // Expand MDT lines with substitutions
        int mdtIndex = entry.mdtIndex;
        while (!mdt.get(mdtIndex).equalsIgnoreCase("MEND")) {
            String line = mdt.get(mdtIndex);
            for (Map.Entry<String, String> e : paramMap.entrySet()) {
                line = line.replace("&" + e.getKey(), e.getValue());
            }
            bw.write("    " + line + "\n");
            mdtIndex++;
        }
    }
}
