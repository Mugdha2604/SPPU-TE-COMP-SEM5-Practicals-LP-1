import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an entry in the Symbol Table (SYMTAB).
 */
class SymbolTableEntry {
    String symbol;
    int address;
    int length; // For DS/DC, length of allocated memory

    public SymbolTableEntry(String symbol, int address, int length) {
        this.symbol = symbol;
        this.address = address;
        this.length = length;
    }

    @Override
    public String toString() {
        return String.format("%-10s %-7d %-6d", symbol, address, length);
    }
}

/**
 * Represents an entry in the Literal Table (LITTAB).
 */
class LiteralTableEntry {
    String literal;
    int address;

    public LiteralTableEntry(String literal, int address) {
        this.literal = literal;
        this.address = address;
    }

    @Override
    public String toString() {
        return String.format("%-10s %-7d", literal, address);
    }
}

/**
 * Represents an entry in the Pool Table (POOLTAB).
 * Each entry points to the first literal in LITTAB for a given pool
 * and tracks the number of literals in that pool.
 */
class PoolTableEntry {
    int firstLiteralIndex; // 1-based index in LITTAB
    int numLiterals;

    public PoolTableEntry(int firstLiteralIndex, int numLiterals) {
        this.firstLiteralIndex = firstLiteralIndex;
        this.numLiterals = numLiterals;
    }

    @Override
    public String toString() {
        return String.format("%-10d %-7d", firstLiteralIndex, numLiterals);
    }
}

/**
 * Represents an entry in the Machine Opcode Table (MOT).
 */
class MOTEntry {
    String mnemonic;
    String opcodeClass; // e.g., "IS" (Imperative Statement)
    String opcodeValue; // e.g., "01"
    int length;         // Instruction length

    public MOTEntry(String mnemonic, String opcodeClass, String opcodeValue, int length) {
        this.mnemonic = mnemonic;
        this.opcodeClass = opcodeClass;
        this.opcodeValue = opcodeValue;
        this.length = length;
    }
}

/**
 * Represents an entry in the Pseudo Opcode Table (POT).
 */
class POTEntry {
    String mnemonic;
    String opcodeClass; // e.g., "AD" (Assembler Directive), "DL" (Declaration)
    String opcodeValue; // e.g., "01"

    public POTEntry(String mnemonic, String opcodeClass, String opcodeValue) {
        this.mnemonic = mnemonic;
        this.opcodeClass = opcodeClass;
        this.opcodeValue = opcodeValue;
    }
}

/**
 * Simulates Pass 1 of a Two-Pass Assembler.
 */
public class Pass1Assembler {

    private int LC; // Location Counter
    private int littab_ptr; // Pointer to the next available entry in LITTAB (1-based)
    private int pooltab_ptr; // Pointer to the next available entry in POOLTAB (1-based)

    private List<SymbolTableEntry> SYMTAB;
    private List<LiteralTableEntry> LITTAB;
    private List<PoolTableEntry> POOLTAB;
    private List<String> intermediateCode;

    // Hardcoded Tables
    private Map<String, MOTEntry> MOT; // Machine Opcode Table
    private Map<String, POTEntry> POT; // Pseudo Opcode Table
    private Map<String, String> REG; // Register Table
    private Map<String, String> CC; // Condition Code Table

    public Pass1Assembler() {
        LC = 0;
        littab_ptr = 1;
        pooltab_ptr = 1;

        SYMTAB = new ArrayList<>();
        LITTAB = new ArrayList<>();
        POOLTAB = new ArrayList<>();
        intermediateCode = new ArrayList<>();

        // Initialize POOLTAB with the first entry
        POOLTAB.add(new PoolTableEntry(0, 0)); // Dummy entry at index 0 for 1-based indexing
        POOLTAB.add(new PoolTableEntry(1, 0)); // First actual pool starts at LITTAB index 1

        initializeTables();
    }

