package main;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Class that will be used to represent the b-tree of order 32
 * for storing the words and frequencies of a specific document.
 */
public class BTree {
    /**
     * Maximum amount of entries that the cache may
     * contain before removing the least recently used.
     */
    static final int MAX_CACHE_ENTRIES = 100;
    /**
     * The root node of the tree.
     */
    Node root;
    /**
     * The file on disk that will contain the data
     * for the tree.
     */
    RandomAccessFile file;
    /**
     * The file channel that will be used to write to
     * the random access file.
     */
    FileChannel channel;
    /**
     * Buffer cache to speed up IO.
     */
    LinkedHashMap<Long, Node> cache;
    /**
     * Size of the tree in terms of amount of nodes.
     */
    long treeSize;

    BTree(RandomAccessFile aFile) throws IOException {
        file = aFile;
        channel = file.getChannel();
        treeSize = 0;
        root = new Node(treeSize++);
        root.leaf = true;
        cache = new LinkedHashMap<>(133) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Node> eldest) {
                return cache.size() >= MAX_CACHE_ENTRIES;
            }
        };
        diskWrite(root);
    }

    /**
     * Method to write the fields of the node to disk.
     * Will be in the order of id, numEntries, leaf, words, counts, children
     * @param n the node whose fields are being written to disk.
     * @throws IOException if there is an issue with seeking to any of the
     * fields indices in the file.
     */
    void diskWrite(Node n) throws IOException {
        System.out.println("DW!");
        channel.position((n.id * PersistentStatics.TOTAL_SIZE) + PersistentStatics.ID_ARR_OFFSET);
        ByteBuffer idBuf = ByteBuffer.allocate(PersistentStatics.ID_ARR_SIZE);
        idBuf.putLong(n.id);
        idBuf.flip();
        channel.write(idBuf); // id
        channel.position((n.id * PersistentStatics.TOTAL_SIZE) + PersistentStatics.NUM_ENTRIES_ARR_OFFSET);
        ByteBuffer numEntriesBuf = ByteBuffer.allocate(PersistentStatics.NUM_ENTRIES_ARR_SIZE);
        numEntriesBuf.putInt(n.numEntries);
        numEntriesBuf.flip();
        channel.write(numEntriesBuf); // num entries
        channel.position((n.id * PersistentStatics.TOTAL_SIZE) + PersistentStatics.LEAF_ARR_OFFSET);
        ByteBuffer leafBuf = ByteBuffer.allocate(PersistentStatics.LEAF_ARR_SIZE);
        if (n.leaf) leafBuf.putInt(1);
        else leafBuf.putInt(0);
        leafBuf.flip();
        channel.write(leafBuf); // leaf
        for (int i = 0; i < n.entries.length; i++) {
            ByteBuffer words = ByteBuffer.allocate(PersistentStatics.MAX_WORD_LEN);
            channel.position((n.id * PersistentStatics.TOTAL_SIZE) + (PersistentStatics.WORD_ARR_OFFSET + (PersistentStatics.MAX_WORD_LEN * i)));
            String word;
            if (n.entries[i] == null) word = "null";
            else word = n.entries[i].word;
            int wordLen = word.length();
            while (wordLen < 32) {
                word += " "; // padding the word to 32 bytes
                wordLen = word.length();
            }
            byte[] bArr = new byte[32];
            for (int j = 0; j < 32; j++) {
                byte b = (byte)word.charAt(j);
                bArr[j] = b;
            }
            words.put(bArr);
            words.flip();
            channel.write(words); // words
        }
        for (int j = 0; j < n.entries.length; j++) {
            if (n.entries[j] == null) continue;
            ByteBuffer count = ByteBuffer.allocate(PersistentStatics.INT_WIDTH);
            channel.position((n.id * PersistentStatics.TOTAL_SIZE) + (PersistentStatics.COUNT_ARR_OFFSET + (PersistentStatics.INT_WIDTH * j)));
            count.putInt(n.entries[j].count);
            count.flip();
            channel.write(count); // count
        }
        for (int k = 0; k < PersistentStatics.MAX_CHILDREN; k++) {
            ByteBuffer child = ByteBuffer.allocate(PersistentStatics.LONG_WIDTH);
            channel.position((n.id * PersistentStatics.TOTAL_SIZE) + (PersistentStatics.CHILDREN_ARR_OFFSET + (PersistentStatics.LONG_WIDTH * k)));
            if (n.children[k] == null) child.putLong(-2);
            else child.putLong(n.children[k].id);
            child.flip();
            channel.write(child); // children
        }
    }

    /**
     * Method to read the fields of a node from disk, and return a copy of that
     * node for processing.
     * @param id the id of the node to be read from disk.
     * @return a copy of the node being read from disk for processing.
     */
    Node diskRead(long id) throws IOException {
        channel.position((id * PersistentStatics.TOTAL_SIZE) + PersistentStatics.ID_ARR_OFFSET);
        ByteBuffer idBuf = ByteBuffer.allocate(PersistentStatics.ID_ARR_SIZE);
        channel.read(idBuf);
        idBuf.flip();
        long aId = idBuf.getLong(); // id
        channel.position((id * PersistentStatics.TOTAL_SIZE) + PersistentStatics.NUM_ENTRIES_ARR_OFFSET);
        ByteBuffer numEntriesBuf = ByteBuffer.allocate(PersistentStatics.NUM_ENTRIES_ARR_SIZE);
        channel.read(numEntriesBuf);
        numEntriesBuf.flip();
        int aNumEntries = numEntriesBuf.getInt(); // # entries
        channel.position((id * PersistentStatics.TOTAL_SIZE) + PersistentStatics.LEAF_ARR_OFFSET);
        ByteBuffer leafBuf = ByteBuffer.allocate(PersistentStatics.LEAF_ARR_SIZE);
        channel.read(leafBuf);
        leafBuf.flip();
        int tmpLeaf = leafBuf.getInt();
        boolean aLeaf = tmpLeaf == 1; // leaf
        String[] words = new String[PersistentStatics.MAX_ENTRIES];
        for (int i = 0; i < words.length; i++) { // words
            channel.position((id * PersistentStatics.TOTAL_SIZE) + (PersistentStatics.WORD_ARR_OFFSET + (PersistentStatics.MAX_WORD_LEN * i)));
            ByteBuffer word = ByteBuffer.allocate(PersistentStatics.MAX_WORD_LEN);
            channel.read(word);
            word.flip();
            byte[] wordBytes = new byte[PersistentStatics.MAX_WORD_LEN];
            int j = 0;
            while (word.hasRemaining()) {
                byte b = word.get();
                wordBytes[j] = b;
                j++;
            }
            String s = new String(wordBytes, StandardCharsets.ISO_8859_1);
            StringTokenizer tz = new StringTokenizer(s);
            String actualWord = tz.nextToken();
            if (actualWord.toString().equalsIgnoreCase("null")) words[i] = null;
            else words[i] = actualWord;
        }
        int[] counts = new int[PersistentStatics.MAX_ENTRIES];
        for (int k = 0; k < counts.length; k++) { // counts
            channel.position((id * PersistentStatics.TOTAL_SIZE) + (PersistentStatics.COUNT_ARR_OFFSET + (PersistentStatics.INT_WIDTH * k)));
            ByteBuffer count = ByteBuffer.allocate(PersistentStatics.INT_WIDTH);
            channel.read(count);
            count.flip();
            int c = count.getInt();
            counts[k] = c;
        }
        Entry[] aEntries = new Entry[PersistentStatics.MAX_ENTRIES];
        for (int l = 0; l < aEntries.length; l++) { // creating the entries from the words and counts
            if (words[l] == null) continue;
            aEntries[l] = new Entry(words[l]);
            aEntries[l].count = counts[l];
        }
        Node[] aChildren = new Node[PersistentStatics.MAX_CHILDREN];
        for (int m = 0; m < aChildren.length; m++) {
            channel.position((id * PersistentStatics.TOTAL_SIZE) + (PersistentStatics.CHILDREN_ARR_OFFSET + (PersistentStatics.LONG_WIDTH * m)));
            ByteBuffer child = ByteBuffer.allocate(PersistentStatics.LONG_WIDTH);
            channel.read(child);
            child.flip();
            long l = child.getLong();
            if (l == -2) aChildren[m] = null;
            else {
                aChildren[m] = new Node();
                aChildren[m].id = l;
            }
        }
        Node fakeNode = new Node();
        fakeNode.entries = aEntries;
        fakeNode.children = aChildren;
        fakeNode.id = aId;
        fakeNode.numEntries = aNumEntries;
        fakeNode.leaf = aLeaf;
        return fakeNode;
    }

    /**
     * Method to find a word in the tree.
     * @param currNode the node we are currently searching.
     * @param word the word we are searching for in the tree.
     * @return the Entry that contains the word if it exists,
     * or return null if the entry is not present in the three.
     */
    Entry search(Node currNode, String word) throws IOException {
        int i = 0;
        while ((i <= currNode.numEntries) && (word.compareTo(currNode.entries[i].word)) > 0) { // moving through the array to find where the word would be placed
            i++;
        }
        if (i < currNode.numEntries && word.equalsIgnoreCase(currNode.entries[i].word)) {
            cache.put(currNode.id, currNode);
            return currNode.entries[i]; // if the word is in the spot return true
        } else if (currNode.leaf) {
            cache.put(currNode.id, currNode);
            return null; // if it's not there and curr node is a leaf node, return false
        } else {
            cache.put(currNode.id, currNode);
            Node n;
            if (cache.containsKey(currNode.children[i].id)) {
                cache.get(currNode.children[i].id);
                cache.put(currNode.children[i].id, currNode.children[i]);
            } else {
                n = diskRead(currNode.children[i].id);
                cache.put(n.id, n);
            }
            return search(currNode.children[i], word); // if it is not a leaf node, recurse
        }
    }

    /**
     * Method to split a full node.
     * @param parent the parent of the node being split.
     * @param nodeBeingSplit the node being split.
     * @param indexInParent index of the node being split in the parent's
     * children array.
     * @throws IOException if disk write throws an IO exception.
     */
    void splitChild(Node parent, Node nodeBeingSplit, int indexInParent) throws IOException {
        System.out.println("PERFORMING SPLIT!");
        Node newRightSibling = new Node(treeSize++);
        newRightSibling.leaf = nodeBeingSplit.leaf;
        for (int i = 0; i < Node.T - 1; i++) { // putting keys larger than median in new node
            newRightSibling.entries[i] = nodeBeingSplit.entries[i + Node.T];
            nodeBeingSplit.entries[i + Node.T] = null;
        }
        if (!nodeBeingSplit.leaf) {
            for (int j = 0; j < Node.T; j++) { // splitting the children up accordingly
                newRightSibling.children[j] = nodeBeingSplit.children[j + Node.T];
                nodeBeingSplit.children[j + Node.T - 1] = null;
            }
        }
        for (int k = parent.children.length - 2; k >= indexInParent + 1; k--) parent.children[k + 1] = parent.children[k]; // making room for new right sibling
        parent.children[indexInParent + 1] = newRightSibling;
        for (int l = parent.entries.length - 2; l >= indexInParent; l--) parent.entries[l + 1] = parent.entries[l]; // making room for median key
        parent.entries[indexInParent] = nodeBeingSplit.entries[Node.T - 1]; // putting median index in parent
        nodeBeingSplit.entries[Node.T - 1] = null; // removing median index from node being split
        parent.numEntries++; nodeBeingSplit.numEntries = Node.T - 1; newRightSibling.numEntries = Node.T - 1; // setting the # of keys in the nodes
        diskWrite(parent); diskWrite(nodeBeingSplit); diskWrite(newRightSibling); // writing all the nodes to disk
        cache.put(parent.id, parent); cache.put(nodeBeingSplit.id, nodeBeingSplit); cache.put(newRightSibling.id, newRightSibling);
    }

    /**
     * Method to insert on a leaf node, assumed to be non-full.
     * @param n the node we are inserting into.
     * @param e the entry we are inserting into the node.
     * @throws IOException if disk write throws an IO exception.
     */
    void linearInsert(Node n, Entry e) throws IOException {
        int i = n.numEntries - 1;
        while (i >= 0 && e.word.compareTo(n.entries[i].word) < 0) {
            n.entries[i + 1] = n.entries[i]; // moving over the entries as we go
            --i;
        }
        n.entries[++i] = e;
        ++n.numEntries;
        diskWrite(n);
    }

    /**
     * Method to search for the word in the current node, and if
     * it is found, increment its count and write the node to disk.
     * @param n the node being traversed.
     * @param word the word being searched for.
     * @return true if the word is found, false otherwise.
     * @throws IOException if disk write throws an IO exception.
     */
    boolean incrementIfPresent(Node n, String word) throws IOException {
        for (Entry e : n.entries) {
            if (e == null) return false;
            if (e.word.equalsIgnoreCase(word)) {
                ++e.count;
                diskWrite(n);
                return true;
            }
        }
        return false;
    }

    /**
     * Method to insert a word into the tree with a nonfull root.
     * @param currNode the node we are currently trying to insert into.
     * @param word the word being inserted into the tree.
     * @throws IOException if there are any issues with the random access file.
     */
    void insertNonfull(Node currNode, String word) throws IOException {
        if (incrementIfPresent(currNode, word)) return; // if word is present: increment count, disk write, and return.
        if (currNode.leaf) {
            Entry e = new Entry(word);
            linearInsert(currNode, e);
        } else { // if currNode is not a leaf node
            int i = currNode.numEntries - 1;
            System.out.println("# of entries - 1: " + i);
            System.out.println("node's entries: " );
            for (Entry e : currNode.entries) {
                if (e == null) System.out.println("null");
                else System.out.println(e.word);
            }
            while (i >= 0 && word.compareTo(currNode.entries[i].word) < 0) --i;
            Node n;
            if (currNode.children[i + 1] != null) {
                if (cache.containsValue(currNode.children[i + 1])) {
                    n = cache.get(currNode.children[++i].id);
                    cache.put(n.id, n);
                } else {
                    n = diskRead(currNode.children[++i].id);
                    cache.put(n.id, n);
                }
            } else {
                if (cache.containsValue(currNode.children[i])) {
                    n = cache.get(currNode.children[i].id);
                    cache.put(n.id, n);
                } else {
                    n = diskRead(currNode.children[i].id);
                    cache.put(n.id, n);
                }
            }
            if (n.numEntries == PersistentStatics.MAX_ENTRIES)
                splitChild(currNode, n, i);
            insertNonfull(n, word);
        }
    }

    /**
     * Insert method for the tree.
     * @param word the word being inserted into the tree.
     * @throws IOException if there are any issues with the random access file.
     */
    void insert(String word) throws IOException {
        Node r;
        if (cache.containsKey(0l)) {
            r = cache.get(0l);
        } else {
            r = diskRead(0);
        }
        if (r.numEntries == Node.MAX_ENTRIES) {
            Node s = new Node(0);
            root = s;
            s.leaf = false;
            s.numEntries = 0;
            r.id = treeSize++;
            s.children[0] = r;
            splitChild(s, r, 0);
            insertNonfull(s, word);
        } else {
            insertNonfull(r, word);
        }
    }
}
