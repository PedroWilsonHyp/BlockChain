package pacote;

import bcp.network.P2PNode;

/**
 * Uso: java pacote.Main [porta] [idDoNo] [seed IP:PORTA] [meuIP]
 * Ex.: java pacote.Main 8333 NodeA
 *      java pacote.Main 8334 NodeB 127.0.0.1:8333
 */
public class Main {

    private static final int DIFICULDADE = 4; // igual em todos os nós

    public static void main(String[] args) throws Exception {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8333;
        String id = args.length > 1 ? args[1] : "No-" + porta;
        String seed = args.length > 2 ? args[2] : null;
        String ip = args.length > 3 ? args[3] : "127.0.0.1";

        Blockchain blockchain = new Blockchain(DIFICULDADE);
        P2PNode node = new P2PNode(id, ip, porta, blockchain);

        node.iniciar();
        if (seed != null) {
            node.bootstrap(seed);
        }

        new Menu(blockchain, new ConectorMenu(node)).run();
        System.exit(0); // o servidor P2P mantém a JVM viva
    }
}