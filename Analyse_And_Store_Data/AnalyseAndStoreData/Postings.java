public class Postings {
    public Postings(String docId, int tf) {
        this.docId = docId;
        this.tf = tf;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public int getTf() {
        return tf;
    }

    public void setTf(int tf) {
        this.tf = tf;
    }

    private String docId;
    private int tf;
}
