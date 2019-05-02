/**
 * SCLMain.java
 * Noah Huck
 * CS4308
 * Section 03
 * 25 February 2019     Revised 25 March 2019
 */

package pkg;

public class Interpreter {

    public static void main(String[] args) {
        //file name is passed in as argument
         
        Parser p = new Parser(args[0]);
        
        System.out.printf("\nNow parsing from file: %s \n\n", args[0]);
        
        while (!p.endOfFileReached()){      //executes until end of file is reached
            p.parseProgram();
            // parseProgram parses for 1 complete function at a time
            // loop will execute until all functions have been parsed
        }
        
        System.out.println("\n\n\n\n");
        
        Parser p2 = new Parser(args[0]);
        p2.running = true;
        while (!p2.endOfFileReached()){      //executes until end of file is reached
            p2.parseProgram();
            // parseProgram parses for 1 complete function at a time
            // loop will execute until all functions have been parsed
        }
        System.out.println("\nEnd of file reached.");

     //   for testing purposes:
//        System.out.println("\nIdentifier Table: \n\n");
//        p2.printIdTable();
    }
    
}
