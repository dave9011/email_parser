
package email.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import org.apache.commons.io.FileUtils;

public class EmailParser {
    
    private final String[] HEADER_FIELD_NAMES = {
        //Fields specified in RFC2822 divided (not all included)
        "Date", "From", "Sender", "Reply-To", "To", "Cc", "Bcc", "Message-ID", "In-Reply-To",
        "References", "Subject", "Commments", "Keywords", "Return-Path"
    };
    
    private File mEmail;
    
    private Tokenizer mTokenizer;
    
    private List<HeaderField> mHeaderFieldsList;
    
    private JTextArea mOutputArea;  //if using UI, this is the text area where we would print output
    
    //Header fields we will output
    public final static String[] FIELD_NAMES_TO_OUTPUT = {  
        "Date", 
        "From", 
        "To",
        "Cc",
        "Bcc",
        "Subject",
        "Sender",
        "Reply-To",
        "Message-ID",
        "Return Path",
        "Received"
    }; 
    
    public final static String[] INFORMATIONAL_FIELD_NAMES = { "Subject", "Comments" };
    
    public EmailParser(JTextArea jTextArea) {
        mOutputArea = jTextArea;
    }
    
    public void parse(String filePath) {
        
        //open email source
        mEmail = new File(filePath);   
        
        try{
            //if file is not actually a file or does not exist, log the error and return from method
            if(!mEmail.isFile()){
                log("Error: File \"" + filePath + "\" is not a file or does not exist!");
                return;
            }
        } catch(SecurityException e){
            log("Exception thrown: " + e);
            return;
        }
            
        //Unfold the email, trim the result, and append an EOF character which
        //will be necessary when we tokenize
        String unfoldedEmail = unfold(mEmail, false);
        
        if(unfoldedEmail.isEmpty()){
            log("File is empty!");
            return;
        }

        unfoldedEmail = unfoldedEmail.trim().concat("\u001a");
        
        mTokenizer = new Tokenizer();
        mTokenizer.tokenize(unfoldedEmail);
        
        //mTokenizer.logTokens(mOutputArea);    //for testing purposes
       
        mHeaderFieldsList = new ArrayList<>();
        
        int tokensIndex = 0;
        headerFiledsLoop:
        while( !mTokenizer.checkIsTokenType(tokensIndex, Tokenizer.TokenType.EMPTY_LINE) ){
            
            if( mTokenizer.checkIsTokenType(tokensIndex, Tokenizer.TokenType.MULTIPART_BODY) ){
                tokensIndex++;  //move to next index to skip this token
                continue;
            } else {
                
                String headerFieldName = ""; //variable to hold the name we use to create our HeaderField object
                String headerFieldBody = ""; //variable to hold the body we use to create our HeaderField object
                
                if( !mTokenizer.checkIsTokenType(tokensIndex, Tokenizer.TokenType.FIELD_NAME) ){
                    log("Email header field name not found.");
                    return;
                }
                
                String fieldNameLexeme = mTokenizer.getTokenLexeme(tokensIndex++);  //get token lexeme and move to next token index
                if( fieldNameLexeme != null ) {    
                
                    //Check to see if we match any of the header fields we are interested in parsing out
                    for(String fieldName : FIELD_NAMES_TO_OUTPUT){
                        if(fieldNameLexeme.equals(fieldName)){
                            headerFieldName = fieldNameLexeme;
                            break;
                        }
                    }
                    
                } else {
                    log("Email header field name lexeme not found.");
                    return;
                }
                
                if(headerFieldName.isEmpty()){
                    tokensIndex++;
                    continue headerFiledsLoop;
                }

                if( !mTokenizer.checkIsTokenType(tokensIndex, Tokenizer.TokenType.FIELD_BODY) && !mTokenizer.checkIsTokenType(tokensIndex, Tokenizer.TokenType.UNSTRUCTURED_FIELD_BODY) ){
                    log("Email header body name not found.");
                    return;
                }
                
                String fieldBodyLexeme = mTokenizer.getTokenLexeme(tokensIndex++);  //get token lexeme and move to next token index
                if( fieldBodyLexeme != null ) {    
                    headerFieldBody = fieldBodyLexeme;
                } else {
                    log("Email header field body lexeme not found.");
                    return;
                }
                
                mHeaderFieldsList.add( new HeaderField(headerFieldName, headerFieldBody) );
                
            }
            
        }
        
        log("\nHere are the header filds you were looking for (\"Received:\" lines should be read from last to first):\n");
        
        for(HeaderField headerField : mHeaderFieldsList){
            log(headerField.getName() + ": " + headerField.getBody());
        }
        
    }

    /**
    * Unfold the email by removing any and all CRLF's
    * that are followed by whitespace
    * 
    * @param email            the file object that contains the 
    *                         email to be unfolded
    * @param keepWhitespace   true if the whitespace following a CRLF should 
    *                         remain the same, false to remove it
    * @return fileInString    a string containing the unfolded 
    *                         email
    */
    private static String unfold(File email, boolean keepWhitespace){
        
        try {
            
            //Put the email into a string
            String fileInString = FileUtils.readFileToString(email); 
            
            String unfoldingRegex = "([ \\t]*|(\\r\\n))([ \\t]+)";
            
            //Set up the regular expression for unfolding
            Pattern unfoldingPattern = Pattern.compile(unfoldingRegex);
            
            //Remove the CRLF's and whitespace depending on the value of
            //the parameter 'keepWhitespace'
            if(keepWhitespace){
                fileInString = fileInString.replaceAll(unfoldingPattern.pattern(), "$1");
            } else{
                fileInString = fileInString.replaceAll(unfoldingPattern.pattern(), " ");
            }
            
            return fileInString;
            
        } catch (IOException io_ex){ 
            JOptionPane.showMessageDialog(null, io_ex.getMessage(), "I/O Exception", JOptionPane.ERROR_MESSAGE);
        }
        
        return null;
        
    }
    
    private void log(String str){
        System.out.println(str);
    }
    
}
