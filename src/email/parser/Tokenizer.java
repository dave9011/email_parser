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
    
    private final String FIELD_NAME_REGEX = "^(.*?):[ \t]*";
    private static final String FIELD_BODY_REGEX = "^([^\\r\\n]+)\\r\\n";
    private static final String UNSTRUCTURED_FIELD_BODY_REGEX = "^([^\\r\\n]*)\\r\\n";
    private static final String EMPTY_LINE_REGEX = "^\r\n"; //CRLF
    private static final String LAST_LINE_REGEX = "^(.+)\\u001a";
    private static final String MULTIPART_HEADER_REGEX = "^multipart\\/.+boundary=(.*)$";
    
    List<TokenInfo> mTokenInfoList;
    private List<Token> mTokensList;
    
    public static enum TokenType {
        FIELD_NAME, FIELD_BODY, UNSTRUCTURED_FIELD_BODY, 
        MULTIPART_BODY,
        MULTIPART_BOUNDARY_DELIMITER,   // could use in later implementation
        EMPTY_LINE, 
        MESSAGE_BODY,
        LAST_LINE
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
        addTokenInfoToList(FIELD_NAME_REGEX, TokenType.FIELD_NAME);   //make this lazy, such as to capture only up to first colon
        addTokenInfoToList(FIELD_BODY_REGEX, TokenType.FIELD_BODY);    
        addTokenInfoToList(EMPTY_LINE_REGEX, TokenType.EMPTY_LINE);
        addTokenInfoToList(LAST_LINE_REGEX, TokenType.LAST_LINE);    //use group 1 for message line without EOF
                
    }
    
    private void addTokenInfoToList(String patternStr, TokenType tokenType){
        mTokenInfoList.add( new TokenInfo(Pattern.compile(patternStr), tokenType));
    }
    
    
    /**
    * Search through email looking to match our patterns from which
    * we will extract our tokens
    * 
    * @param str    string representation of email to tokenize
    * 
    */
    public void tokenize(String str){
        
        String email = str;
        
        mTokensList.clear();
        
        setUpPatterns();
        
        boolean couldHaveUnstructured = false;  //flag set true when we encounter an potential unstructure field
        boolean fieldNameWasLastMatch = false;  //flag for when we find a field name
        boolean isMultiPart = false;            //flag for when we encounter a multipart header
        boolean foundEmailBody = false;
        
        String multiPartBoundary = "";
        
        //Iterate through email
        while( !email.isEmpty() ){
            
            if( foundEmailBody ){
                mTokensList.add( new Token(email, TokenType.MESSAGE_BODY) );  //Add the token to our list
                break;
            }
            
            if( couldHaveUnstructured ){
                if( hasUnstructured(email) ){
                    fieldNameWasLastMatch = false;  //we found a field body; we reset the flag to look for a field name next
                    email = getUnstructuredFieldBody(email);    //look for a potential unstructured field body
                }
                couldHaveUnstructured = false;  //reset flag
            } 
            else if ( isMultiPart ) {
                email = getMultiPartBody(email, multiPartBoundary);
                isMultiPart = false;
            }
            else {
         
                //We look for the first occurrence of any of our tokens
                for(TokenInfo tokenInfo : mTokenInfoList){

                    //If the previous token was a field name we move on to look for a field body, instead of another field name
                    if( fieldNameWasLastMatch ){
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
                                    fieldNameWasLastMatch = false;
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
                            if(tokenInfo.tokenType == TokenType.EMPTY_LINE){

                                String lastLexeme = mTokensList.get(mTokensList.size()-1).lexeme;

                                /*
                                 *  Matcher using a regex to match a multipart header, capturing the boundary value;
                                 *  this value is used to find the boundary delimiter
                                 */
                                Matcher boundaryMatcher = Pattern.compile(MULTIPART_HEADER_REGEX).matcher(lastLexeme);

                                //If we encounter a multipart header, raise a flag
                                if( boundaryMatcher.find() ){
                                    isMultiPart = true;
                                    multiPartBoundary = boundaryMatcher.group(1);
                                } else {
                                    foundEmailBody = true;
                                }

                            }

                            lexeme = matcher.group().trim();

                        }
                        
                        mTokensList.add( new Token(lexeme, tokenInfo.tokenType) );  //Add the token to our list
                        
                        email = matcher.replaceFirst("");   //Remove our found lexeme from the email string

                        break;

                    }

                }

            }
            
        }
        
    }
    
    
    private boolean hasUnstructured(String email){
        //Set up matcher using pattern for unstructure field body
        Matcher unstructuredMatcher = (Pattern.compile(UNSTRUCTURED_FIELD_BODY_REGEX)).matcher(email);
        //If we find an unstructured field body
        if(unstructuredMatcher.find()){
            return true;   
        }
        return false;
    }
    
    //Retrieve the unstructured field body
    private String getUnstructuredFieldBody(String email) {
        String lexeme;
        //Set up matcher using pattern for unstructure field body
        Matcher unstructuredMatcher = (Pattern.compile(UNSTRUCTURED_FIELD_BODY_REGEX)).matcher(email);
        //If we find an unstructured field body
        if(unstructuredMatcher.find()){
            lexeme = unstructuredMatcher.group(1);  
            mTokensList.add( new Token(lexeme, TokenType.UNSTRUCTURED_FIELD_BODY) );
            email = unstructuredMatcher.replaceFirst("");
        }
        return email;
    }
    
     /**
    * Extract the body of a multi-part field; this version of the parse wont address
    * this part so we'll store the whole multi-part body as one token; this means if
    * nested multiparts are present then the inner multipart fields will be included 
    * within the lexeme of the single token for the outermost multipart field
    * 
    * @param str        string representation of email to tokenize
    * 
    * @param boundary   value of the boundary; used to match a boundary delimiter
    * 
    * @return email     the remainder of the email, excluding the extracted multipart body
    * 
    */
    private String getMultiPartBody(String email, String boundary) {
        
        
        //Get matcher for multipart body
        Matcher multipartMatcher = Pattern.compile("^((.|\\n)*--" + boundary + ")[ \\t]*\\r\\n").matcher(email);    
        
        if(multipartMatcher.find()){
            
            String multipartBody = multipartMatcher.group(1);  //Get multipart body, still missing the first line
            
            String firstLineOfBody = mTokensList.get(mTokensList.size()-1).lexeme;   //Get lexeme of last token, i.e. first line
        
            mTokensList.remove(mTokensList.size()-1);   //Remove the last token from the tokens list
            
            String fullLexeme = firstLineOfBody + multipartBody; //Append first line to rest of body
            
            mTokensList.add( new Token(fullLexeme, TokenType.MULTIPART_BODY) );     //Add the token to our list

            //Remove our found lexeme from the email string
            email = multipartMatcher.replaceFirst(""); 
            
        }
        
        return email;
    }
    
    public TokenType getTokenType(int index){
        if(mTokensList == null || mTokensList.isEmpty()){
            return null;
        }
        try {
            return mTokensList.get(index).id;
        } catch( IndexOutOfBoundsException e ){
            log(e.getMessage());
            return null;
        }
    }
    
    public String getTokenLexeme(int index){
        if(mTokensList == null || mTokensList.isEmpty()){
            return null;
        }
        try {
            return mTokensList.get(index).lexeme;
        } catch( IndexOutOfBoundsException e ){
            log(e.getMessage());
            return null;
        }
    }
    
    public boolean checkIsTokenType(int index, TokenType tokenType){
        if(mTokensList == null || mTokensList.isEmpty()){
            return false;
        }
        try {
            return ( mTokensList.get(index).id == tokenType) ;
        } catch( IndexOutOfBoundsException e ){
            log(e.getMessage());
            return false;
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
