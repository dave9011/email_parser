/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package email.parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextArea;

public class Tokenizer {
    
    List<TokenInfo> mTokenInfoList;
    List<Token> mTokensList;
    
    public enum TokenType {
        FIELD_NAME, FIELD_LINE, MESSAGE, MESSAGE_BODY_DIVIDER ,BODY, EMPTY_LINE, LAST_LINE
    }
    
    private class TokenInfo {
        //Store the regex pattern
        public final Pattern regexPattern;
        public final TokenType tokenType;
        
        public TokenInfo(Pattern regexPattern, TokenType tokenType){
            this.regexPattern = regexPattern;
            this.tokenType = tokenType;
        }
        
    }
    
    private class Token {
        public String lexeme;
        public TokenType id;
        
        public Token(String lexeme, TokenType tokenType){
            this.lexeme = lexeme;
            this.id = tokenType;
        }
        
    }
    
    public Tokenizer(){
        mTokenInfoList = new LinkedList<>();
        mTokensList = new ArrayList<>();
    }
    
    private void setUpPatterns(){
    
        //use group 1 for field name
        addTokenInfoToList("^(.*): ", TokenType.FIELD_NAME);
        
        addTokenInfoToList("^(.+)(\r\n)", TokenType.FIELD_LINE);
        addTokenInfoToList("^(\r\n)", TokenType.EMPTY_LINE);
        
        //use group 1 for message line without EOF
        addTokenInfoToList("^(.+)\\u001a", TokenType.LAST_LINE);
             
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
    
    private void addTokenInfoToList(String patternStr, TokenType tokenType){
        mTokenInfoList.add( new TokenInfo(Pattern.compile(patternStr), tokenType));
    }
    
    public void tokenize(String str){
        
        String email = str;
        
         mTokensList.clear();
        
        setUpPatterns();
        
        //Iterate through email
        while(!email.isEmpty()){
         
            //We look for the first occurrence of any of our tokens
            for(TokenInfo tokenInfo : mTokenInfoList){
            
                //Look for a match within the current string
                Matcher matcher = tokenInfo.regexPattern.matcher(email);
                
                //If a match is found
                if(matcher.find()){
                    
                    String lexeme;
                    if(tokenInfo.tokenType == TokenType.FIELD_NAME){
                        lexeme = matcher.group(1).trim();
                    }else {
                        lexeme = matcher.group().trim();
                    }
                    

                    //Add the token to our list
                    mTokensList.add( new Token(lexeme, tokenInfo.tokenType) );

                    //Remove our found lexeme from the email string
                    email = matcher.replaceFirst("");
                    
                    break;
                    
                }
                
            }
            
        }
        
    }
    
    public void logTokens(JTextArea outputArea){
        int t=0;
        
        for(Token token : mTokensList){
            log(t++ + "\t" + token.lexeme);
            outputArea.append(token.lexeme + "\n");
        }
        outputArea.setCaretPosition(0); //Return cursor to top of output area
        
    }
    
    private void log(String str){
        System.out.println(str);
    }

}
