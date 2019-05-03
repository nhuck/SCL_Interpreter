/**
 * AssignmentStatement.java
 * Noah Huck
 * CS4308
 * Section 03
 * 29 April 2019
 */

package pkg;

public class AssignmentStatement {

    public AssignmentStatement(Identifier id, String value){
        id.setValue(value);
//        System.out.println("Value changed: "+id.toString());
    }

}
