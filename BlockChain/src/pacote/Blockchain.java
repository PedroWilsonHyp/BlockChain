package pacote;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda e valida a cadeia. Thread-safe: o P2PServer/ConnectionHandler (threads de rede)
 * e o Menu (thread principal) acessam a mesma instância.
 */
public class Blockchain {

    /** Genesis fixo: idêntico em todos os nós (timestamp, dados e nonce constantes). */
    private static final long GENESIS_TIMESTAMP = 1_700_000_000_000L;
    private static final Block GENESIS = new Block(0, GENESIS_TIMESTAMP, "Genesis Block", "0");

    /** Resultado de tryReplace(); 'reason' vai no payload do REJECT quando rejeitada. */
    public static final class ReplaceResult {
        public final boolean accepted;
        public final String reason;

        private ReplaceResult(boolean accepted, String reason) {
            this.accepted = accepted;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return (accepted ? "aceita" : "rejeitada") + " (" + reason + ")";
        }
    }

    private final ArrayList<Block> chain = new ArrayList<>();
    private final int difficulty;

    public Blockchain(int difficulty) {
        if (difficulty < 0) throw new IllegalArgumentException("dificuldade < 0");
        this.difficulty = difficulty;
        chain.add(genesis());
    }

    /** Cópia do bloco gênese (cada cadeia tem sua própria instância, para poder ser corrompida). */
    public static Block genesis() {
        return new Block(GENESIS.getIndex(), GENESIS.getTimestamp(),
                GENESIS.getData(), GENESIS.getPreviousHash());
    }

    public int getDifficulty() { return difficulty; }

    public synchronized int size() { return chain.size(); }

    public synchronized Block getLast() { return chain.get(chain.size() - 1); }

    /** Cópia da cadeia, pronta para ir como payload de BLOCKCHAIN_COPY (ArrayList<Block>). */
    public synchronized ArrayList<Block> getChainCopy() {
        return new ArrayList<>(chain);
    }

    // ---------------------------------------------------------------- mineração

    /**
     * Minera um novo bloco em cima da ponta atual da cadeia e o adiciona.
     * A mineração roda fora do lock; se a cadeia mudar no meio (ex.: chegou uma cadeia maior
     * da rede), o bloco é descartado e o método devolve null.
     */
    public Block mineBlock(String data) {
        Block prev;
        synchronized (this) {
            prev = getLast();
        }
        Block block = new Block(prev.getIndex() + 1, System.currentTimeMillis(), data, prev.getHash());
        block.mine(difficulty);
        synchronized (this) {
            if (!getLast().getHash().equals(prev.getHash())) {
                return null; // ponta mudou durante a mineração
            }
            chain.add(block);
        }
        return block;
    }

    // ---------------------------------------------------------------- validação

    /** Valida a cadeia local. */
    public synchronized boolean isChainValid() {
        return isChainValid(chain);
    }

    /** Valida uma cadeia qualquer (ex.: recebida da rede) com a dificuldade local. */
    public boolean isChainValid(List<Block> candidate) {
        return validate(candidate, difficulty) == null;
    }

    /**
     * Verifica integridade e criptografia. Devolve null se válida, ou a descrição do primeiro erro.
     * Regras: começa no gênese fixo; para cada bloco i>=1: índice sequencial, hash recalculado igual
     * ao registrado, previousHash igual ao hash do bloco anterior, e PoW (hash com 'difficulty' zeros).
     */
    public static String validate(List<Block> candidate, int difficulty) {
        if (candidate == null || candidate.isEmpty()) return "cadeia vazia";

        Block first = candidate.get(0);
        if (first == null) return "bloco nulo na posição 0";
        if (!first.getHash().equals(GENESIS.getHash())
                || !first.getHash().equals(first.calculateHash())
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
            if (!cur.getHash().equals(cur.calculateHash())) {
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

    // ---------------------------------------------------------------- consenso

    /**
     * Regra da maior cadeia válida: substitui a cadeia local somente se a recebida
     * for válida E estritamente maior (recebida.size() > local.size()).
     */
    public synchronized ReplaceResult tryReplace(List<Block> received) {
        if (received == null) {
            return new ReplaceResult(false, "Cadeia nula");
        }
        String error = validate(received, difficulty);
        if (error != null) {
            return new ReplaceResult(false, "Cadeia inválida: " + error);
        }
        if (received.size() <= chain.size()) {
            return new ReplaceResult(false, "Tamanho menor que a cadeia local");
        }
        int old = chain.size();
        chain.clear();
        chain.addAll(received);
        return new ReplaceResult(true, "cadeia substituída: tamanho " + old + " -> " + chain.size());
    }

    // ---------------------------------------------------------------- demo

    /** Adultera os dados de um bloco local sem refazer o hash (para demonstrar o REJECT ao vivo). */
    public synchronized boolean corruptBlock(int index, String newData) {
        if (index < 0 || index >= chain.size()) return false;
        chain.get(index).tamperData(newData);
        return true;
    }
}
