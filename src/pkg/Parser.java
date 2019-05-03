/**
 * Parser.java
 * Noah Huck
 * CS4308
 * Section 03
 * 18 March 2019
 */

package pkg;

import java.io.File;
import java.util.ArrayList;
import static pkg.Constants.*;

public class Parser {
    
    Scanner scanner;                        //variable which references scanner object from 1st deliverable
    boolean EOF;                            // boolean value to indicate when end of file has been reached
    private int nextTok;                //stores the most recent token value from scanner
    private String nextLex;             //stores the most recent lexeme from the scanner
    protected ArrayList<Identifier> idTable;        //table to store identifiers as they're created
    private final ArrayList<String> wsal;       //ArrayList where current statement is being built (working statement array list)
    private int activeIdentifier;           //for use in assignment / declaration of identifiers at execution time
    private String activeIdentifierName;    //for use in assignment / declaration of identifiers at execution time
    private AssignmentStatement assignmentStatement;
    File f;
    
    java.util.Scanner inputHandler;
    
    boolean running = false;
    boolean displayParsedLines = true;
    boolean whileSkip = false;
    
    //Constructor:
    public Parser(String in){
        //constructor initializing class variables
        f = new File(in);
        inputHandler = new java.util.Scanner(System.in);
        this.nextLex = "";
        this.idTable = new ArrayList();
        this.EOF = false;
        this.scanner = new Scanner(f);
        this.wsal = new ArrayList();
        
    }
    
    //Administrative Functions:
    public void scan(){                    //method calls the scanner, leaves next token and lexeme in class variables
        nextTok = scanner.nextToken();
        nextLex = scanner.currentLexeme;
//        System.out.println("Found "+nextTok+" "+nextLex);
    }
    public boolean endOfFileReached(){      //public access to private end of file boolean
        return EOF;
    }
    private void error(String str, int loc, boolean skip){
        /**
         * Method prints out error messages for the parse.
         * First argument is error method that will be printed
         * Second argument is row # where error occurred
         * Third argument determines whether the parser should skip to the next line or not.
         */
        System.err.println(str+"; line "+loc);
        if (skip){
            int x = scanner.getRow();
            while (x == scanner.getRow()){
                scan();
            }
        }
    }  
    public void printIdTable(){     //administrative method to return complete ID table during testing
        idTable.forEach((i) -> {
            System.out.println(i.toString());
        });
    }
    private boolean lookup(String str){
        //Searches to see if string is already in identifier table
        //returns true if it exists
        activeIdentifierName = nextLex;     //allows whole class to remember most recently used ident name
        for(Identifier id : idTable){
            if (str.compareTo(id.getName()) == 0){
                //active identifier is set to index of most recently referenced identifier
                //used for assignments
                activeIdentifier = idTable.indexOf(id);
                return true;
            }
        }
        return false;
    }
    public void flush(){        //Method prints the statement that has been built
        if (running)
            displayParsedLines = false;
        if (!displayParsedLines){
            wsal.clear();
            return;
        }
        String out = "";
        for (String s : wsal){
            out = out.concat(s+" ");
        }
        System.out.println(out);
        wsal.clear();           //ArrayList containing current statement is reset to allow for next statement generation.
    }
    public void output(String out){
        String s = out.replace("\"", "");
        System.out.print(s);
    }

    //Parsing Functions:
    public void parseProgram(){
        //function parses for an entire function
        /* Derived BNF for this method:
           FUNCTION IDENTIFIER IS 
            ( | CONSTANTS data_declarations) VARIABLES data_declarations (  | STRUCT data_declarations)
            BEGIN pactions ENDFUN IDENTIFIER
        */
        scan();
        if (nextTok == FUNCTION){
            wsal.add(nextLex);
            scan();
            if (nextTok == MAIN){       //BNF subset supports 'main' keyword as function name, not identifiers
                wsal.add(nextLex);
                scan();
                if (nextTok == IS){
                    wsal.add(nextLex);
                    flush();
                    scan();
                    if (nextTok == VARIABLES){      //nested ifs were used because syntax is rigid
                        wsal.add(nextLex);      
                        flush();
                        scan();
                        variables();                  //all variable declaration statements here
                    } 
                    if (nextTok == BEGIN){
                        wsal.add(nextLex);
                        flush();
                        scan();
                        begin();           //all function body statements here
                        endfun();           
                    } else {
                        error("Must begin statement declaration list with 'begin' keyword", scanner.getRow(), false);
                    }
                }
                else error("Invalid Function Declaration", scanner.getRow(), false);
            }
            else error("Function name must be 'main' keyword", scanner.getRow(), false);
        }
        else {
            error("Must contain main function", scanner.getRow(), false);
        }
        if (nextTok == Constants.EOF){          //After parsing for a function, method checks for end of file
            EOF = true;
        }
    }
    
