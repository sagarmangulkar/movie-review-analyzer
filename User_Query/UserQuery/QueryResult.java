public class QueryResult {

    public QueryResult(String filename, String title, String reviewer, String rate, String snippet) {
        this.filename = filename;
        this.title = title;
        this.reviewer = reviewer;
        this.rate = rate;
        this.snippet = snippet;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    private String filename;
    private String title;
    private String reviewer;
    private String rate;
    private String snippet;
}
