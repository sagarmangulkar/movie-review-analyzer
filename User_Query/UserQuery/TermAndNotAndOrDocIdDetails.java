import java.util.HashMap;

public class TermAndNotAndOrDocIdDetails {

    public TermAndNotAndOrDocIdDetails(Operation operation, HashMap<Integer, String> docIdHashMap) {
        this.operation = operation;
        this.docIdHashMap = docIdHashMap;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public HashMap<Integer, String> getDocIdHashMap() {
        return docIdHashMap;
    }

    public void setDocIdHashMap(HashMap<Integer, String> docIdHashMap) {
        this.docIdHashMap = docIdHashMap;
    }

    public enum Operation {AND, NOTAND, OR}

    private Operation operation;
    private HashMap<Integer, String> docIdHashMap;
}
