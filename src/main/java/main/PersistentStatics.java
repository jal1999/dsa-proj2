package main;

/**
 * A container class for all the static variables having to do
 * with the persistent b-tree.
 */
public class PersistentStatics {
    /**
     * Fixed size of a given word on disk.
     */
    static final int MAX_WORD_LEN = 32;
    /**
     * Maximum amount of entries in a Node's Entry[].
     */
    static final int MAX_ENTRIES = 31;
    /**
     * The size of an int in bytes.
     */
    static final int INT_WIDTH = 4;
    /**
     * The size of a long in bytes.
     */
    static final int LONG_WIDTH = 8;
    /**
     * Maximum amount of children any give node may have (2T).
     */
    static final int MAX_CHILDREN = 32;
    /**
     * Size of a node's id array on disk.
     */
    static final int ID_ARR_SIZE = LONG_WIDTH;
    /**
     * Size of a node's number of entries array on disk.
     */
    static final int NUM_ENTRIES_ARR_SIZE = INT_WIDTH;
    /**
     * Size of a node's leaf array on disk.
     */
    static final int LEAF_ARR_SIZE = INT_WIDTH;
    /**
     * Size of a node's word array on disk.
     */
    static final int WORD_ARR_SIZE = MAX_WORD_LEN * MAX_ENTRIES;
    /**
     * Size of a node's count array on disk.
     */
    static final int COUNT_ARR_SIZE = INT_WIDTH * MAX_ENTRIES;
    /**
     * Size of a node's children array on disk.
     */
    static final int CHILDREN_ARR_SIZE = LONG_WIDTH * MAX_CHILDREN;
    /**
     * Starting index of a node's id array on disk.
     */
    static final int ID_ARR_OFFSET = 0;
    /**
     * Starting index of a node's number of entries array on disk.
     */
    static final int NUM_ENTRIES_ARR_OFFSET = ID_ARR_OFFSET + ID_ARR_SIZE;
    /**
     * Starting index of a node's leaf array on disk.
     */
    static final int LEAF_ARR_OFFSET = NUM_ENTRIES_ARR_OFFSET + NUM_ENTRIES_ARR_SIZE;
    /**
     * Starting index of a node's word array on disk.
     */
    static final int WORD_ARR_OFFSET = LEAF_ARR_OFFSET + LEAF_ARR_SIZE;
    /**
     * Starting index of a node's count array on disk.
     */
    static final int COUNT_ARR_OFFSET = WORD_ARR_OFFSET + WORD_ARR_SIZE;
    /**
     * Starting index of a node's children array on disk.
     */
    static final int CHILDREN_ARR_OFFSET = COUNT_ARR_OFFSET + COUNT_ARR_SIZE;
    /**
     * Total size of a node on disk.
     */
    static final int TOTAL_SIZE = CHILDREN_ARR_OFFSET + CHILDREN_ARR_SIZE;
}