    /**
     * Initializes the hardcoded MOT, POT, REG, and CC tables.
     */
    private void initializeTables() {
        MOT = new HashMap<>();
        MOT.put("ADD", new MOTEntry("ADD", "IS", "01", 1));
        MOT.put("SUB", new MOTEntry("SUB", "IS", "02", 1));
        MOT.put("MOVER", new MOTEntry("MOVER", "IS", "04", 1));
        MOT.put("MOVEM", new MOTEntry("MOVEM", "IS", "05", 1)); // Corrected to MOTEntry
        MOT.put("READ", new MOTEntry("READ", "IS", "09", 1));
        MOT.put("PRINT", new MOTEntry("PRINT", "IS", "10", 1));
        MOT.put("BC", new MOTEntry("BC", "IS", "07", 1));
        MOT.put("STOP", new MOTEntry("STOP", "IS", "00", 1));

        POT = new HashMap<>();
        POT.put("START", new POTEntry("START", "AD", "01"));
        POT.put("END", new POTEntry("END", "AD", "02"));
        POT.put("ORIGIN", new POTEntry("ORIGIN", "AD", "03"));
        POT.put("EQU", new POTEntry("EQU", "AD", "04"));
        POT.put("LTORG", new POTEntry("LTORG", "AD", "05"));
        POT.put("DS", new POTEntry("DS", "DL", "01"));
        POT.put("DC", new POTEntry("DC", "DL", "02"));

        REG = new HashMap<>();
        REG.put("AREG", "01");
        REG.put("BREG", "02");
        REG.put("CREG", "03");
        REG.put("DREG", "04");

        CC = new HashMap<>();
        CC.put("LT", "01");
        CC.put("LTE", "02");
        CC.put("EQ", "03");
        CC.put("GT", "04");
        CC.put("GTE", "05");
        CC.put("ANY", "06");
    }

