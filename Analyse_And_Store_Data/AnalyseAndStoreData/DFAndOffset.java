import java.util.List;

public class DFAndOffset{
    public DFAndOffset(int df, int offset, List<String> docId) {
        this.df = df;
        this.offset = offset;
        this.docId = docId;
    }

    public int getDf() {
        return df;
    }

    public void setDf(int df) {
        this.df = df;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public List<String> getDocId() {
        return docId;
    }

    public void setDocId(List<String> docId) {
        this.docId = docId;
    }

    private int df;
    private int offset;
    private List<String> docId;
}