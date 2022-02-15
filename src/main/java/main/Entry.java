package main;

/**
 * An object which will consist of both the word and the
 * frequency of the word in the document.
 */
public class Entry {
    /**
     * The word in the document.
     */
    String word;
    /**
     * The amount of times this word has appeared in the document.
     */
    int count;

    Entry(String aWord) {
        word = aWord;
        count = 1;
    }
}
