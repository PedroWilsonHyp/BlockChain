package pacote;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Bloco da blockchain. Serializável para trafegar dentro de NetworkMessage
 * (ObjectOutputStream / ObjectInputStream).
 */
public class Block implements Serializable {

    private static final long serialVersionUID = 1L;

    private int index;
    private long timestamp;
    private String data;            // não é final: permite corromper o bloco na demo
    private String previousHash;
    private String hash;
    private int nonce;

    public Block(int index, long timestamp, String data, String previousHash) {
        setIndex(index);
        setTimeStamp(timestamp);
        setData(data);
        setPreviousHash(previousHash);
        this.nonce = 0;
        this.hash = calcularHash();
    }

    /** SHA-256 de index + previousHash + timestamp + nonce + data. */
    public String calcularHash() {
        return sha256(
                index + previousHash + timestamp + nonce + data);
    }

    /** Proof of Work: incrementa o nonce até o hash começar com 'difficulty' zeros. */
    public void minerar(int difficulty) {
        String target = "0".repeat(difficulty);
        while (!hash.startsWith(target)) {
            nonce++;
            hash = calcularHash();
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

    public void setIndex(int index) {this.index = index;}
    public void setTimeStamp(long timestamp) {this.timestamp = timestamp;}
    public void setData(String data) {this.data = data;}
    public void setPreviousHash(String previousHash) {this.previousHash = previousHash;}


    
    @Override
    public String toString() {
        return "Block{#" + index + ", hash=" + shortHash(hash)
                + ", prev=" + shortHash(previousHash)
                + ", nonce=" + nonce + ", data='" + data + "'}";
    }

    static String shortHash(String h) {
        return h == null ? "null" : (h.length() <= 10 ? h : h.substring(0, 10) + "…");
    }

    /** SHA-256 em hexadecimal minúsculo. */
    private static String sha256(String input) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(h.length * 2);
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
