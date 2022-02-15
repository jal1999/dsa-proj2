package main;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Object that will be representing each of the wikipedia
 * pages.
 */
public class Webpage {
    /**
     * The file that we will be writing the webpage's data to.
     */
    RandomAccessFile file;
    /**
     * The b-tree associated with this webpage.
     */
    BTree tree;
    /**
     * The link to this webpage.
     */
    String link;
    /**
     * The title of this webpage.
     */
    String title;
    /**
     * The text of the body  of this wikipedia page.
     */
    String text;

    Webpage(RandomAccessFile aFile, BTree aTree, String aLink) throws IOException {
        link = aLink;
        title = Jsoup.connect(link).get().title();
        file = aFile;
        tree = aTree;
        text = Jsoup.connect(link).get().getElementsByTag("p").text().replaceAll("\\p{Punct}",
                "").toLowerCase();
        addAllWordsToTree();
    }

    /**
     * Method to add all words on a wikipedia page to its b-tree.
     * @throws IOException if disk read or disk write throws an IO exception.
     */
    void addAllWordsToTree() throws IOException {
        String bText = Jsoup.connect(link).get().body().text();
        StringTokenizer tz = new StringTokenizer(bText);
        while (tz.hasMoreTokens()) {
            String token = tz.nextToken();
            tree.insert(token);
        }
    }

    /**
     * Method to calculate the term frequency of a given word in this webpage.
     * @param word the word we are counting the frequency of.
     * @return log(freq + 1).
     */
    double tf(String word) throws IOException {
        double freq = tree.search(tree.root, word).count;
        return Math.log10(freq + 1);
    }

    /**
     * Method to calculate the inverse document frequency of the given word.
     * @param s the word we are calculating the IDF for.
     * @param pages the corpus.
     * @return the IDF calculation.
     */
    double idf(String s, Webpage pages[]) throws IOException {
        double corpusSize = pages.length + 1;
        double count = 0;
        for (int i = 0; i < pages.length; i++) {
            Node n = pages[i].tree.diskRead(0);
            if (pages[i].tree.search(n, s) != null) ++count;
        }
        return Math.log10(corpusSize / (count + 1));
    }

    /**
     * Method to calculate the TF-IDF of a given word.
     * @param s the word we are calculating the TF-IDF for.
     * @param pages the corpus.
     * @return the TF-IDF calculation.
     * @throws IOException if disk read throws an IO exception.
     */
    double tfidf(String s, Webpage[] pages) throws IOException {
        return tf(s) * idf(s, pages);
    }

    /**
     * Method to get the keywords of this document.
     * @param pages the corpus.
     * @return a list of the keywords of this document.
     * @throws IOException if disk read or disk write throws an IO exception.
     */
    ArrayList<String> getKeywords(Webpage[] pages) throws IOException {
        ArrayList<String> keywords = new ArrayList<>();
        StringTokenizer tz = new StringTokenizer(text);
        while (tz.hasMoreTokens()) {
            String tok = tz.nextToken();
            double tfidf = tfidf(tok, pages);
            if (tfidf > .3 && !keywords.contains(tok)) keywords.add(tok);
        }
        return keywords;
    }

    /**
     * Method to sort the corpus based on their tf-idf for the keywords of the inputted document.
     * @param scores the array containing the total score of the document.
     * @param corpus the corpus.
     */
    static void bubbleSort(double[] scores, Webpage[] corpus) {
        int len = scores.length;
        for (int i = 0; i < len - 1; i++) {
            for (int j = 0; j < len - i - 1; j++) {
                if (scores[j] > scores[j + 1]) {
                    // swap arr[j+1] and arr[j]
                    double temp = scores[j];
                    Webpage temp2 = corpus[j];
                    scores[j] = scores[j + 1];
                    corpus[j] = corpus[j + 1];
                    scores[j + 1] = temp;
                    corpus[j + 1] = temp2;
                }
            }
        }
    }

    /**
     * Method to get the most closely related wikipedia page from the corpus.
     * @param pages the corpus.
     * @return the webpage that is most closely related to the current one.
     * @throws IOException disk read throws an IO exception.
     */
    Webpage getBestMatch(Webpage[] pages) throws IOException {
        double[] results = new double[pages.length];
        ArrayList<String> keywords = getKeywords(pages);
        for (int i = 0; i < pages.length; i++) {
            double total = 0;
            for (int j = 0; j < keywords.size(); j++) {
                if (pages[i].text.contains(keywords.get(j))) total += 1;
                double tfidf = pages[i].tfidf(keywords.get(i), pages);
                total += tfidf;
            }
            results[i] = total;
        }
        bubbleSort(results, pages);
        return pages[pages.length - 1];
    }
}
