package pacote;

import bcp.network.P2PNode;

import java.io.IOException;
import java.util.List;

public class Main {

    private static final int DIFICULDADE = 4;

    public static void main(String[] args) {
        if (args.length > 4) {
            mostrarUso("Argumentos demais.");
            return;
        }

        int porta;
        try {
            porta = args.length > 0 ? Integer.parseInt(args[0]) : 8333;
        } catch (NumberFormatException e) {
            mostrarUso("Porta inválida: " + args[0]);
            return;
        }
        if (porta < 1 || porta > 65535) {
            mostrarUso("Porta fora do intervalo 1 a 65535: " + porta);
            return;
        }

        String idNo = args.length > 1 ? args[1] : "No-" + porta;
        String ip = args.length > 2 ? args[2] : "127.0.0.1";
        String seed = args.length > 3 ? args[3] : P2PNode.SEED_PADRAO;
        String enderecoProprio = ip + ":" + porta;

        Blockchain blockchain = new Blockchain(DIFICULDADE);

        final P2PNode node;
        try {
            node = new P2PNode(idNo, ip, porta, blockchain);
        } catch (IllegalArgumentException e) {
            mostrarUso(e.getMessage());
            return;
        }

        try {
            node.iniciar();
        } catch (IOException e) {
            System.out.println("[MAIN] Não foi possível escutar na porta " + porta + ": " + e.getMessage());
            System.out.println("[MAIN] Verifique se outro nó já está usando essa porta.");
            return;
        }

        System.out.println("[MAIN] Nó " + idNo + " em " + enderecoProprio + " (dificuldade " + DIFICULDADE + ")");
        if (seed.equals(enderecoProprio)) {
            System.out.println("[MAIN] Este nó é o seed. Aguardando os outros nós.");
        } else {
            node.bootstrap(seed);
        }

        Menu.NodeActions acoes = new Menu.NodeActions() {
            @Override
            public List<String> getPeers() {
                return node.getPeers();
            }

            @Override
            public void syncNow() {
                node.syncNow();
            }

            @Override
            public void broadcastChain() {
                node.broadcastChain();
            }
        };

        try {
            new Menu(blockchain, acoes).run();
        } catch (RuntimeException e) {
            System.out.println("[MAIN] Erro no menu: " + e);
        } finally {
            System.out.println("[MAIN] Encerrando o nó.");
            System.exit(0);
        }
    }

    private static void mostrarUso(String erro) {
        System.out.println("Erro: " + erro);
        System.out.println("Uso: java pacote.Main [porta] [idNo] [ipAnunciado] [seed]");
        System.out.println("  porta        porta onde este nó escuta (padrão 8333)");
        System.out.println("  idNo         nome do nó nos logs, sem '|' (padrão No-<porta>)");
        System.out.println("  ipAnunciado  IP pelo qual os outros nós alcançam este (padrão 127.0.0.1)");
        System.out.println("  seed         IP:porta do nó semente (padrão " + P2PNode.SEED_PADRAO + ")");
    }
}