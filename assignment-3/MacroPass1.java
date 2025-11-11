import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap; // Using LinkedHashMap to maintain the insertion order of parameters

/**
 * Implements Pass 1 of a two-pass macro processor.
 * This pass reads the input assembly code, identifies macro definitions,
 * and builds the necessary data structures (MNT, MDT, KPDT, PNTAB).
 * It also generates an intermediate file for non-macro code.
 */
public class MacroPass1 {

    public static void main(String[] args) throws IOException {
        
        // 1. Setup: Open all necessary input and output files
        
        // Input assembly file
        BufferedReader br = new BufferedReader(new FileReader("macro_input.asm"));
        
        // Output file for Macro Name Table (MNT)
        FileWriter mnt = new FileWriter("mnt.txt");
        // Output file for Macro Definition Table (MDT)
        FileWriter mdt = new FileWriter("mdt.txt");
        // Output file for Keyword Parameter Default Table (KPDT)
        FileWriter kpdt = new FileWriter("kpdt.txt");
        // Output file for Parameter Name Table (PNTAB)
        FileWriter pnt = new FileWriter("pntab.txt");
        // Output file for Intermediate Code (non-macro code)
        FileWriter ir = new FileWriter("intermediate.txt");
        
        // Temporary storage for the *current* macro's PNTAB.
        // Maps parameter name (String) to its positional index (Integer).
        LinkedHashMap<String, Integer> pntab = new LinkedHashMap<>();
        
        // 2. Initialization: Set up variables, counters, and pointers
        
        String line;
        String Macroname = null; // Stores the name of the macro currently being defined
        int mdtp = 1;      // Macro Definition Table Pointer (tracks line number in MDT)
        int kpdtp = 0;     // Keyword Parameter Default Table Pointer (tracks starting index in KPDT)
        int paramNo = 1;   // Counter for the number of parameters (1, 2, 3...)
        int pp = 0;        // Positional Parameter count for the current macro
        int kp = 0;        // Keyword Parameter count for the current macro
        int flag = 0;      // Flag: 0 = outside macro, 1 = inside macro definition
        
        // 3. Processing: Read the input file line by line
        
        while ((line = br.readLine()) != null) {
            
            // Split the line into parts based on whitespace
            String parts[] = line.split("\\s+");
            
            // -----------------------------------------------------------------
            // Case 1: Start of a macro definition
            // -----------------------------------------------------------------
            if (parts[0].equalsIgnoreCase("MACRO")) {
                flag = 1; // Set flag to indicate we are inside a macro definition
                
                // The next line is the macro prototype (name and parameters)
                line = br.readLine();
                parts = line.split("\\s+");
                Macroname = parts[0]; // The first part is the macro's name
                
                // If the macro prototype has no parameters (just the name)
                if (parts.length <= 1) {
                    // Write to MNT: MacroName, PP_Count, KP_Count, MDT_Pointer, KPDT_Pointer
                    mnt.write(parts[0] + "\t" + pp + "\t" + kp + "\t" + mdtp + "\t" + (kp == 0 ? kpdtp : (kpdtp + 1)) + "\n");
                    continue; // Skip parameter processing and read the next line
                }
                
                // Process parameters (if they exist)
                for (int i = 1; i < parts.length; i++) { // Start from index 1 (parameters)
                    
                    // Clean the parameter string (remove '&' and ',')
                    parts[i] = parts[i].replaceAll("[&,]", ""); 
                    
                    // Check if it's a Keyword Parameter (contains '=')
                    if (parts[i].contains("=")) {
                        ++kp; // Increment keyword parameter count
                        String keywordParam[] = parts[i].split("=");
                        
                        // Add parameter to PNTAB: "PARAM_NAME" -> index (paramNo)
                        pntab.put(keywordParam[0], paramNo++);
                        
                        // Check if a default value is provided
                        if (keywordParam.length == 2) {
                            // Write to KPDT: Param_Name, Default_Value
                            kpdt.write(keywordParam[0] + "\t" + keywordParam[1] + "\n");
                        } else {
                            // No default value (e.g., "PARAM="), write '-'
                            kpdt.write(keywordParam[0] + "\t-\n");
                        }
                    } else { // It's a Positional Parameter
                        ++pp; // Increment positional parameter count
                        // Add parameter to PNTAB: "PARAM_NAME" -> index (paramNo)
                        pntab.put(parts[i], paramNo++);
                    }
                }
                
                // After processing all parameters, write the MNT entry
                // MNT Format: MacroName, PP_Count, KP_Count, MDT_Pointer, KPDT_Pointer
                mnt.write(parts[0] + "\t" + pp + "\t" + kp + "\t" + mdtp + "\t" + (kp == 0 ? kpdtp : (kpdtp + 1)) + "\n");
                
                // Update the global KPDT pointer for the *next* macro
                kpdtp = kpdtp + kp;
            } 
            // -----------------------------------------------------------------
            // Case 2: End of a macro definition
            // -----------------------------------------------------------------
            else if (parts[0].equalsIgnoreCase("MEND")) {
                mdt.write(line + "\n"); // Write "MEND" to the MDT
                flag = 0;  // We are now outside the macro definition
                kp = 0;    // Reset keyword param count
                pp = 0;    // Reset positional param count
                mdtp++;    // Increment MDT pointer
                paramNo = 1; // Reset parameter index counter
                
                // Write the completed PNTAB for this macro to pntab.txt
                pnt.write(Macroname + ":\t");
                Iterator<String> itr = pntab.keySet().iterator();
                while (itr.hasNext()) {
                    pnt.write(itr.next() + "\t");
                }
                pnt.write("\n");
                pntab.clear(); // Clear the temporary PNTAB map for the next macro
            } 
            // -----------------------------------------------------------------
            // Case 3: Inside a macro definition (processing the macro body)
            // -----------------------------------------------------------------
            else if (flag == 1) {
                // This line is part of the macro's body
                for (int i = 0; i < parts.length; i++) {
                    // Check if a part of the line is a parameter
                    if (parts[i].contains("&")) {
                        // Clean the parameter name
                        parts[i] = parts[i].replaceAll("[&,]", "");
                        
                        // Substitute the name with its (P, index) notation
                        // e.g., "&ARG1" becomes "(P,1)" by looking up in pntab
                        mdt.write("(P," + pntab.get(parts[i]) + ")\t");
                    } else {
                        // Not a parameter, write the part as is (e.g., opcode, register)
                        mdt.write(parts[i] + "\t");
                    }
                }
                mdt.write("\n"); // End of line in MDT
                mdtp++; // Increment MDT pointer
            } 
            // -----------------------------------------------------------------
            // Case 4: Not in a macro (regular assembly code)
            // -----------------------------------------------------------------
            else {
                // This is normal assembly code, write it to the intermediate file
                ir.write(line + "\n");
            }
        }
        
        // 4. Cleanup: Close all file writers and the reader
        br.close();
        mdt.close();
        mnt.close();
        ir.close();
        pnt.close();
        kpdt.close();
        
        System.out.println("Macro Pass 1 Processing complete. Check output files (mnt.txt, mdt.txt, etc.)");
    }
}