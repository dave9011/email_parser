
package email.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
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

    //By default folding is disabled
    private boolean foldingEnabled = false;
    
    private ArrayList<String> emailLines = new ArrayList<>();
    private ArrayList<String> receivedLines = new ArrayList<>();
    
    private Pattern pattern;
    private Matcher matcher;
    
    private HashMap<String, String> hashLines = new HashMap<>();  
    
    private File mEmail;
    
    private JTextArea mOutputArea;
    
    private String[] fields = {  
        "Date:", 
        "From:", 
        "To:",
        "Subject:",
        "Delivery Date:",
        "X-Originating-IP:",
        "Received:",
        "Message-ID:",
        "Return Path:",
        "Mime Version:"
    }; 
    
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
        String unfoldedEmail = unfold(mEmail, false).trim().concat("\u001a");
        
        Tokenizer tokenizer = new Tokenizer();
        tokenizer.tokenize(unfoldedEmail);
        
        tokenizer.logTokens(mOutputArea);
        
        //initialize scanner, making its delimiter a CRLF to parse the header fields
        Scanner scanner = new Scanner(unfoldedEmail).useDelimiter("\r\n");
        
        //log("\n---------------Header Fields----------------");        COMMENTED THIS OUT FOR TESTING TOKENIZER
        
        /*
        scanLoop:
        while(scanner.hasNextLine()){

            String line = scanner.nextLine();

            //if(line.contains("\r") || line.contains("\n") || line.contains("\r\n")){
            //    log("***************a line break*********");
            //    break scanLoop;
            //}

            //
            if(line.isEmpty()){
                break scanLoop;
            }
               
            if(line.startsWith("Received:")){
                //line = line.replaceFirst("Received:", "");
                //line = line.trim();
                
                //log(line);        COMMENTED THIS OUT FOR TESTING TOKENIZER
                
                line = line.replaceFirst("\\s*Received:\\s*", "");
                emailLines.add(line); 
                receivedLines.add(line); 
                continue scanLoop;
            } 

            for(String fld : HEADER_FIELD_NAMES) { 

                if(line.startsWith(fld)) {
                    
                    //log(line);    COMMENTED THIS OUT FOR TESTING TOKENIZER
                    
                    emailLines.add(line); 

                    continue scanLoop;

                }

            }

        }
        
        //log("\n---------------Message Body----------------"); COMMENTED THIS OUT FOR TESTING TOKENIZER
        while(scanner.hasNextLine()){
            //log(scanner.nextLine());   COMMENTED THIS OUT FOR TESTING TOKENIZER
        }


        //for(Map.Entry entry : hashLines.entrySet()){
        //    log(entry.getKey() + ": " + entry.getValue());
        //}
        //log("Path of email: ");
        //for(String rec_line : receivedLines){
        //    log("\t" + rec_line);
        //}

        //log();
        * 
        * */
       
        
        
    }
    
    private void parseHeaders(){
        
        
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
            
            String CRLF = "\r\n";
            
            String unfoldingRegex = "([ \\t]*|(\\r\\n))([ \\t]+)";
            
            //Set up the regular expression for unfolding
            Pattern unfoldingPattern = Pattern.compile(unfoldingRegex);
            //Pattern unfoldingPattern = // Pattern.compile("\r\n([^\\S\\r\\n]+)");
            
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
