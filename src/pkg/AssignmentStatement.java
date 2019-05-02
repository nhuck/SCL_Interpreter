
package pkg;

/**
 *
 * @author Noah Huck
 * May 1, 2019
 */
public class AssignmentStatement {

    public AssignmentStatement(Identifier id, String value){
        id.setValue(value);
        System.out.println("Value changed: "+id.toString());
    }

}
