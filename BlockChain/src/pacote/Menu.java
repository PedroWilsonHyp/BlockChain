package pacote;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

/** Menu de console usado na apresentação. */
public class Menu {

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
    }

    /** Laço do menu. Retorna quando o usuário escolhe sair (ou a entrada acaba). */
    public void run() {
        while (true) {
            printMenu();
            String opt = readLine();
            if (opt == null) return;
            switch (opt.trim()) {
                case "1": mine(); break;
                case "2": showChain(); break;
                case "3": showPeers(); break;
                case "4": sync(); break;
                case "5": corrupt(); break;
                case "0": Log.info("encerrando o nó."); return;
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

    // ------------------------------------------------------------ ações

    private void mine() {
        System.out.print("Dados do bloco: ");
        System.out.flush();
        String data = readLine();
        if (data == null) return;
        if (data.isBlank()) data = "bloco de " + nodeId;

        Log.info("minerando bloco (dificuldade %d)...", blockchain.getDifficulty());
        long t0 = System.currentTimeMillis();
        Block b = blockchain.mineBlock(data);
        long ms = System.currentTimeMillis() - t0;

        if (b == null) {
            Log.warn("mineração descartada: a cadeia local mudou durante a mineração.");
            return;
        }
        Log.info("bloco #%d minerado em %d ms (nonce %d, hash %s). Cadeia agora tem %d blocos.",
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
        String error = Blockchain.validate(chain, blockchain.getDifficulty());
        System.out.println();
        System.out.println(error == null
                ? "Cadeia VÁLIDA (" + chain.size() + " blocos)."
                : "Cadeia INVÁLIDA: " + error);
    }

    private void showPeers() {
        List<String> peers = node.getPeers();
        if (peers == null || peers.isEmpty()) {
            System.out.println("Nenhum peer conhecido.");
            return;
        }
        System.out.println("Peers conhecidos (" + peers.size() + "):");
        for (String p : peers) System.out.println("  - " + p);
    }

    private void sync() {
        Log.info("sincronização manual: pedindo a cadeia aos peers...");
        node.syncNow();
    }

    private void corrupt() {
        int size = blockchain.size();
        if (size < 2) {
            System.out.println("Só existe o bloco gênese. Minere um bloco primeiro.");
            return;
        }
        System.out.print("Índice do bloco a corromper (1 a " + (size - 1) + "): ");
        System.out.flush();
        String s = readLine();
        if (s == null) return;
        int idx;
        try {
            idx = Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            System.out.println("Índice inválido.");
            return;
        }
        if (idx < 1 || idx >= size || !blockchain.corruptBlock(idx, "DADOS ADULTERADOS")) {
            System.out.println("Índice fora do intervalo.");
            return;
        }
        Log.warn("bloco #%d corrompido localmente (hash não foi recalculado). isChainValid() = %s",
                idx, blockchain.isChainValid());
        System.out.println("Agora use 'Mostrar cadeia' ou 'Minerar' + broadcast para ver o REJECT nos outros nós.");
    }

    private String readLine() {
        return in.hasNextLine() ? in.nextLine() : null;
    }
}
