package pacote;
import java.util.ArrayList;
import java.util.List;


public class Blockchain {

   private static final long GENESIS_TIMESTAMP = 1_700_000_000_000L;
    private static final Block GENESIS = new Block(0, GENESIS_TIMESTAMP, "Genesis Block", "0");

    public static final class ResultadoTroca {
        public final boolean aceita;
        public final String motivo;

        private ResultadoTroca(boolean aceita, String motivo) {
            this.aceita = aceita;
            this.motivo = motivo;
        }

        @Override
        public String toString() {
            return (aceita ? "aceita" : "rejeitada") + " (" + motivo + ")";
        }
    }

    private final ArrayList<Block> chain = new ArrayList<>();
    private final int difficulty;

    public Blockchain(int difficulty) {
        if (difficulty < 0) throw new IllegalArgumentException("dificuldade < 0");
        this.difficulty = difficulty;
        chain.add(genesis());
    }

    public static Block genesis() {
        return new Block(GENESIS.getIndex(), GENESIS.getTimestamp(),
                GENESIS.getData(), GENESIS.getPreviousHash());
    }

    public int getDifficulty() { return difficulty; }

    public synchronized int size() { return chain.size(); }

    public synchronized Block getLast() { return chain.get(chain.size() - 1); }

    public synchronized ArrayList<Block> getChainCopy() {
        return new ArrayList<>(chain);
    }

    public Block mineBlock(String data) {
        Block prev;
        synchronized (this) {
            prev = getLast();
        }
        Block block = new Block(prev.getIndex() + 1, System.currentTimeMillis(), data, prev.getHash());
        block.minerar(difficulty);
        synchronized (this) {
            if (!getLast().getHash().equals(prev.getHash())) {
                return null; 
                }
            chain.add(block);
        }
        return block;
    }

   public synchronized boolean ChainValida() {
        return ChainValida(chain);
    }

   public boolean ChainValida(List<Block> candidate) {
        return validar(candidate, difficulty) == null;
    }


    public static String validar(List<Block> candidate, int difficulty) {
        if (candidate == null || candidate.isEmpty()) return "cadeia vazia";

        Block first = candidate.get(0);
        if (first == null) return "bloco nulo na posição 0";
        if (!first.getHash().equals(GENESIS.getHash())
                || !first.getHash().equals(first.calcularHash())
                || first.getIndex() != 0) {
            return "bloco gênese diferente do gênese esperado";
        }

        String target = "0".repeat(difficulty);
        for (int i = 1; i < candidate.size(); i++) {
            Block cur = candidate.get(i);
            Block prev = candidate.get(i - 1);
            if (cur == null) return "bloco nulo na posição " + i;
            if (cur.getIndex() != i) {
                return "índice incorreto no bloco " + i + " (veio " + cur.getIndex() + ")";
            }
            if (!cur.getHash().equals(cur.calcularHash())) {
                return "hash do bloco " + i + " não confere com seu conteúdo (dados adulterados)";
            }
            if (!cur.getPreviousHash().equals(prev.getHash())) {
                return "encadeamento quebrado entre os blocos " + (i - 1) + " e " + i;
            }
            if (!cur.getHash().startsWith(target)) {
                return "bloco " + i + " não foi minerado (PoW com dificuldade " + difficulty + ")";
            }
        }
        return null;
    }

    public synchronized ResultadoTroca tryReplace(List<Block> received) {
        if (received == null) {
            return new ResultadoTroca(false, "Cadeia nula");
        }
        String error = validar(received, difficulty);
        if (error != null) {
            return new ResultadoTroca(false, "Cadeia inválida: " + error);
        }
        if (received.size() <= chain.size()) {
            return new ResultadoTroca(false, "Tamanho menor que a cadeia local");
        }
        int old = chain.size();
        chain.clear();
        chain.addAll(received);
        return new ResultadoTroca(true, "cadeia substituída: tamanho " + old + " -> " + chain.size());
    }

    public synchronized boolean corruptBlock(int index, String newData) {
        if (index < 0 || index >= chain.size()) return false;
        chain.get(index).tamperData(newData);
        return true;
    }
}