    //Declaration Parsing
    private void variables() {      //parses for variable declaration statements
        /** BNF: comp_declare -> DEFINE data_declaration */
        while(nextTok == DEFINE){
            wsal.add(nextLex);
            scan();
            dataDeclaration();
        }
    }  
    private void dataDeclaration() {
        /** BNF: data_declaration -> IDENTIFIER OF TYPE data_type */
        if (nextTok == IDENT) {
            wsal.add(nextLex);
            lookup(nextLex); 
            scan();
            if (nextTok == OF) {
                wsal.add(nextLex);
                scan();
                if (nextTok == TYPE){
                    wsal.add(nextLex);
                    scan();
                    type();
                }
                else {
                    error("Invalid variable declaration", scanner.getRow(), true);
                }
            }
            else {
                error("Invalid variable declaration", scanner.getRow(), true);
            }
        } else {
            error("Invalid Identifier name", scanner.getRow(), true);
        }
    }
    private void type() { 
        /* In BNF, the non-terminal associated with this method is only found in data declarations.
        accordingly, this is the only place identifiers are declared and added to the identifier table.*/
        /** BNF: data_type -> INTEGER | TSTRING */
        switch (nextTok){
            case INTEGER:
                // Decision made here to set default value of unassigned integers to 0.
                idTable.add(new IntegerIdentifier(this.activeIdentifierName, Constants.INTEGER, 0));  
                wsal.add(nextLex);
                flush();
                scan();
                return;
            case TSTRING:
                idTable.add(new StringIdentifier(this.activeIdentifierName, Constants.TSTRING, ""));
                wsal.add(nextLex);
                flush();
                scan();
                return;
            default:
                error("Type not supported.", scanner.getRow(), true);
        }
    }
    
    //Function Body Parsing:
    private void begin(){
        /* BNF: BEGIN pactions */
        while (nextTok != ENDFUNCTION){
            actions();
        } if (nextTok == ENDFUNCTION) {
            wsal.add(nextLex);
            scan();
        }
    }
    private void actions() {
        /* BNF: action_def -> SET name_ref EQUOP expr
            | INPUT name_ref
            | DISPLAY pvar_value_list
            | INCREMENT name_ref
            | DECREMENT name_ref
            | IF pcondition THEN pactions opt_else ENDIF
            | WHILE pcondition DO pactions ENDWHIL
        */
        //actions() method is only looking for the first keyword from each RHS to determine proper method to call.
            switch (nextTok){
                case SET:
                    wsal.add(nextLex);
                    scan();
                    assignment();
                    flush();
                    break;
                case INPUT:
                    wsal.add(nextLex);
                    scan();
                    input();
                    flush();
                    break;
                case DISPLAY:
                    wsal.add(nextLex);
                    scan();
                    display();
                    flush();
                    break;
                case INCREMENT:
                    wsal.add(nextLex);
                    scan();
                    increment();
                    break;
                case DECREMENT:
                    wsal.add(nextLex);
                    scan();
                    decrement();
                    break;
                case IF:
                    wsal.add(nextLex);
                    scan();
                    ifStatement();
                    break;
                case WHILE:
                    wsal.add(nextLex);
                    scan();
                    whileStatement();
                    break;
                default:
                    //  decision made here to skip to the end of a line if no action method is applicable
                    error("Invalid word to begin a statement", scanner.getRow(), true);
                    scan();
                    break;
            }
    }

