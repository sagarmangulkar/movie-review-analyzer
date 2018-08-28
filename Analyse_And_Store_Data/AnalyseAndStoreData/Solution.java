import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.sql.Connection;
import java.sql.DriverManager;

public class Solution {
    private static final String[] stopWords = {"and", "a", "the", "an", "by", "from", "for", "hence", "of", "the", "with", "in",
            "within", "who", "when", "where", "why", "how", "whom", "have", "had", "has", "not", "for", "but", "do", "does", "done"};
    private static final String[] positiveWords = {"best", "exciting", "outstanding"};
    private static final String[] negativeWords = {"dull", "boring", "disappointing", "failure"};

    public static void main(String[] args) throws Exception{
        if (args.length <= 3){
            System.out.println("Kindly provide all four arguments.");
        }
        else {
            String inputFileLocation = args[0];
            String dictionaryFileName = args[1];
            String postingFileName = args[2];
            String docsTableFileName = args[3];

            File folder = new File(inputFileLocation);
            File[] listOfFiles = folder.listFiles();

            System.out.println("Reading files from location: " + inputFileLocation);
            HashMap<String, String> fileContentHashMap = new HashMap<>();
            if (listOfFiles != null) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    fileContentHashMap.put(listOfFiles[i].getName(), ReadFiles(inputFileLocation + "/" + listOfFiles[i].getName()));
                }
            }

            fileContentHashMap.remove(".DS_Store");
            TreeMap<String, DFAndOffset> dictionaryHashMap = new TreeMap<>();
            List<Postings> postingsList = new ArrayList<>();
            HashMap<String, Integer> tfTermsHashMap = new HashMap<>();
            HashMap<String, DocTableDetails> docTableHashMap = new HashMap<>();

            //iterates all files
            for (String fileName : fileContentHashMap.keySet()) {
                String fileContent = fileContentHashMap.get(fileName);

                //docTable
                String title = fileContent.substring(fileContent.indexOf("<TITLE>")+7, fileContent.indexOf("</TITLE>"));
                String reviewer = fileContent.substring(fileContent.indexOf("review by")+10, fileContent.indexOf("review by")+10+15);
                String snippet = null;
                if (fileContent.contains("Capsule review")) {
                    snippet = fileContent.substring(fileContent.indexOf("Capsule review") + 14,
                            fileContent.indexOf("Capsule review")+14+300);
                } else {
                    snippet = fileContent.substring(fileContent.indexOf("<P>")+3,
                            fileContent.indexOf("<P>")+3+300);
                }
                String rate = null;
                String positiveOrNegative = null;
                if (fileContent.contains("-4 to +4 scale")){
                    positiveOrNegative = fileContent.substring(fileContent.indexOf("-4 to +4 scale")-10, fileContent.indexOf("-4 to +4 scale")-10+1);
                } else {
                    positiveOrNegative = "NA";
                }

                int ratingPoints = 0;
                if (positiveOrNegative.equals("+") || positiveOrNegative.equals("0")){
                    rate = "P";
                } else if (positiveOrNegative.equals("-")){
                    rate = "N";
                } else {
                    for (int i = 0; i < positiveWords.length; i++) {
                        if (snippet.contains(positiveWords[i])){
                            ratingPoints++;
                        }
                        if (snippet.contains(negativeWords[i])){
                            ratingPoints--;
                        }
                    }
                    if (ratingPoints > 0){
                        rate = "P";
                    }
                    if (ratingPoints < 0){
                        rate = "N";
                    } else {
                        rate = "NA";
                    }
                }
                docTableHashMap.put(fileName, new DocTableDetails(title, reviewer, snippet.replaceAll("\\<.*?>", " "), rate));
                TreeMap<String, Integer> index = PrepareIndex(fileContent, tfTermsHashMap);
                //ads each term to dictionary
                for (String term : index.keySet()) {
                    if (!dictionaryHashMap.containsKey(term)) {
                        List<String> fileNameList = new ArrayList<>();
                        fileNameList.add(fileName);
                        dictionaryHashMap.put(term, new DFAndOffset(1, 0, fileNameList));
                    } else {
                        int newDf = dictionaryHashMap.get(term).getDf() + 1;
                        DFAndOffset newDfAndOffsetObj = dictionaryHashMap.get(term);
                        newDfAndOffsetObj.setDf(newDf);
                        List<String> fileNameListTemp = newDfAndOffsetObj.getDocId();
                        fileNameListTemp.add(fileName);
                        newDfAndOffsetObj.setDocId(fileNameListTemp);
                        dictionaryHashMap.replace(term, newDfAndOffsetObj);
                    }
                }
            }

