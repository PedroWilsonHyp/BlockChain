package pacote;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Menu de console da apresentação.
 * Também guarda (como classes/métodos internos) a leitura de argumentos e os logs,
 * para a sua parte ficar em 3 arquivos: Menu, Blockchain e Block.
 */
public class Menu {

    // ===================================================== contrato com o P2PNode

    /** O P2PNode (parte do grupo) implementa isto e é passado ao Menu. */
    public interface NodeActions {
        /** Peers conhecidos, como "IP:Porta". */
        List<String> getPeers();
        /** GET_BLOCKCHAIN aos peers + tryReplace. */
        void syncNow();
        /** BLOCKCHAIN_COPY da cadeia local para todos os peers. */
        void broadcastChain();
    }

    // ===================================================== argumentos

    /** Uso: java Main --port 8333 --id NodeA --seed IP:PORTA [--difficulty 4]  (ou: Main <porta> <id> <seed>) */
    public static class Config {
        public final int port;
        public final String nodeId;
        public final List<String> seeds;
        public final int difficulty;

        private Config(int port, String nodeId, List<String> seeds, int difficulty) {
            this.port = port; this.nodeId = nodeId; this.seeds = seeds; this.difficulty = difficulty;
        }

        public static Config parse(String[] args) {
            int port = 8333, difficulty = 4;
            String id = null;
            List<String> seeds = new ArrayList<>(), pos = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                switch (a) {
                    case "--port": case "-p": port = port(val(args, ++i, a)); break;
                    case "--id": case "-i": id = val(args, ++i, a); break;
                    case "--seed": case "-s":
                        for (String s : val(args, ++i, a).split(",")) seed(seeds, s);
                        break;
                    case "--difficulty": case "-d": difficulty = difficulty(val(args, ++i, a)); break;
                    default:
                        if (a.startsWith("-")) throw new IllegalArgumentException("opção desconhecida: " + a);
                        pos.add(a);
                }
            }
            if (!pos.isEmpty()) {
                port = port(pos.get(0));
                if (pos.size() > 1) id = pos.get(1);
                if (pos.size() > 2) seed(seeds, pos.get(2));
            }
            if (id == null || id.isBlank()) id = "Node-" + port;
            return new Config(port, id, seeds, difficulty);
        }

        @Override public String toString() {
            return "porta=" + port + ", id=" + nodeId + ", seeds=" + seeds + ", dificuldade=" + difficulty;
        }

