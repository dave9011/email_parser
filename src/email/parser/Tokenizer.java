/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package email.parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class Tokenizer {
    
    List<TokenInfo> mTokenInfoList;
    List<Token> mTokensList;
    
    public enum TokenType {
        LINE, MESSAGE, MESSAGE_BODY_DIVIDER ,BODY
    }
    
    private class TokenInfo {
        //Store the regex pattern
        public final Pattern regexPattern;
        public final TokenType token;
        
        public TokenInfo(Pattern regexPattern, TokenType tokenType){
            this.regexPattern = regexPattern;
            this.token = tokenType;
        }
        
    }
    
    private class Token {
        public String lexeme;
        public TokenType id;
    }
    
    public Tokenizer(){
        mTokenInfoList = new LinkedList<>();
        mTokensList = new ArrayList<>();
    }
    
    private void setUpPatterns(){
        //mTokenInfoList.add("", TokenType.BODY); 
        
        //mTokenInfoList.add("Date"); 
                
       /* "Date:", 
        "From:", 
        "To:",
        "Subject:",
        "Delivery Date:",
        "X-Originating-IP:",
        "Received:",
        "Message-ID:",
        "Return Path:",
        "Mime Version:"    
        */
                
    }
    
    private void separateHeaderAndBody(){
        
        
        
    }
    

}
