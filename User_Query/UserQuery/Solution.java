import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;

public class Solution {
    private static final String[] stopWords = {"and", "a", "the", "an", "by", "from", "for", "hence", "of", "the", "with", "in",
            "within", "who", "when", "where", "why", "how", "whom", "have", "had", "has", "not", "for", "but", "do", "does", "done"};
    public static void main(String[] args){
        if (args.length <= 2){
            System.out.println("Kindly provide all three arguments.");
        }
        else {
            String dictionaryFileName = args[0];
            String postingFileName = args[1];
            String docsTableFileName = args[2];

            Scanner reader;
            while (true) {
                reader = new Scanner(System.in);
                System.out.println("Enter the query: ");
                String userQuery = reader.nextLine();
                if (userQuery.toLowerCase().equals("exit")){
                    break;
                }
                if (userQuery.toLowerCase().contains("and") || userQuery.toLowerCase().contains("or")) {
                    MainFunctionality(userQuery, dictionaryFileName, postingFileName, docsTableFileName);
                } else {
                    userQuery = userQuery + " and " + userQuery;
                    MainFunctionality(userQuery, dictionaryFileName, postingFileName, docsTableFileName);
                }
            }
            reader.close();
        }
    }

    private static void MainFunctionality(String queryArgument,
                                          String dictionaryFileName,
                                          String postingFileName,
                                          String docsTableFileName) {

        String query = queryArgument.toLowerCase();

        //data structure to store dictionary, postings list and doc table
        TreeMap<String, DFAndOffset> dictionaryHashMap = new TreeMap<>();
        List<Postings> postingsList = new ArrayList<>();
        HashMap<String, DocTableDetails> docTableHashMap = new HashMap<>();

        PrepareDictionaryHashMap(dictionaryFileName, dictionaryHashMap);
        PreparePostingList(postingFileName, postingsList);
        PrepareDocTableHashMap(docsTableFileName, docTableHashMap);

        List<TermAndNotAndOrDocIdDetails> docIdHashMapList = new ArrayList<>();
        if (query.contains("and") || query.contains("or")){
            //Oring
            String[] queryTerms = query.split(" ");
            TermAndNotAndOrDocIdDetails.Operation operation = null;
            //for (String term: queryTerms) {
            for (int k = 0; k < queryTerms.length; k++) {

                //adding operation as a flag for future differentiation
                if (queryTerms[k].equals("and")) {
                    operation = TermAndNotAndOrDocIdDetails.Operation.AND;
                    if (queryTerms[k+1].equals("not")) {
                        operation = TermAndNotAndOrDocIdDetails.Operation.NOTAND;
                    }
                }
                if (queryTerms[k].equals("or")) {
                    operation = TermAndNotAndOrDocIdDetails.Operation.OR;
                }

                HashMap<Integer, String> docIdHashMap = new HashMap<>();
                if (!queryTerms[k].equals("and")
                        && !queryTerms[k].equals("or")
                        && !(queryTerms[k].equals("not") && queryTerms[k-1].equals("and"))) {
                    //covers all docId from posting list depending on df of dictionary
                    if (dictionaryHashMap.get(queryTerms[k]) != null) {
                        for (int i = 0; i < dictionaryHashMap.get(queryTerms[k]).getDf(); i++) {
                            int offset = dictionaryHashMap.get(queryTerms[k]).getOffset();
                            String docId = postingsList.get(offset + i).getDocId();
                            //avoids repetition of docId
                            if (!docIdHashMap.containsValue(docId)) {
                                docIdHashMap.put(i, docId);
                            }
                        }
                        docIdHashMapList.add(new TermAndNotAndOrDocIdDetails(operation, docIdHashMap));
                    }
                }
            }
        } else {
            System.out.println("Invalid query, kindly use AND/OR.");
        }

        HashMap<Integer, QueryResult> resultHashMap = new HashMap<>();
        //collect final result for Oring
        for (int i = 0; i < docIdHashMapList.size(); i++) {
            int j = 0;
            for (int docIdEntry : docIdHashMapList.get(i).getDocIdHashMap().keySet()) {
                //avoiding repetitions of docIds
                if (!resultHashMap.containsValue(docIdHashMapList.get(i).getDocIdHashMap().get(docIdEntry))) {
                    if (docIdHashMapList.get(i).getOperation() == TermAndNotAndOrDocIdDetails.Operation.OR) {
                        String fileName = docIdHashMapList.get(i).getDocIdHashMap().get(docIdEntry);
                        QueryResult queryResult = new QueryResult(fileName,
                                docTableHashMap.get(fileName).getTitle(),
                                docTableHashMap.get(fileName).getReviewer(),
                                docTableHashMap.get(fileName).getRate(),
                                docTableHashMap.get(fileName).getSnippet());

                        resultHashMap.put(j++, queryResult);
                    }
                }
            }
        }

        //prepare and flag
        HashMap<String, Boolean> andFlagHashMap = new HashMap<>();
        for (int docIdFirstEntry : docIdHashMapList.get(0).getDocIdHashMap().keySet()) {
            if (!andFlagHashMap.containsKey(docIdHashMapList.get(0).getDocIdHashMap().get(docIdFirstEntry))){
                andFlagHashMap.put(docIdHashMapList.get(0).getDocIdHashMap().get(docIdFirstEntry), true);
            }
        }

        //setting the and flag
        for (int docIdFirstEntry : docIdHashMapList.get(0).getDocIdHashMap().keySet()) {
            for (int i = 1; i < docIdHashMapList.size(); i++) {
                String docIdToBeChecked = docIdHashMapList.get(0).getDocIdHashMap().get(docIdFirstEntry);
                if (!docIdHashMapList.get(i).getDocIdHashMap().containsValue(docIdToBeChecked)
                        && docIdHashMapList.get(i).getOperation() == TermAndNotAndOrDocIdDetails.Operation.AND){
                    andFlagHashMap.replace(docIdToBeChecked, false);
                }
                else if (docIdHashMapList.get(i).getDocIdHashMap().containsValue(docIdToBeChecked)
                        && docIdHashMapList.get(i).getOperation() == TermAndNotAndOrDocIdDetails.Operation.NOTAND){
                    andFlagHashMap.replace(docIdToBeChecked, false);
                }
            }
        }

        //collect final result for Anding
        int k = 0;
        for (String docId : andFlagHashMap.keySet()) {
//                System.out.println(docId + "- " + andFlagHashMap.get(docId));
            if (andFlagHashMap.get(docId)){
                String fileName = docId;
                QueryResult queryResult = new QueryResult(fileName,
                        docTableHashMap.get(fileName).getTitle(),
                        docTableHashMap.get(fileName).getReviewer(),
                        docTableHashMap.get(fileName).getRate(),
                        docTableHashMap.get(fileName).getSnippet());

                resultHashMap.put(k++, queryResult);
            }
        }
        WriteOutputToFile(queryArgument, resultHashMap);
    }

