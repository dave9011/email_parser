/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package email.parser;


public class HeaderField {
    
    private String name;
    private String body;
    
    public HeaderField(String name, String body){
        this.name = name;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}
