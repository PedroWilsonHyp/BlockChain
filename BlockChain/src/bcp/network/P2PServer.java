package bcp.network;

import pacote.Blockchain;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class P2PServer {

    private final int porta;
    private final PeerManager peerManager;
    private final Blockchain blockchain;
    private final Runnable aoAceitarCadeia;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;

    public P2PServer(int porta, PeerManager peerManager, Blockchain blockchain, Runnable aoAceitarCadeia) {
        this.porta = porta;
        this.peerManager = peerManager;
        this.blockchain = blockchain;
        this.aoAceitarCadeia = aoAceitarCadeia;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(porta);
        Thread acceptThread = new Thread(this::acceptLoop, "p2p-accept-" + porta);
        acceptThread.start();
        System.out.println("[SERVER] Escutando na porta " + porta);
    }

    private void acceptLoop() {
        while (true) {
            try {
                Socket client = serverSocket.accept();
                pool.execute(new ConnectionHandler(client, peerManager, blockchain, aoAceitarCadeia));
            } catch (IOException e) {
                System.out.println("[SERVER] Erro ao aceitar conexão: " + e.getMessage());
            }
        }
    }
}