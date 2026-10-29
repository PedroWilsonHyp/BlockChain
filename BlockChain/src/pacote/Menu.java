package pacote;

import java.util.List;
import java.util.Scanner;

public class Menu {

    /** Contrato com a rede: o P2PNode (do seu amigo) implementa estes 3 métodos. */
    public interface NodeActions {
        List<String> getPeers();
        void syncNow();
        void broadcastChain();
    }

    private final Blockchain blockchain;
    private final NodeActions node;
    private final Scanner in = new Scanner(System.in);

    public Menu(Blockchain blockchain, NodeActions node) {
        this.blockchain = blockchain;
        this.node = node;
    }

    public void run() {
        while (true) {
            System.out.println("\n1) Minerar bloco");
            System.out.println("2) Mostrar cadeia");
            System.out.println("3) Mostrar peers");
            System.out.println("4) Sincronizar manualmente");
            System.out.println("5) Corromper um bloco local");
            System.out.println("0) Sair");
            System.out.print("> ");

            switch (in.nextLine().trim()) {
                case "1": minerar(); break;
                case "2": mostrarCadeia(); break;
                case "3": mostrarPeers(); break;
                case "4": node.syncNow(); break;
                case "5": corromper(); break;
                case "0": return;
                default: System.out.println("Opção inválida.");
            }
        }
    }

    private void minerar() {
        System.out.print("Dados do bloco: ");
        Block b = blockchain.mineBlock(in.nextLine());
        if (b == null) {
            System.out.println("Mineração descartada: a cadeia mudou durante a mineração.");
            return;
        }
        System.out.println("Bloco #" + b.getIndex() + " minerado. Cadeia: " + blockchain.size() + " blocos.");
        node.broadcastChain();
    }

    private void mostrarCadeia() {
        for (Block b : blockchain.getChainCopy()) {
            System.out.println("Bloco #" + b.getIndex() + " | " + b.getData() + " | nonce " + b.getNonce());
            System.out.println("   prev: " + b.getPreviousHash());
            System.out.println("   hash: " + b.getHash());
        }
        System.out.println(blockchain.ChainValida() ? "Cadeia VÁLIDA." : "Cadeia INVÁLIDA.");
    }

    private void mostrarPeers() {
        List<String> peers = node.getPeers();
        if (peers.isEmpty()) {
            System.out.println("Nenhum peer conhecido.");
            return;
        }
        for (String p : peers) System.out.println("  - " + p);
    }

    private void corromper() {
        System.out.print("Índice do bloco a corromper: ");
        int i = Integer.parseInt(in.nextLine().trim());
        if (blockchain.corruptBlock(i, "DADOS ADULTERADOS")) {
            System.out.println("Bloco #" + i + " corrompido.");
        } else {
            System.out.println("Índice inválido.");
        }
    }
}