    //Actions Parsing:
    private void assignment() {
        /** BNF: SET name_ref EQUOP expr */
        
        if (nextTok == IDENT){
            // this call is necessary to establish current identifier as active
            lookup(nextLex);        
            
            wsal.add(nextLex);
            scan();
            if (nextTok == EQUAOP){
                wsal.add(nextLex);
                scan();
                if(running){
                    Identifier id = idTable.get(activeIdentifier);
                    assignmentStatement = new AssignmentStatement(id, expr());
                } else
                    expr();
                /**** Code replaced by AssignmentStatement.java ****/
//                int i = idTable.get(activeIdentifier).getType();
//                if (i == INTEGER){
//                    IntegerIdentifier id = (IntegerIdentifier)idTable.get(activeIdentifier);
//                    System.err.println(id.toString());
//                    id.setValue(Integer.parseInt(expr()));
//                }
//                else {
//                    StringIdentifier id = (StringIdentifier)idTable.get(activeIdentifier);
//                    id.setValue(expr());
//                }
            }
            else {
                error("Invalid syntax for assignment operation", scanner.getRow(), true);
            }
        } else {
            error("Invalid syntax for assignment operation", scanner.getRow(), true);
        }
    }
    private void input() {
        /** BNF: INPUT name_ref
        BNF sample contradicts sample file where a string is included.
        Actual implementation here allows for INPUT string_literal COMMA name_ref */
        switch (nextTok) {
            case STRING:
                wsal.add(nextLex);
                if(running)output(nextLex);
                scan();
                if (nextTok == COMMA){
                    wsal.add(nextLex);
                    scan();
                    if (nextTok == IDENT){
                        wsal.add(nextLex);
                        if (running) {
                            lookup(nextLex);        //index of current identifier in idTable stored to global variable
                            String s = inputHandler.nextLine();
                            Identifier id = idTable.get(activeIdentifier);      // nre reference to existing identifier in memory
                            assignmentStatement = new AssignmentStatement(id, s);
                        }
                        scan();
                    } else
                        error("Input statement invalid syntax", scanner.getRow(), true);
                } else
                    error("Input statement invalid syntax", scanner.getRow(), true);
                break;
            case IDENT:
                wsal.add(nextLex);
                scan();
                break;
            default:
                error("Input statement invalid syntax", scanner.getRow(), true);
                break;
        }
        if(running)output("\n");
    }
    private void display() {
        /** BNF: DISPLAY pvar_value_list */
        /** BNF: pvar_value_list -> expr | pvar_value_list COMMA expr */
        if(running)output(expr());
        else expr();
        while (nextTok == COMMA){
            wsal.add(nextLex);
            scan();
            if(running)output(" "+expr());
            else expr();
        }
        if (running)output("\n");
    }
    private void increment() {
        /** BNF: INCREMENT name_ref 
            BNF: name_ref -> IDENTIFIER */
        if (nextTok == IDENT){
            wsal.add(nextLex);
            if (running){
                lookup(nextLex);
                IntegerIdentifier id = (IntegerIdentifier) idTable.get(activeIdentifier);
                int val = id.getIntValue();
                val++;
                id.setIntValue(val);
            }
            flush();
            scan();
        }
        else {
            error("Only valid identifiers can be incremented", scanner.getRow(), true);
        }
    }
    private void decrement() {
        /** BNF: DECREMENT name_ref 
            BNF: name_ref -> IDENTIFIER */
        if (nextTok == IDENT){
            wsal.add(nextLex);
            if (running){
                lookup(nextLex);
                IntegerIdentifier id = (IntegerIdentifier) idTable.get(activeIdentifier);
                int val = id.getIntValue();
                val--;
                id.setIntValue(val);
            }
            flush();
            scan();
        }
        else {
            error("Only valid identifiers can be decremented", scanner.getRow(), true);
        }
    }
    private void ifStatement() {
        /** BNF: IF pcondition THEN pactions opt_else ENDIF */
        boolean execute = pcondition();
        if (nextTok == THEN){
            wsal.add(nextLex);
            flush();
            scan();
            if (!execute)
                running = false;
            while (nextTok != ENDIF && nextTok != ELSE){
                actions();
            }
            if (nextTok == ELSE){
                wsal.add(nextLex);
                flush();
                scan();
                
                optionalElse(execute);
            } if (nextTok == ENDIF) {   //This is after optional else so it can be written only once
                wsal.add(nextLex);      //when optional else returns, it arrives at this if statement
                flush();
                scan();
                if(!whileSkip)
                    running = true;
            }
        }
        
    }
    private void whileStatement() {
        /** BNF: WHILE pcondition DO pactions ENDWHILE */
        int conditionIterations = scanner.count;
        boolean execute = pcondition();     //boolean set to value of conditional expression
        if (nextTok == DO){
            wsal.add(nextLex);
            flush();
            scan();
            if (!execute){              // if condition was false, parse through loop
                running = false;        // but don't execute any code
                whileSkip = true;       // ensure if statements don't execute any code
            }
            while (nextTok != ENDWHILE) {
                actions();
            }
            if (execute){
                /** 
                 * New parser instance created to come back to beginning of while loop
                 * New Instance will not print anything or execute any code by default
                 * New instance is given values for identifiers from current instance
                 */
                Parser p = new Parser(f.getAbsolutePath());
                p.displayParsedLines = false;
                p.idTable = this.idTable;
                for (int i = 0; i<conditionIterations; i++)
                    p.scan();
                /**
                 * Upon arriving at original conditional expression, it is evaluated again
                 * If true, while statement is parsed and executed again
                 * identifiers from this instance are given values from new instance
                 * New parser instance is reset to conditional statement point, 
                 * where it will be evaluated when loop returns
                 */
                while(p.pcondition()){
                    p.running = true;
                    p.scan();
                    while(p.nextTok != ENDWHILE){
                        p.actions();
                    }
                    this.idTable = p.idTable;
                    p = new Parser(f.getAbsolutePath());
                    p.displayParsedLines = false;
                    p.idTable = this.idTable;
                    for (int i = 0; i<conditionIterations; i++)
                       p.scan();
                }
            }
            if (nextTok == ENDWHILE){
                wsal.add(nextLex);
                flush();
                running = true;         // if condition was originally false, proper execution resumes
                scan();
            }
        }
        else error("Invalid while loop declaration", scanner.getRow(), true);
    }