    /**
     * Processes a single line of assembly code.
     *
     * @param line The assembly code line to process.
     */
    public void processLine(String line) {
        line = line.trim();
        if (line.isEmpty()) {
            return;
        }

        String[] tokens = line.split("\\s+");

        String label = null;
        String opcode;
        String operand1 = null;
        String operand2 = null;

        // Determine if there's a label
        if (tokens.length > 1 && !MOT.containsKey(tokens[0]) && !POT.containsKey(tokens[0])) {
            label = tokens[0];
            opcode = tokens[1];
            if (tokens.length > 2) {
                operand1 = tokens[2].replace(",", ""); // Remove comma from operand1
            }
            if (tokens.length > 3) {
                operand2 = tokens[3];
            }
        } 
        else 
        {
            opcode = tokens[0];
            if (tokens.length > 1) {
                operand1 = tokens[1].replace(",", ""); // Remove comma from operand1
            }
            if (tokens.length > 2) {
                operand2 = tokens[2];
            }
        }

        // --- DEBUG PRINT: Added to show which opcode is being processed ---
        System.out.println("  -> Identified Opcode: " + opcode + (label != null ? ", Label: " + label : ""));
        // --- END DEBUG PRINT ---

        // --- START OF MODIFICATION FOR USER'S RULE ---
        // Handle label definition: Add to SYMTAB or update existing entry
        // only if the opcode is an Imperative Statement (IS) or a Declaration (DL) directive.
        boolean shouldInitializeLabel = false;
        if (MOT.containsKey(opcode)) {
            shouldInitializeLabel = true; // IS
        } else if (POT.containsKey(opcode)) {
            POTEntry potEntry = POT.get(opcode);
            if (potEntry.opcodeClass.equals("DL")) {
                shouldInitializeLabel = true; // DL (DS, DC)
            }
            // For AD (START, END, LTORG, ORIGIN, EQU), shouldInitializeLabel remains false
        }

        if (label != null && shouldInitializeLabel) {
            int existingSymbolIndex = getSymbolIndex(label);
            if (existingSymbolIndex == -1) {
                // New symbol, add with current LC
                SYMTAB.add(new SymbolTableEntry(label, LC, 0)); // Length will be updated for DS/DC
            } else {
                // Existing symbol (e.g., forward reference), update its address
                SYMTAB.get(existingSymbolIndex).address = LC;
            }
        }
        // --- END OF MODIFICATION FOR USER'S RULE ---

        // Process based on opcode type
        if (POT.containsKey(opcode)) {    // If opcode is AD or DL
            POTEntry potEntry = POT.get(opcode);
            switch (potEntry.mnemonic) {
                case "START":
                    LC = Integer.parseInt(operand1);
                    intermediateCode.add(String.format("(%s, %s) (C, %s)",
                            potEntry.opcodeClass, potEntry.opcodeValue, operand1));
                    break;
                case "END":
                    processLiteralsAtEnd();
                    intermediateCode.add(String.format("(%s, %s)",
                            potEntry.opcodeClass, potEntry.opcodeValue));
                    break;
                case "LTORG":
                    processLTORG();
                    intermediateCode.add(String.format("(%s, %s)",
                            potEntry.opcodeClass, potEntry.opcodeValue));
                    break;
                case "ORIGIN":
                    try {
                        LC = Integer.parseInt(operand1); // Assuming operand1 is a direct integer for simplicity
                        intermediateCode.add(String.format("(%s, %s) (C, %s)",
                                potEntry.opcodeClass, potEntry.opcodeValue, operand1));
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid operand for ORIGIN. Expected an integer, but got '" + operand1 + "'. IC not generated for this line.");
                    }
                    break;
                case "EQU":
                    // For EQU, the address of the symbol is the value of the operand
                    // This logic remains the same as EQU defines the symbol's value,
                    // regardless of whether it was added to SYMTAB by the LC.
                    int equAddress;
                    try {
                        equAddress = Integer.parseInt(operand1);
                    } catch (NumberFormatException e) {
                        // If operand1 is a symbol, find its address in SYMTAB
                        int symIndex = getSymbolIndex(operand1);
                        if (symIndex != -1) {
                            equAddress = SYMTAB.get(symIndex).address;
                        } else {
                            System.err.println("Error: Operand '" + operand1 + "' for EQU is neither an integer nor a defined symbol. Assuming 0 for " + label + ".");
                            equAddress = 0; // Default or error handling
                        }
                    }
                    // Ensure the label for EQU is in SYMTAB before updating its address.
                    // If it's not already there (due to the new rule), add it now.
                    int equSymbolIndex = getSymbolIndex(label);
                    if (equSymbolIndex == -1 && label != null) {
                        SYMTAB.add(new SymbolTableEntry(label, equAddress, 0));
                    } else if (equSymbolIndex != -1) {
                        SYMTAB.get(equSymbolIndex).address = equAddress;
                    } else if (label == null) {
                        System.err.println("Error: EQU statement without a label. Skipping IC generation.");
                    }
                    intermediateCode.add(String.format("(%s, %s)",
                            potEntry.opcodeClass, potEntry.opcodeValue)); // No operand in IC for EQU
                    break;
                case "DS":
                    int dsSize = Integer.parseInt(operand1);
                    if (label != null) {
                        int symIndex = getSymbolIndex(label);
                        if (symIndex != -1) {
                            SYMTAB.get(symIndex).length = dsSize;
                        }
                    }
                    intermediateCode.add(String.format("(%s, %s) (C, %s)",
                            potEntry.opcodeClass, potEntry.opcodeValue, operand1));
                    LC += dsSize;
                    break;
                case "DC":
                    int dcValue = Integer.parseInt(operand1.replace("'", "")); // Remove quotes if present
                    if (label != null) {
                        int symIndex = getSymbolIndex(label);
                        if (symIndex != -1) {
                            SYMTAB.get(symIndex).length = 1; // DC typically reserves 1 word
                        }
                    }
                    intermediateCode.add(String.format("(%s, %s) (C, %s)",
                            potEntry.opcodeClass, potEntry.opcodeValue, operand1.replace("'", "")));
                    LC += 1; // DC typically reserves 1 word
                    break;
            }
        } else if (MOT.containsKey(opcode)) {
            MOTEntry motEntry = MOT.get(opcode);
            StringBuilder icEntry = new StringBuilder();
            icEntry.append(String.format("(%s, %s)", motEntry.opcodeClass, motEntry.opcodeValue));

            // Process operand1
            if (operand1 != null) {
                if (REG.containsKey(operand1)) { // It's a register
                    icEntry.append(String.format(" (%s)", REG.get(operand1)));
                } else if (CC.containsKey(operand1)) { // It's a condition code (for BC)
                    icEntry.append(String.format(" (%s)", CC.get(operand1)));
                } else {
                    // It's a symbol. Add to SYMTAB if not present (forward reference).
                    int symIndex = getSymbolIndex(operand1);
                    if (symIndex == -1) {
                        SYMTAB.add(new SymbolTableEntry(operand1, 0, 0)); // Address will be resolved in Pass 2
                        symIndex = SYMTAB.size(); // 1-based index
                    } else {
                        symIndex++; // Convert 0-based to 1-based
                    }
                    icEntry.append(String.format(" (S, %02d)", symIndex));
                }
            }

            // Process operand2
            if (operand2 != null) {
                if (isLiteral(operand2)) {
                    String literalValue = operand2.substring(2, operand2.length() - 1); // Remove =' and '
                    int litIndex = getLiteralIndex(literalValue);
                    if (litIndex == -1) {
                        // Add to LITTAB
                        LITTAB.add(new LiteralTableEntry(literalValue, 0)); // Address resolved in LTORG/END
                        litIndex = LITTAB.size(); // 1-based index
                        // Update current pool's literal count
                        POOLTAB.get(pooltab_ptr).numLiterals++;
                    } else {
                        litIndex++; // Convert 0-based to 1-based
                    }
                    icEntry.append(String.format(" (L, %02d)", litIndex));
                } else { // It's a symbol
                    int symIndex = getSymbolIndex(operand2);
                    if (symIndex == -1) {
                        // Add to SYMTAB (forward reference)
                        SYMTAB.add(new SymbolTableEntry(operand2, 0, 0)); // Address will be resolved later
                        symIndex = SYMTAB.size(); // 1-based index
                    } else {
                        symIndex++; // Convert 0-based to 1-based
                    }
                    icEntry.append(String.format(" (S, %02d)", symIndex));
                }
            }
            intermediateCode.add(icEntry.toString());
            LC += motEntry.length; // Increment LC by instruction length
        } else {
            System.err.println("Error: Unknown opcode '" + opcode + "' in line: " + line);
        }
    }