            //update offsets
            int newOffset = 0;
            for (String term : dictionaryHashMap.keySet()) {
                DFAndOffset dfAndOffsetObj = dictionaryHashMap.get(term);
                dfAndOffsetObj.setOffset(newOffset);
                dictionaryHashMap.replace(term, dfAndOffsetObj);
                int df = dictionaryHashMap.get(term).getDf();
                newOffset = dictionaryHashMap.get(term).getOffset() + df;

                //adding postings
                for (int i = 0; i < dictionaryHashMap.get(term).getDocId().size(); i++) {
                    if (tfTermsHashMap.get(term) != null) {
                        postingsList.add(new Postings(dictionaryHashMap.get(term).getDocId().get(i), tfTermsHashMap.get(term)));
                    } else {
                        postingsList.add(new Postings(dictionaryHashMap.get(term).getDocId().get(i), 1));
                    }
                }
            }
            WriteDictionaryToFile(dictionaryHashMap, dictionaryFileName);
            WritePostingsToFile(postingsList, postingFileName);
            WriteDocsTableToFile(docTableHashMap, docsTableFileName);
        }
    }

    private static void spInsertDictionary(String term, int documentFrequency, int offset, Connection conn) throws Exception{
        CallableStatement callableStatement = conn.prepareCall("{call SP_insert_dictionary(?, ?, ?)}");
        callableStatement.setString(1, term);
        callableStatement.setInt(2, documentFrequency);
        callableStatement.setInt(3, offset);
        callableStatement.execute();
        callableStatement.close();
    }

    private static Connection openDatabaseConnection() throws Exception{
        Class.forName("oracle.jdbc.driver.OracleDriver");
        Connection conn =  DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE","sagar","Sagar");
        return conn;
    }

    private static void WriteDocsTableToFile(HashMap<String, DocTableDetails> docTableHashMap, String docsTableFileName) {
        PrintWriter writer = null;
        try {
            Connection conn = openDatabaseConnection();
            System.out.println("Inserting in Documents Table...");
            writer = new PrintWriter(docsTableFileName, "UTF-8");
            for (String sortedTerm : docTableHashMap.keySet()) {
                String title = docTableHashMap.get(sortedTerm).getTitle();
                String reviewer = docTableHashMap.get(sortedTerm).getReviewer();
                String snippet = docTableHashMap.get(sortedTerm).getSnippet();
                String rate = docTableHashMap.get(sortedTerm).getRate();
                writer.println(sortedTerm + "-NextAttribute-"
                        + title + "-NextAttribute-"
                        + reviewer + "-NextAttribute-"
                        + snippet + "-NextAttribute-"
                        + rate + "-NextDoc-");
                spInsertDocumentsTable(sortedTerm, title, reviewer, snippet, rate, conn);
            }
            writer.close();
            conn.close();
            System.out.println("DocsTable written to " + docsTableFileName + " file successfully. \nThank you...!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void spInsertDocumentsTable(String documentId, String title, String reviewer, String snippet, String rate, Connection conn) throws Exception{
        CallableStatement callableStatement = conn.prepareCall("{call SP_insert_documents_table(?, ?, ?, ?, ?)}");
        callableStatement.setString(1, documentId);
        callableStatement.setString(2, title);
        callableStatement.setString(3, reviewer);
        callableStatement.setString(4, snippet);
        callableStatement.setString(5, rate);
        callableStatement.execute();
        callableStatement.close();
    }

    private static void WritePostingsToFile(List<Postings> postingsList, String postingFileName) {
        PrintWriter writer = null;
        try {
            Connection conn = openDatabaseConnection();
            System.out.println("Inserting in Postings...");
            writer = new PrintWriter(postingFileName, "UTF-8");
            for (int i = 0; i < postingsList.size(); i++) {
                String documentId = postingsList.get(i).getDocId();
                int termFrequency = postingsList.get(i).getTf();
                writer.println(documentId + ", " + termFrequency + ", ");
                spInsertPostings(documentId, termFrequency, conn);
            }
            writer.close();
            conn.close();
            System.out.println("Posting Lists written to " + postingFileName + " file successfully.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void spInsertPostings(String documentId, int termFrequency, Connection conn) throws SQLException {
        CallableStatement callableStatement = conn.prepareCall("{call SP_insert_postings(?, ?)}");
        callableStatement.setString(1, documentId);
        callableStatement.setInt(2, termFrequency);
        callableStatement.execute();
        callableStatement.close();
    }

    private static void WriteDictionaryToFile(TreeMap<String, DFAndOffset> dictionaryHashMap, String dictionaryFileName) {
        PrintWriter writer = null;
        try {
            Connection conn = openDatabaseConnection();
            System.out.println("Inserting in Dictionary...");
            writer = new PrintWriter(dictionaryFileName, "UTF-8");
            for (String sortedTerm : dictionaryHashMap.keySet()) {
                int documentFrequency = dictionaryHashMap.get(sortedTerm).getDf();
                int offset = dictionaryHashMap.get(sortedTerm).getOffset();
                writer.println(sortedTerm + ", " + documentFrequency + ", " + offset + ", ");
                spInsertDictionary(sortedTerm, documentFrequency, offset, conn);
            }
            writer.close();
            conn.close();
            System.out.println("Dictionary written to " + dictionaryFileName + " file successfully.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static TreeMap<String, Integer> PrepareIndex(String fileContent, HashMap<String, Integer> tfTermHashMap) {
        String noHTMLTags = fileContent.replaceAll("\\<.*?>", " ");
        String noFullStops = noHTMLTags.replaceAll("[\\-\\']", " ");

        noFullStops = noFullStops.replaceAll("\\s\\(", " ");
        noFullStops = noFullStops.replaceAll("\\(\\s", " ");

        noFullStops = noFullStops.replaceAll("\\s\\)", " ");
        noFullStops = noFullStops.replaceAll("\\)\\s", " ");
        noFullStops = noFullStops.replaceAll("\\).\\s", " ");

        noFullStops = noFullStops.replaceAll("\\s\\'", " ");
        noFullStops = noFullStops.replaceAll("\\'\\s", " ");

        noFullStops = noFullStops.replaceAll("\\s\\[", " ");
        noFullStops = noFullStops.replaceAll("\\[\\s", " ");

        noFullStops = noFullStops.replaceAll("\\s\\]", " ");
        noFullStops = noFullStops.replaceAll("\\]\\s", " ");

        noFullStops = noFullStops.replaceAll("\\,\\s", " ");

        noFullStops = noFullStops.replaceAll("[\\s][\\w][\\s]", " ");

        noFullStops = noFullStops.replaceAll("\\.+\\s", " ");

        noFullStops = noFullStops.replaceAll("\\?\\s", " ");

        noFullStops = noFullStops.replaceAll("\\:\\s", " ");

        noFullStops = noFullStops.replaceAll("\\;\\s", " ");

        noFullStops = noFullStops.replaceAll("\\!\\s", " ");

        noFullStops = noFullStops.replaceAll("\\'", "");

        noFullStops = noFullStops.replaceAll("\"", "");

        String terms[] = noFullStops.split("\\s+");
        TreeMap<String, Integer> index = new TreeMap<>();
        HashMap<Integer, String> stopWordsHashMap = new HashMap<>();
        for (int i = 0; i < stopWords.length; i++) {
            stopWordsHashMap.put(i, stopWords[i]);
        }

        //processing index
        String term;
        int j = 0;
        int tf = 1;
        for (int i = 0; i < terms.length; i++) {
            if (!terms.equals(" ")) {
                term = Stem(terms[i].toLowerCase());
                if (!stopWordsHashMap.containsValue(term)) {
                    if (!index.containsKey(term)) {
                        index.put(term, j);
                        j++;
                    } else {
                        if (!tfTermHashMap.containsKey(term)) {
                            tfTermHashMap.put(term, ++tf);
                        } else {
                            tfTermHashMap.replace(term, tfTermHashMap.get(term)+1);
                        }
                    }
                }
            }
        }
        return index;
    }

    private static String Stem(String term){
        String lastOneChar;
        String lastTwoChar;
        String lastThreeChar;
        String lastFourChar;
        if (term.length() >= 4) {
            lastThreeChar = term.substring(term.length() - 3, term.length());
            lastFourChar = term.substring(term.length() - 4, term.length());
            if (!lastFourChar.equals("eies") && !lastFourChar.equals("aies")) {
                if (lastThreeChar.equals("ies")) {
                    term = term.replace(lastThreeChar, "y");
                }
            }
        }
        if (term.length() >= 3) {
            lastTwoChar = term.substring(term.length() - 2, term.length());
            lastThreeChar = term.substring(term.length() - 3, term.length());
            if (!lastThreeChar.equals("aes") && !lastThreeChar.equals("ees") && !lastThreeChar.equals("oes")) {
                if (lastTwoChar.equals("es")) {
                    term = term.replace(lastTwoChar, "e");
                }
            }
        }
        if (term.length() >= 2) {
            lastOneChar = term.substring(term.length() - 1, term.length());
            lastTwoChar = term.substring(term.length() - 2, term.length());
            if (!lastTwoChar.equals("us") && !lastTwoChar.equals("ss")) {
                if (lastOneChar.equals("s")) {
                    term = term.replace(lastOneChar, "");
                }
            }
        }
        return term;
    }

    private static String ReadFiles(String inputFileName){
        BufferedReader br = null;
        String fileContent = null;
        try {
            br = new BufferedReader(new FileReader(inputFileName));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            fileContent = sb.toString();
            br.close();
        }
        catch (FileNotFoundException e) {
            System.out.println(inputFileName + " file not present.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileContent;
    }
}