    //Support for Actions Parsing:
    private String expr() {
        /** BNF: expr -> term
            | term PLUS term
            | term MINUS term */
        String s = term();
        while (nextTok == ADDOP || nextTok == SUBOP){
            int i = Integer.parseInt(s);
            wsal.add(nextLex);
            if (nextTok == ADDOP){
                scan();
                String s1 = term();
                int i1 = Integer.parseInt(s1);
                return Integer.toString(i + i1);
            }
            else {
                scan();
                String s1 = term();
                int i1 = Integer.parseInt(s1);
                return Integer.toString(i - i1);
            }
        }
        return s;
    }
    private String term() {
        /** BNF: term -> punary
            | punary STAR punary
            | punary DIVOP punary */
        String s = punary();
        while (nextTok == STAROP || nextTok == DIVOP){
            int i = Integer.parseInt(s);
            wsal.add(nextLex);
            if (nextTok == STAROP){
                scan();
                String s1 = punary();
                int i1 = Integer.parseInt(s1);
                return Integer.toString(i * i1);
            }
            else {
                scan();
                String s1 = punary();
                int i1 = Integer.parseInt(s1);
                return Integer.toString(i / i1);
            }
            
        }
        return s;
    }
    private String punary() {
        /** BNF: punary -> element | MINUS element */
        if (nextTok == SUBOP){
            wsal.add(nextLex);
            scan();
            return element();
        } else {
            return element();
        }
    }
    private String element() {
        //This will be where values are given out at execution time
        /** BNF: element -> IDENTIFIER | STRING	| NUMBER */
        switch (nextTok) {
            case STRING:
                wsal.add(nextLex);
                String s = nextLex;
                scan();
                return s;
            case IDENT:
                wsal.add(nextLex);
                String s1 = nextLex;
                scan();
                return idTable.get(getIndex(s1)).getValue();
            case NUMBER:
                wsal.add(nextLex);
                String s2= nextLex;
                scan();
                return s2;
            default:
//                scan();
                return "";
        }

    }
    private int getIndex(String s){
        for(Identifier id : idTable){
            if (s.compareTo(id.getName()) == 0){
                return idTable.indexOf(id);
            }
        }
        return -1;
    }
    private boolean pcondition() {
        /** BNF: pcondition -> expr eq_v expr */
        String exp1 = expr();
        int operation = comparison();
        String exp2 = expr();
        
        int val1 = Integer.parseInt(exp1);
        int val2 = Integer.parseInt(exp2);
        switch (operation){
            case EQUATOO:
                if (val1 == val2)
                    return true;
                return false;
            case GTHAN:
                if (val1 > val2)
                    return true;
                return false;
            case LTHAN:
                if (val1 < val2)
                    return true;
                return false;
            default:
                return false;
        }
    }
    private int comparison() {
        /** BNF: eq_v -> EQUALS | GREATER THAN | LESS THAN */
        wsal.add(nextLex);
        int ret = nextTok;
        switch (nextTok){
            case EQUATOO:
                scan();
                break;
            case LTHAN:
                scan();
                break;
            case GTHAN:
                scan();
                break;
            default:
                error("Invalid comparison operator", scanner.getRow(), false);
                scan();
        }
        return ret;
    }
    private void optionalElse(boolean dontExecute) {
        /** opt_else ->
	| ELSE pactions */
        if (dontExecute)
            running = false;
        if(!dontExecute)
            running = true;
         while (nextTok != ENDIF){      //optionalElse method looking for end of if statement
             actions();
         }
    }
    
    private void endfun() {
        /* BNF: ENDFUN IDENTIFIER */
        if (nextTok == IDENT || nextTok == MAIN){
            wsal.add(nextLex);
            flush();
            scan();
        }
        else {
            error("Invalid function name", scanner.getRow(), true);
        }
    }

}