        private static String val(String[] a, int i, String opt) {
            if (i >= a.length) throw new IllegalArgumentException("falta o valor de " + opt);
            return a[i];
        }
        private static int port(String s) {
            try { int p = Integer.parseInt(s.trim()); if (p < 1 || p > 65535) throw new NumberFormatException(); return p; }
            catch (NumberFormatException e) { throw new IllegalArgumentException("porta inválida: " + s); }
        }
        private static int difficulty(String s) {
            try { int d = Integer.parseInt(s.trim()); if (d < 0 || d > 8) throw new NumberFormatException(); return d; }
            catch (NumberFormatException e) { throw new IllegalArgumentException("dificuldade inválida (0 a 8): " + s); }
        }
        private static void seed(List<String> seeds, String raw) {
            String s = raw.trim();
            if (s.isEmpty()) return;
            int c = s.lastIndexOf(':');
            if (c <= 0 || c == s.length() - 1) throw new IllegalArgumentException("seed deve ser IP:PORTA: " + s);
            port(s.substring(c + 1));
            seeds.add(s);
        }
    }

    // ===================================================== logs

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static volatile String logId = "?";

    public static void setNodeId(String id) { logId = id; }

    /** Ex.: Menu.log("recebi BLOCKCHAIN_COPY de %s, tamanho %d, %s", ip, n, "aceita"); */
    public static synchronized void log(String fmt, Object... args) {
        String msg = (args == null || args.length == 0) ? fmt : String.format(fmt, args);
        System.out.println("[" + LocalTime.now().format(FMT) + "] [" + logId + "] " + msg);
    }

    // ===================================================== menu

    private final Blockchain blockchain;
    private final NodeActions node;
    private final String nodeId;
    private final Scanner in;

    public Menu(Blockchain blockchain, NodeActions node, String nodeId) {
        this(blockchain, node, nodeId, System.in);
    }

    public Menu(Blockchain blockchain, NodeActions node, String nodeId, InputStream input) {
        this.blockchain = blockchain;
        this.node = node;
        this.nodeId = nodeId;
        this.in = new Scanner(input);
        setNodeId(nodeId);
    }

    public void run() {
        while (true) {
            printMenu();
            String opt = readLine();
            if (opt == null) return;
            switch (opt.trim()) {
                case "1": mine(); break;
                case "2": showChain(); break;
                case "3": showPeers(); break;
                case "4": log("sincronização manual: pedindo a cadeia aos peers..."); node.syncNow(); break;
                case "5": corrupt(); break;
                case "0": log("encerrando o nó."); return;
                default: System.out.println("Opção inválida.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("===== BCP - nó " + nodeId + " (cadeia: " + blockchain.size() + " blocos) =====");
        System.out.println(" 1) Minerar bloco");
        System.out.println(" 2) Mostrar cadeia");
        System.out.println(" 3) Mostrar peers");
        System.out.println(" 4) Sincronizar manualmente");
        System.out.println(" 5) Corromper um bloco local");
        System.out.println(" 0) Sair");
        System.out.print("> ");
        System.out.flush();
    }

    private void mine() {
        System.out.print("Dados do bloco: ");
        System.out.flush();
        String data = readLine();
        if (data == null) return;
        if (data.isBlank()) data = "bloco de " + nodeId;

        log("minerando bloco (dificuldade %d)...", blockchain.getDifficulty());
        long t0 = System.currentTimeMillis();
        Block b = blockchain.mineBlock(data);
        long ms = System.currentTimeMillis() - t0;
        if (b == null) {
            log("mineração descartada: a cadeia local mudou durante a mineração.");
            return;
        }
        log("bloco #%d minerado em %d ms (nonce %d, hash %s). Cadeia agora tem %d blocos.",
                b.getIndex(), ms, b.getNonce(), b.getHash(), blockchain.size());
        node.broadcastChain();
    }

    private void showChain() {
        List<Block> chain = blockchain.getChainCopy();
        System.out.println();
        for (Block b : chain) {
            System.out.println("Bloco #" + b.getIndex());
            System.out.println("  dados       : " + b.getData());
            System.out.println("  timestamp   : " + b.getTimestamp());
            System.out.println("  nonce       : " + b.getNonce());
            System.out.println("  previousHash: " + b.getPreviousHash());
            System.out.println("  hash        : " + b.getHash());
        }
        String error = Blockchain.validar(chain, blockchain.getDifficulty());
        System.out.println();
        System.out.println(error == null
                ? "Cadeia VÁLIDA (" + chain.size() + " blocos)."
                : "Cadeia INVÁLIDA: " + error);
    }

    private void showPeers() {
        List<String> peers = node.getPeers();
        if (peers == null || peers.isEmpty()) { System.out.println("Nenhum peer conhecido."); return; }
        System.out.println("Peers conhecidos (" + peers.size() + "):");
        for (String p : peers) System.out.println("  - " + p);
    }

    private void corrupt() {
        int size = blockchain.size();
        if (size < 2) { System.out.println("Só existe o bloco gênese. Minere um bloco primeiro."); return; }
        System.out.print("Índice do bloco a corromper (1 a " + (size - 1) + "): ");
        System.out.flush();
        String s = readLine();
        if (s == null) return;
        int idx;
        try { idx = Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { System.out.println("Índice inválido."); return; }
        if (idx < 1 || idx >= size || !blockchain.corruptBlock(idx, "DADOS ADULTERADOS")) {
            System.out.println("Índice fora do intervalo.");
            return;
        }
        log("bloco #%d corrompido localmente (hash não foi recalculado). isChainValid() = %s",
                idx, blockchain.ChainValida());
    }

    private String readLine() { return in.hasNextLine() ? in.nextLine() : null; }
}
