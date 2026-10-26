package pacote;

import java.io.Serializable;

/**
 * Bloco da blockchain. Serializável para trafegar dentro de NetworkMessage
 * (ObjectOutputStream / ObjectInputStream).
 */
public class Block implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int index;
    private final long timestamp;
    private String data;            // não é final: permite corromper o bloco na demo
    private final String previousHash;
    private String hash;
    private int nonce;

    public Block(int index, long timestamp, String data, String previousHash) {
        this.index = index;
        this.timestamp = timestamp;
        this.data = data;
        this.previousHash = previousHash;
        this.nonce = 0;
        this.hash = calculateHash();
    }

    /** SHA-256 de index + previousHash + timestamp + nonce + data. */
    public String calculateHash() {
        return StringUtil.applySha256(
                index + previousHash + timestamp + nonce + data);
    }

    /** Proof of Work: incrementa o nonce até o hash começar com 'difficulty' zeros. */
    public void mine(int difficulty) {
        String target = "0".repeat(difficulty);
        while (!hash.startsWith(target)) {
            nonce++;
            hash = calculateHash();
        }
    }

    /** Altera os dados SEM recalcular o hash (simula adulteração; usado na demo de REJECT). */
    public void tamperData(String newData) {
        this.data = newData;
    }

    public int getIndex() { return index; }
    public long getTimestamp() { return timestamp; }
    public String getData() { return data; }
    public String getPreviousHash() { return previousHash; }
    public String getHash() { return hash; }
    public int getNonce() { return nonce; }

    @Override
    public String toString() {
        return "Block{#" + index + ", hash=" + shortHash(hash)
                + ", prev=" + shortHash(previousHash)
                + ", nonce=" + nonce + ", data='" + data + "'}";
    }

    static String shortHash(String h) {
        return h == null ? "null" : (h.length() <= 10 ? h : h.substring(0, 10) + "…");
    }
}
