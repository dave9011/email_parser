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
        FIELD_NAME, FIELD_BODY, UNSTRUCTURED_FIELD_BODY, EMPTY_LINE, MESSAGE_BODY_DIVIDER, LAST_LINE
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
    
        //use group 1 for field names and field bodies
        addTokenInfoToList("^(.*?):[ \t]*", TokenType.FIELD_NAME);   //make this lazy, such as to capture only up to first colon
        addTokenInfoToList("^([^\\r\\n]+)\\r\\n", TokenType.FIELD_BODY);    
        
        addTokenInfoToList("^\r\n", TokenType.EMPTY_LINE);
        addTokenInfoToList("^(.+)\\u001a", TokenType.LAST_LINE);    //use group 1 for message line without EOF
                
    }
    
    private void addTokenInfoToList(String patternStr, TokenType tokenType){
        mTokenInfoList.add( new TokenInfo(Pattern.compile(patternStr), tokenType));
    }
    
    public void tokenize(String str){
        
        boolean couldHaveUnstructured = false;  //flag set true when we encounter an potential unstructure field
        boolean fieldNameWasLastMatch = false;  //flag for when we find a field name
        
        String email = str;
        
         mTokensList.clear();
        
        setUpPatterns();
        
        //Iterate through email
        while( !email.isEmpty() ){
         
            //We look for the first occurrence of any of our tokens
            for(TokenInfo tokenInfo : mTokenInfoList){
                
                //If the previous token was a field name we move on to look for a field body, instead of another field name
                if(fieldNameWasLastMatch){
                    fieldNameWasLastMatch = false;
                    continue;
                }
            
                //Look for a match within the current string
                Matcher matcher = tokenInfo.regexPattern.matcher(email);
                
                //If a match is found
                if( matcher.find() ){
                    
                    String lexeme;
                    
                    //Check if the lexeme we found is a field header name
                    if(tokenInfo.tokenType == TokenType.FIELD_NAME || tokenInfo.tokenType == TokenType.FIELD_BODY){
                        
                        /*  
                         *  If we found a field name we set the "fieldNameWasLastMatch" flag to true,
                         *  which will be indicate to skip looking for a field name in the next
                         *  loop iteration; the reason for this is to prevent looking for two
                         *  field names in a row
                         */
                        if(tokenInfo.tokenType == TokenType.FIELD_NAME){
                            fieldNameWasLastMatch = true;
                        }
                        
                        //If it is then we capture the 1st group only; to exclude the colon
                        lexeme = matcher.group(1).trim();
                        
                        //Check for the special case of unstructred header
                        for(String info_field : EmailParser.INFORMATIONAL_FIELD_NAMES){
                            
                            //if we found a header that could have an unstructured body, set a flag
                            if( lexeme.equals(info_field) ){
                                couldHaveUnstructured = true;  
                                break;
                            }
                            
                        }
                                             
                    } else {
                        
                        /*
                         *  Typically an empty line separates the email headers from the body, however
                         *  with the inclusion of the MIME multipart type there can be empty lines in 
                         *  the MIME body within its boundary; this method will check if we are at a MIME
                         *  multipart body and will process all lines within in up until it's final boundary
                         */
                       // if(tokenInfo.tokenType == TokenType.EMPTY_LINE){
                            
                       //     String lastLexeme = mTokensList.get(mTokensList.size()-1).lexeme;
                            
                       //     Matcher boundaryMatcher = Pattern.compile("").matcher(lastLexeme);
                            
                            //if()
                            
                        //}
                        
                        lexeme = matcher.group().trim();
                        
                    }
                    
                    //Add the token to our list
                    mTokensList.add( new Token(lexeme, tokenInfo.tokenType) );

                    //Remove our found lexeme from the email string
                    email = matcher.replaceFirst("");
                    
                    //Look for a potential unstructured field body
                    if(couldHaveUnstructured){
                        
                        couldHaveUnstructured = false; 
                        
                        //Set up matcher using pattern for unstructure field body
                        Matcher unstructuredMatcher = (Pattern.compile("^([^\\r\\n]*)\\r\\n")).matcher(email);
                        
                        //If we find an unstructured field body
                        if(unstructuredMatcher.find()){
                            lexeme = unstructuredMatcher.group(1);  
                            mTokensList.add( new Token(lexeme, TokenType.UNSTRUCTURED_FIELD_BODY) );
                            email = unstructuredMatcher.replaceFirst("");
                        }
                        
                    }
                    
                    break;
                    
                }
                
            }
            
        }
        
    }
    
    public void logTokens(JTextArea outputArea){
        int t=0;
        
        for( Token token : mTokensList ){ 
            log(t++ + "\t" + token.id + "\t\t" + token.lexeme);
            if( outputArea != null ){
                outputArea.append(token.lexeme + "\n");
            }
        }
        if( outputArea != null ){
            outputArea.setCaretPosition(0); //Return cursor to top of output area
        }
    }
    
    private void log(String str){
        System.out.println(str);
    }

}
