package bcp.network;

import bcp.protocolo.NetworkMessage;
import bcp.protocolo.NetworkMessage.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConnectionHandler implements Runnable {

    private static final int READ_TIMEOUT_MS = 10000;
    
    private final Socket socket;
    private final PeerManager peerManager;

    public ConnectionHandler(Socket socket, PeerManager peerManager) {
        this.socket = socket;
        this.peerManager = peerManager;
    }

    @Override
    public void run() {
        String remoto = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

        try (socket) {
            socket.setSoTimeout(READ_TIMEOUT_MS);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            Object recebido = in.readObject();
            NetworkMessage resposta;

            if (recebido instanceof NetworkMessage mensagem) {
                System.out.println("[IN  <- " + remoto + "] " + mensagem);
                resposta = processar(mensagem);
            } else {
                resposta = rejeitar("Mensagem fora do protocolo BCP");
            }

            out.writeObject(resposta);
            out.flush();
            System.out.println("[OUT -> " + remoto + "] " + resposta);

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[SERVER] Falha na conexão com " + remoto + ": " + e.getMessage());
        }
    }

    private NetworkMessage processar(NetworkMessage mensagem) {
        return switch (mensagem.getType()) {
            case HELLO -> tratarMensagem(mensagem);
            case GET_PEERS -> new NetworkMessage(MessageType.PEERS_LIST, peerManager.listar());
            default -> rejeitar("Tipo ainda não suportado: " + mensagem.getType());
        };
    }

    private NetworkMessage tratarMensagem(NetworkMessage mensagem) {
        if (!(mensagem.getPayload() instanceof String texto)) {
            return rejeitar("HELLO sem payload de texto");
        }

        String[] partes = texto.split("\\|");
        if (partes.length != 3) {
            return rejeitar("HELLO mal formado: " + texto);
        }

        String versao = partes[0];
        String idNo = partes[1];
        if (!NetworkMessage.VERSAO_PROTOCOLO.equals(versao)) {
            return rejeitar("Versão incompatível: " + versao);
        }

        try {
            int portaEscuta = Integer.parseInt(partes[2]);
            String endereco = socket.getInetAddress().getHostAddress() + ":" + portaEscuta;
            if (peerManager.adicionar(endereco)) {
                System.out.println("[PEERS] Novo peer: " + endereco + " (" + idNo + ")");
            }
        } catch (IllegalArgumentException e) {
            return rejeitar("Endereço inválido no HELLO: " + e.getMessage());
        }

        return new NetworkMessage(MessageType.ACK, "HELLO aceito, " + idNo);
    }

    private NetworkMessage rejeitar(String motivo) {
        return new NetworkMessage(MessageType.REJECT, motivo);
    }
}