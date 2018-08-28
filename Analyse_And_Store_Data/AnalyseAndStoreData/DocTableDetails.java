public class DocTableDetails {

    public DocTableDetails(String title, String reviewer, String snippet, String rate) {
        this.title = title;
        this.reviewer = reviewer;
        this.snippet = snippet;
        this.rate = rate;
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

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    private String title;
    private String reviewer;
    private String snippet;
    private String rate;
}