    private static void WriteOutputToFile(String query, HashMap<Integer, QueryResult> resultHashMap) {
        String fileName = "output.txt";
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(fileName, "UTF-8");
            writer.println(query);
            if (resultHashMap.isEmpty()){
                writer.println("NO RESULTS");
            }
            else {
                for (int key : resultHashMap.keySet()) {
                    writer.println("Document: " + resultHashMap.get(key).getFilename() + ", \n"
                        + "Title: " + resultHashMap.get(key).getTitle() + ", \n"
                        + "Reviewer: " + resultHashMap.get(key).getReviewer() + ", \n"
                        + "Rate: " + resultHashMap.get(key).getRate() + ", \n"
                        + "Snippet: " + resultHashMap.get(key).getSnippet() + "\n\n");
                }
            }
            writer.close();
            System.out.println("Result written to " + fileName + " file successfully. \nThank you...!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static void PrepareDocTableHashMap(String docsTableFileName, HashMap<String, DocTableDetails> docTableHashMap) {
        String docTableContent = ReadFiles(docsTableFileName);
        String[] docTableOneLineArray = docTableContent.split("-NextDoc-");

        for (String line : docTableOneLineArray) {
            String[] docIdTitleReviewerSnippetAndRate = line.split("-NextAttribute-");
            if (docIdTitleReviewerSnippetAndRate.length == 5) {
                String docId = docIdTitleReviewerSnippetAndRate[0].replace(" ", "").replace("\n", "");
                String title = docIdTitleReviewerSnippetAndRate[1].replace(" ", "");
                String reviewer = docIdTitleReviewerSnippetAndRate[2].replace(" ", "");
                String snippet = docIdTitleReviewerSnippetAndRate[3];
                String rate = docIdTitleReviewerSnippetAndRate[4].replace(" ", "");
                docTableHashMap.put(docId, new DocTableDetails(title, reviewer, snippet, rate));
            } else {
                break;
            }
        }
    }

    private static void PreparePostingList(String postingFileName, List<Postings> postingsList) {
        String postingContent = ReadFiles(postingFileName);
        String[] postingOneLineArray = postingContent.split("\n");
        for (String line : postingOneLineArray) {
            String[] docIdAndTf = line.split(" ");
            String docId = docIdAndTf[0].replace(",", "");
            int tf = Integer.parseInt(docIdAndTf[1].replace(",", ""));
            postingsList.add(new Postings(docId, tf));
        }
    }

    private static void PrepareDictionaryHashMap(String dictionaryFileName, TreeMap<String, DFAndOffset> dictionaryHashMap) {
        String dictionaryContent = ReadFiles(dictionaryFileName);
        String[] dictionaryOneLineArray = dictionaryContent.split("\n");
        for (String line : dictionaryOneLineArray) {
            String[] termDFAndOffset = line.split(" ");
            String term = termDFAndOffset[0].replace(",", "");
            int df = Integer.parseInt(termDFAndOffset[1].replace(",", ""));
            int offset = Integer.parseInt(termDFAndOffset[2].replace(",", ""));
            dictionaryHashMap.put(term, new DFAndOffset(df, offset));
        }
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
