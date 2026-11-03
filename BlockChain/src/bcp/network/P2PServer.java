package bcp.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class P2PServer {
	private final int porta;
	private final ExecutorService pool = Executors.newCachedThreadPool();
	private ServerSocket serverSocket;
    private final PeerManager peerManager;

    public P2PServer(int porta, PeerManager peerManager) {
        this.porta = porta;
        this.peerManager = peerManager;
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
				pool.submit(new ConnectionHandler(client));
			} catch (IOException e) {
				System.out.println("[SERVER] Erro ao aceitar conexão: " + e.getMessage());
			}
		}
	}
}
