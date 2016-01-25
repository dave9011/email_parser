/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package email.parser;

public class EmailParserDriver {
    public static void main(String args[]){
        EmailParser parser = new EmailParser(null); 
        parser.parse("email4.txt");
    }
}
