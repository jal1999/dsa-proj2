package main;

/**
 * Object that will act as the blocks of the b-tree.
 */
public class Node {
    /**
     * T in the function for both max keys (2T - 1) and max children (2T)
     * for any given node in the tree.
     */
    static final int T = 16;
    /**
     * Maximum amount of children any given node may have.
     */
    static final int MAX_CHILDREN = 2 * T;
    /**
     * Maximum entries (keys) any given node may have.
     */
    static final int MAX_ENTRIES = (2 * T) - 1;
    /**
     * Array for the entries (keys) that a node contains.
     */
    Entry[] entries;
    /**
     * Array for the children that a node points to.
     */
    Node[] children;
    /**
     * Whether or not a node is a leaf node.
     */
    boolean leaf;
    /**
     * The ID for the node.
     */
    long id;
    /**
     * Number of entries (keys) that a node contains.
     */
    int numEntries;

    Node(long aId) {
        entries = new Entry[MAX_ENTRIES];
        children = new Node[MAX_CHILDREN];
        leaf = true;
        numEntries = 0;
        id = aId;
    }

    Node() { // only being used for disk read.
    }
}
