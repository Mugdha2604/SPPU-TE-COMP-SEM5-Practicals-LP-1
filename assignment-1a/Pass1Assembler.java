import java.io.*;
import java.util.*;

class SymbolTableEntry
{
    String symbol;
    int address;
    int length;

    SymbolTableEntry(String symbol, int address, int length)
    {
        this.symbol = symbol;
        this.address = address;
        this.length = length;
    }
}

class LiteralTableEntry
{
    String literal;
    int address;

    LiteralTableEntry(String literal, int address)
    {
        this.literal = literal;
        this.address = address;
    }
}

class PoolTableEntry
{
    int startIndex;
    int numOfLiterals;

    PoolTableEntry(int startIndex, int numOfLiterals)
    {
        this.startIndex = startIndex;
        this.numOfLiterals = numOfLiterals;
    }
}

class POTEntry
{
    String mnemonic;
    String opcodetype;
    String opcodeval;
    
    POTEntry(String mnemonic, String opcodetype, String opcodeval)
    {
        this.mnemonic = mnemonic;
        this.opcodetype = opcodetype;
        this.opcodeval = opcodeval;
    }
}

class MOTEntry
{
    String mnemonic;
    String opcodetype;
    String opcodeval;
    int length;

    MOTEntry(String mnemonic, String opcodetype, String opcodeval, int length)
    {
        this.mnemonic = mnemonic;
        this.opcodetype = opcodetype;
        this.opcodeval = opcodeval;
        this.length = length;
    }

}

class Pass1Assembler 
{

    private static List<SymbolTableEntry> SYMTAB;
    private static List<LiteralTableEntry> LITTAB;
    private static List<PoolTableEntry> POOLTAB;
    private static List<String> IC;

    private static Map<String,POTEntry> POT;
    private static Map<String,MOTEntry> MOT;
    private static Map<String,String> REG;
    private static Map<String,String> CC;

    private int littab_ptr;
    private int pooltab_ptr;
    private int LC;

    Pass1Assembler()
    {
        POT = new HashMap<>();
		POT.put("START", new POTEntry("START", "AD", "01"));
		POT.put("END", new POTEntry("END", "AD", "02"));
		POT.put("ORIGIN", new POTEntry("ORIGIN", "AD", "03"));
		POT.put("EQU", new POTEntry("EQU", "AD", "04"));
		POT.put("LTORG", new POTEntry("LTORG", "AD", "05"));
		POT.put("DS", new POTEntry("DS", "DL", "01"));
		POT.put("DC", new POTEntry("DC", "DL", "02"));
		
		MOT = new HashMap<>();
		MOT.put("STOP", new MOTEntry("STOP", "IS", "00", 1));
		MOT.put("ADD", new MOTEntry("ADD", "IS", "01", 1));
		MOT.put("SUB", new MOTEntry("SUB", "IS", "02", 1));
		MOT.put("MULT", new MOTEntry("MULT", "IS", "03", 1));
		MOT.put("MOVER", new MOTEntry("MOVER", "IS", "04", 1));
		MOT.put("MOVEM", new MOTEntry("MOVEM", "IS", "05", 1));
		MOT.put("COMP", new MOTEntry("COMP", "IS", "06", 1));
		MOT.put("BC", new MOTEntry("BC", "IS", "07", 1));
		MOT.put("DIV", new MOTEntry("DIV", "IS", "08", 1));
		MOT.put("READ", new MOTEntry("READ", "IS", "09", 1));
		MOT.put("PRINT", new MOTEntry("PRINT", "IS", "10", 1));

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

		SYMTAB = new ArrayList<>();
		LITTAB = new ArrayList<>();
		POOLTAB = new ArrayList<>();
		IC = new ArrayList<>();

		POOLTAB.add(new PoolTableEntry(0,0));
		// POOLTAB.add(new PoolTableEntry(1,0)); // --- LOGICAL ERROR 1: This entry is incorrect.
        // The first pool (at index 0) is all that's needed to start.

        LC = 0;
        littab_ptr = 1;
        pooltab_ptr = 0; // --- LOGICAL ERROR 1: Pointer must start at 0 to point to the first pool.
    }

    // initializeTables();