    /**
     * Checks if a string represents a literal (e.g., ='1').
     *
     * @param s The string to check.
     * @return true if it's a literal, false otherwise.
     */
    private boolean isLiteral(String s) {
        return s.startsWith("='") && s.endsWith("'");
    }

    /**
     * Gets the 0-based index of a symbol in SYMTAB.
     *
     * @param symbol The symbol to search for.
     * @return The 0-based index if found, -1 otherwise.
     */
    private int getSymbolIndex(String symbol) {
        for (int i = 0; i < SYMTAB.size(); i++) {
            if (SYMTAB.get(i).symbol.equals(symbol)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the 0-based index of a literal value in LITTAB.
     *
     * @param literalValue The literal value (e.g., "1", "5").
     * @return The 0-based index if found, -1 otherwise.
     */
    private int getLiteralIndex(String literalValue) {
        for (int i = 0; i < LITTAB.size(); i++) {
            if (LITTAB.get(i).literal.equals(literalValue)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Processes literals when an LTORG statement is encountered.
     */
    private void processLTORG() {
        PoolTableEntry currentPool = POOLTAB.get(pooltab_ptr);
        if (currentPool.numLiterals > 0) {
            // Process literals from currentPool.firstLiteralIndex (1-based)
            // up to (currentPool.firstLiteralIndex + currentPool.numLiterals - 1)
            for (int i = currentPool.firstLiteralIndex - 1; i < (currentPool.firstLiteralIndex - 1) + currentPool.numLiterals; i++) {
                if (LITTAB.get(i).address == 0) { // Only process unassigned literals
                    LITTAB.get(i).address = LC;
                    LC += 1; // Each literal takes 1 memory location
                }
            }
        }
        // Start a new pool
        pooltab_ptr++;
        POOLTAB.add(new PoolTableEntry(littab_ptr, 0)); // New pool starts at current littab_ptr
    }

    /**
     * Processes any remaining literals when an END statement is encountered.
     */
    private void processLiteralsAtEnd() {
        // Process literals in the last pool
        PoolTableEntry currentPool = POOLTAB.get(pooltab_ptr);
        if (currentPool.numLiterals > 0) {
            for (int i = currentPool.firstLiteralIndex - 1; i < (currentPool.firstLiteralIndex - 1) + currentPool.numLiterals; i++) {
                if (LITTAB.get(i).address == 0) { // Only process unassigned literals
                    LITTAB.get(i).address = LC;
                    LC += 1;
                }
            }
        }
    }

    /**
     * Prints the Symbol Table (SYMTAB).
     */
    public void printSymbolTable() {
        System.out.println("\n----- SYMBOL TABLE -----");
        System.out.println("Symbol     Address Length");
        System.out.println("------------------------");
        for (SymbolTableEntry entry : SYMTAB) {
            System.out.println(entry);
        }
        System.out.println("------------------------");
    }

    /**
     * Prints the Literal Table (LITTAB).
     */
    public void printLiteralTable() {
        System.out.println("\n----- LITERAL TABLE -----");
        System.out.println("Literal    Address");
        System.out.println("--------------------");
        for (LiteralTableEntry entry : LITTAB) {
            System.out.println(entry);
        }
        System.out.println("--------------------");
    }

    /**
     * Prints the Pool Table (POOLTAB).
     */
    public void printPoolTable() {
        System.out.println("\n----- POOL TABLE -----");
        System.out.println("First Lit  #Literals");
        System.out.println("--------------------");
        // Skip the dummy entry at index 0
        for (int i = 1; i < POOLTAB.size(); i++) {
            System.out.println(POOLTAB.get(i));
        }
        System.out.println("--------------------");
    }

    /**
     * Writes the generated Intermediate Code to a file.
     * @param filename The name of the file to write to.
     */
    public void writeIntermediateCodeToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("----- INTERMEDIATE CODE -----\n");
            for (String code : intermediateCode) {
                writer.write(code + "\n");
            }
            writer.write("-----------------------------\n");
            System.out.println("\nIntermediate Code written to: " + filename);
        } catch (IOException e) {
            System.err.println("Error writing Intermediate Code to file: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        Pass1Assembler assembler = new Pass1Assembler();

        String inputFileName = "input.txt"; // Default input file name
        String outputFileName = "intermediate_code.txt"; // Default output file name

        System.out.println("Reading assembly program from: " + inputFileName);
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Processing: " + line); // Print line being processed to console
                assembler.processLine(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
            System.err.println("Please ensure '" + inputFileName + "' exists in the same directory.");
            return; // Exit if input file cannot be read
        }

        // Write results to file and print tables to console
        assembler.writeIntermediateCodeToFile(outputFileName);
        assembler.printSymbolTable();
        assembler.printLiteralTable();
        assembler.printPoolTable();
    }
}
