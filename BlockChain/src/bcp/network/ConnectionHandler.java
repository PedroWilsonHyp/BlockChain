package bcp.network;

import bcp.protocolo.NetworkMessage;
import bcp.protocolo.NetworkMessage.MessageType;
import pacote.Block;
import pacote.Blockchain;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectionHandler implements Runnable {

    private static final int READ_TIMEOUT_MS = 10000;

    private final Socket socket;
    private final PeerManager peerManager;
    private final Blockchain blockchain;
    private final Runnable aoAceitarCadeia;

    public ConnectionHandler(Socket socket, PeerManager peerManager,
                             Blockchain blockchain, Runnable aoAceitarCadeia) {
        this.socket = socket;
        this.peerManager = peerManager;
        this.blockchain = blockchain;
        this.aoAceitarCadeia = aoAceitarCadeia;
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
                try {
                    resposta = processar(mensagem);
                } catch (RuntimeException e) {
                    resposta = rejeitar("Erro ao processar " + mensagem.getType() + ": " + e);
                }
            } else {
                resposta = rejeitar("Mensagem fora do protocolo BCP");
            }

            out.writeObject(resposta);
            out.flush();
            System.out.println("[OUT -> " + remoto + "] " + resposta);

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[SERVER] Falha na conexão com " + remoto + ": " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("[SERVER] Erro inesperado com " + remoto + ": " + e);
        }
    }

    private NetworkMessage processar(NetworkMessage mensagem) {
        return switch (mensagem.getType()) {
            case HELLO -> tratarHello(mensagem);
            case GET_PEERS -> new NetworkMessage(MessageType.PEERS_LIST, peerManager.listar());
            case GET_BLOCKCHAIN -> new NetworkMessage(MessageType.BLOCKCHAIN_COPY, blockchain.getChainCopy());
            case BLOCKCHAIN_COPY -> tratarCadeia(mensagem);
            default -> rejeitar("Tipo não esperado como pedido: " + mensagem.getType());
        };
    }

    private NetworkMessage tratarHello(NetworkMessage mensagem) {
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

    private NetworkMessage tratarCadeia(NetworkMessage mensagem) {
        List<Block> recebida = extrairCadeia(mensagem.getPayload());
        if (recebida == null) {
            return rejeitar("BLOCKCHAIN_COPY sem uma lista de blocos válida");
        }

        Blockchain.ResultadoTroca resultado = blockchain.tryReplace(recebida);
        if (!resultado.aceita) {
            return rejeitar(resultado.motivo);
        }

        System.out.println("[CHAIN] Cadeia aceita. Novo tamanho: " + blockchain.size());
        aoAceitarCadeia.run();
        return new NetworkMessage(MessageType.ACK, "Cadeia aceita, " + recebida.size() + " blocos");
    }

    private List<Block> extrairCadeia(Serializable payload) {
        if (!(payload instanceof List<?> lista)) {
            return null;
        }
        List<Block> cadeia = new ArrayList<>();
        for (Object item : lista) {
            if (!(item instanceof Block bloco)) {
                return null;
            }
            cadeia.add(bloco);
        }
        return cadeia;
    }

    private NetworkMessage rejeitar(String motivo) {
        return new NetworkMessage(MessageType.REJECT, motivo);
    }
}