    void processLine (String line)
    {
        line = line.trim();
        if (line.isEmpty()) return;
        String[] tokens = line.split("\\s+");

        String label = null;
        String opcode;
        String operand1 = null;
        String operand2 = null;
        
        // --- FIX: 'bool' is not a Java type, use 'boolean'
        boolean shouldInitialiseLabel = false;
        int existingLabelIndex = -1; // Added initialization

        //check if label is present
        // --- FIX: tokens[0] is a String, not a boolean. Check if it's NOT in POT or MOT.
        if (tokens.length > 0 && !POT.containsKey(tokens[0]) && !MOT.containsKey(tokens[0]))
        {
            label = tokens[0];
            opcode = tokens[1];
            if (tokens.length > 2)
            {
                operand1 = tokens[2].trim().replace(",", "");
            }
            if (tokens.length > 3)
            {
                operand2 = tokens[3].trim().replace(",", "");
            }

            // check if label exists
            existingLabelIndex = getSymbolIndex(label);
            if (existingLabelIndex == -1)
            {
                shouldInitialiseLabel = true;
            }
            else
            {
                SymbolTableEntry entry = SYMTAB.get(existingLabelIndex);
                entry.address = LC;
            }
        }
        else
        {
            // --- FIX: Typo, token[0] -> tokens[0]
            opcode = tokens[0];
            if (tokens.length > 1)
            {
                operand1 = tokens[1].trim().replace(",", "");
            }
            if (tokens.length > 2)
            {
                operand2 = tokens[2].trim().replace(",", "");
            }

        }
        if (label != null && shouldInitialiseLabel)
        {
            SymbolTableEntry symbol = new SymbolTableEntry(label,-1, 1);    //-1 for uninitialised
            SYMTAB.add(symbol);
            // Update existingLabelIndex to the newly added symbol
            existingLabelIndex = SYMTAB.size() - 1;
        }

        //label processing done

        // Processing opcode
        if (POT.containsKey(opcode))    // is AD or DL
        {
            POTEntry entry = POT.get(opcode);
            switch(entry.mnemonic)
            {
                case "START" : 
                {
                    LC = Integer.parseInt(operand1);    // set LC 
                    IC.add(String.format("(AD,01) (C,%d)", LC));
                    
                }
                break;

                case "END" : 
                {
                    processLiteralsAtEnd();    
                    // pooltab_ptr++; // --- LOGICAL ERROR 2: Do not increment pointer on END. This is the final pool.
                    IC.add(String.format("(AD,02)"));
                }
                break;

                case "ORIGIN" :
                {
                    // assuming operand 1 is symbol + int
                    if (operand1.contains("+")) 
                    {
                        // --- FIX: Must escape '+' for split
                        String[] parts = operand1.split("\\+");
                    
                        // --- FIX: Use 'parts' array, not 'operand1'
                        String symbol = parts[0].trim();
                        String valStr = parts[1].trim();
                        int val = Integer.parseInt(valStr);
                        
                        int symbolIndex = getSymbolIndex(symbol);
                        int newLC = val; // Default to just the value

                        // --- FIX: Corrected scope logic ---
                        if (symbolIndex!=-1) 
                        {
                            SymbolTableEntry sym = SYMTAB.get(symbolIndex); // Declare AND initialize inside
                            if (sym.address!=-1) { // Now it's safe to use sym
                                newLC += sym.address;
                            }
                        }
                        LC = newLC; // Assign the final value to LC
                        // --- FIX: IC format should use (S, index) for symbol
                        IC.add(String.format("(AD,03) (S,%02d)+%d", symbolIndex, val));          
                    }
                    else if (operand1.contains("-"))
                    {
                        String[] parts = operand1.split("-");
                    
                        // --- FIX: Use 'parts' array
                        String symbol = parts[0].trim();
                        String valStr = parts[1].trim();
                        int val = Integer.parseInt(valStr);
                        int symbolIndex = getSymbolIndex(symbol);
                        
                        // --- FIX: Corrected scope logic ---
                        if (symbolIndex!=-1) {
                             SymbolTableEntry sym = SYMTAB.get(symbolIndex); // Declare AND initialize inside
                            if (sym.address!=-1) {
                                LC = sym.address - val; // Only assign LC if sym is valid
                            }
                        }
                        IC.add(String.format("(AD,03) (S,%02d)-%d", symbolIndex, val));
                    }
                    else    //just symbol
                    {
                        // --- FIX: 'symbol' not defined here, use 'operand1'
                        int symbolIndex = getSymbolIndex(operand1);
                        
                        // --- FIX: Corrected scope logic ---
                        if (symbolIndex!=-1) {
                            SymbolTableEntry sym = SYMTAB.get(symbolIndex); // Declare AND initialize inside
                            if (sym.address != -1) LC = sym.address; // Only assign LC if sym is valid
                        }
                        // --- FIX: Corrected parenthesis and format (C -> S)
                        IC.add(String.format("(AD,03) (S,%02d)", symbolIndex));
                    }               
                }
                break;

                case "EQU" : 
                {
                    // --- FIX: Add a check for a missing label ---
                    if (existingLabelIndex == -1) {
                        System.err.println("Error: EQU directive must have a label.");
                        break; // Stop processing this line
                    }
                    
                    // This assumes a label was present on the EQU line
                    SymbolTableEntry labelEntry = SYMTAB.get(existingLabelIndex);

                    if (operand1.contains("+"))
                    {
                        // --- FIX: Must escape '+'
                        String[] parts = operand1.trim().split("\\+");
                        int operand1Index = getSymbolIndex(parts[0]);
                        SymbolTableEntry opearand1Entry = SYMTAB.get(operand1Index);
                        int val = Integer.parseInt(parts[1]);
                        labelEntry.address = opearand1Entry.address + val;
                        IC.add(String.format("(AD,04) (S,%02d)+%d", operand1Index, val));
                    }
                    else if (operand1.contains("-"))
                    {
                        String[] parts = operand1.trim().split("-");
                        int operand1Index = getSymbolIndex(parts[0]);
                        SymbolTableEntry opearand1Entry = SYMTAB.get(operand1Index);
                        int val = Integer.parseInt(parts[1]);
                        labelEntry.address = opearand1Entry.address - val;
                        IC.add(String.format("(AD,04) (S,%02d)-%d", operand1Index, val));
                    }
                    else
                    {
                        // --- HERE IS THE FIX ---
                        // Try to parse operand1 as a plain number first.
                        try {
                            int val = Integer.parseInt(operand1);
                            
                            // It's a number! Assign it as a constant.
                            labelEntry.address = val;
                            IC.add(String.format("(AD,04) (C,%d)", val));

                        } catch (NumberFormatException e) {
                            // It's not a number, so treat it as a symbol.
                            // This was your original 'else' logic.
                            int operand1Index = getSymbolIndex(operand1.trim());
                            SymbolTableEntry opearand1Entry = SYMTAB.get(operand1Index);
                            labelEntry.address = opearand1Entry.address;
                            IC.add(String.format("(AD,04) (S,%02d)", operand1Index));
                        }
                    }                     
                }
                break;

                case "LTORG" : 
                {
                    processLiteralsAtLTORG();
                    pooltab_ptr++;
                    IC.add(String.format("(AD,05)"));
                }
                break;

                case "DS" : 
                {
                    // Assumes a label was present
                    SymbolTableEntry symbol = SYMTAB.get(existingLabelIndex);
                    
                    int len = Integer.parseInt(operand1);

                    symbol.length = len;
                    symbol.address = LC;
                    LC += len;

                    IC.add(String.format("(DL,01) (C,%d)", len)); // DS length is a constant
                }
                break;

                case "DC" : 
                {
                    // Assumes a label was present
                    SymbolTableEntry symbol = SYMTAB.get(existingLabelIndex);
                    
                    int val = Integer.parseInt(operand1);
                    symbol.address = LC;
                    symbol.length = 1;
                    LC += 1;

                    IC.add(String.format("(DL,02) (C,%d)", val)); // DC value is a constant

                }
                break;

                default : System.out.println("Invalid Type!");
                        break;
                
            }

        }
        else if (MOT.containsKey(opcode))
        {
            MOTEntry motEntry = MOT.get(opcode);
            StringBuilder icEntry = new StringBuilder();
            
            // --- FIX: Field names are opcodetype and opcodeval
            icEntry.append(String.format("(%s, %s)", motEntry.opcodetype, motEntry.opcodeval));

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
                        SYMTAB.add(new SymbolTableEntry(operand1, -1, 1)); // Address will be resolved in Pass 2
                        symIndex = SYMTAB.size() - 1; // Use 0-based index
                    } 
                    icEntry.append(String.format(" (S, %02d)", symIndex));
                }
            }

            // Process operand2
            if (operand2 != null) {
                if (operand2.startsWith("='") && operand2.endsWith("'")) {
                    String literalValue = operand2.substring(2, operand2.length() - 1); // Remove =' and '
                    int litIndex = getLiteralIndex(literalValue);
                    if (litIndex == -1) {
                        // Add to LITTAB
                        LITTAB.add(new LiteralTableEntry(literalValue, 0)); // Address resolved in LTORG/END
                        litIndex = LITTAB.size() - 1; // Use 0-based index
                        // Update current pool's literal count
                        POOLTAB.get(pooltab_ptr).numOfLiterals++;
                    }
                    icEntry.append(String.format(" (L, %02d)", litIndex));
                } else { // It's a symbol
                    int symIndex = getSymbolIndex(operand2);
                    if (symIndex == -1) {
                        // Add to SYMTAB (forward reference)
                        SYMTAB.add(new SymbolTableEntry(operand2, -1, 1)); // Address will be resolved later
                        symIndex = SYMTAB.size() - 1; // Use 0-based index
                    }
                    icEntry.append(String.format(" (S, %02d)", symIndex));
                }
            }
            // --- FIX: List name is IC
            IC.add(icEntry.toString());
            LC += motEntry.length; // Increment LC by instruction length
        }
        else
        {
            System.err.println("Error: Invalid opcode! " + opcode);
        }
        
    }

    void processLiteralsAtLTORG()
    {
        PoolTableEntry poolEntry = POOLTAB.get(pooltab_ptr);
        
        // --- FIX: Field name is startIndex
        for (int i = poolEntry.startIndex; i < poolEntry.startIndex + poolEntry.numOfLiterals; i++)
        {
            LiteralTableEntry litEntry = LITTAB.get(i);
            // --- FIX: Address 0 means unassigned (based on LITTAB.add)
            if (litEntry.address == 0)
            {
                litEntry.address = LC;
                LC++;
            }
        }
        // Add new entry for the next pool
        POOLTAB.add(new PoolTableEntry(LITTAB.size(), 0));
    }

    void processLiteralsAtEnd()
    {
        PoolTableEntry poolEntry = POOLTAB.get(pooltab_ptr);
        
        // --- FIX: Field name is startIndex
        for (int i = poolEntry.startIndex; i < poolEntry.startIndex + poolEntry.numOfLiterals; i++)
        {
            LiteralTableEntry litEntry = LITTAB.get(i);
            // --- FIX: Address 0 means unassigned
            if (litEntry.address == 0)
            {
                litEntry.address = LC;
                LC++;
            }
        }
    }

    // --- FIX: Corrected List access ---
    int getSymbolIndex(String symbolname)
    {
        for (int i=0; i < SYMTAB.size(); i++)
        {
            SymbolTableEntry entry = SYMTAB.get(i);
            // --- FIX: Use .equals() for String comparison
            if (entry.symbol.equals(symbolname))
            {
                return i;
            }
        }
        return -1;
    }

    // --- FIX: Corrected List access ---
    int getLiteralIndex(String literalname)
    {
        // --- LOGICAL ERROR 3: Must search *only* the current pool.
        // Searching the whole LITTAB breaks literal re-use across pools.
        PoolTableEntry currentPool = POOLTAB.get(pooltab_ptr);
        for (int i = currentPool.startIndex; i < currentPool.startIndex + currentPool.numOfLiterals; i++)
        {
            LiteralTableEntry entry = LITTAB.get(i);
            // --- LOGICAL ERROR 4: Typo, was checking entry.symbol
            if (entry.literal.equals(literalname))
            {
                return i;
            }
        }
        return -1; // Not found in current pool
    }

    // --- FIX: Must handle potential IOException ---
    void writeIntermediateCodeToFile() throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new FileWriter("IntermediateCode.txt"));
        for (String line : IC)
        {
            bw.write(line);
            bw.write("\n");
            System.out.println(line);
        }
        bw.close();
    }

    void displaySymbolTable()
    {
        System.out.println("\n--------SYMBOL TABLE-----------");
        System.out.println("Symbol\tAddress\tLength");
        for (SymbolTableEntry entry : SYMTAB)
        {
            System.out.println(entry.symbol + "\t" + entry.address + "\t" + entry.length);
        }
        System.out.println("---------------------------------");
    }

    void displayLiteralTable()
    {
        System.out.println("\n--------LITERAL TABLE-----------");
        // --- FIX: LiteralTableEntry has no length field ---
        System.out.println("Literal\tAddress");
        for (LiteralTableEntry entry : LITTAB)
        {
            System.out.println(entry.literal + "\t" + entry.address);
        }
        System.out.println("----------------------------------");
    }

    void displayPoolTable()
    {
        System.out.println("\n--------POOL TABLE-----------");
        System.out.println("Start Index \tNumber of Literals");
        for (PoolTableEntry entry : POOLTAB)
        {
            // --- FIX: Field name is startIndex ---
            System.out.println(entry.startIndex + "\t\t" + entry.numOfLiterals);
        }
        System.out.println("-----------------------------");
    }

    // --- FIX: Must handle potential IOException ---
    public static void main(String args[]) throws IOException
    {
        Pass1Assembler assembler = new Pass1Assembler();
        BufferedReader br = new BufferedReader(new FileReader("input.asm"));
        String line;
        while((line = br.readLine())!=null)
        {
            System.out.println("Processing line : " + line);
            assembler.processLine(line);
        }
        br.close(); // Close the reader

        assembler.writeIntermediateCodeToFile();
        assembler.displaySymbolTable();
        assembler.displayLiteralTable();
        assembler.displayPoolTable();
    }
}