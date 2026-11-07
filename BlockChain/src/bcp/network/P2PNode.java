package bcp.network;

import bcp.protocolo.NetworkMessage;
import bcp.protocolo.NetworkMessage.MessageType;
import pacote.Block;
import pacote.Blockchain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class P2PNode {

    public static final String SEED_PADRAO = "127.0.0.1:8333";

    private final String idNo;
    private final int porta;
    private final Blockchain blockchain;
    private final PeerManager peerManager;
    private final PeerClient client;
    private final P2PServer server;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public P2PNode(String idNo, String ip, int porta, Blockchain blockchain) {
        if (idNo.contains("|")) {
            throw new IllegalArgumentException("O identificador do nó não pode conter '|': " + idNo);
        }
        this.idNo = idNo;
        this.porta = porta;
        this.blockchain = blockchain;
        this.peerManager = new PeerManager(ip + ":" + porta);
        this.client = new PeerClient();
        this.server = new P2PServer(porta, peerManager, blockchain, this::broadcastChain);
    }

    
    public void iniciar() throws IOException {
        server.start();
    }


    public void bootstrap(String seed) {
        System.out.println("[BOOT] Conectando ao seed " + seed);

        if (!apresentar(seed)) {
            System.out.println("[BOOT] Seed indisponível. Nó segue sozinho, aguardando conexões.");
            return;
        }
        peerManager.adicionar(seed);

        for (String peer : pedirPeers(seed)) {
            try {
                if (peerManager.adicionar(peer) && !apresentar(peer)) {
                    peerManager.remover(peer);
                }
            } catch (IllegalArgumentException e) {
                System.out.println("[BOOT] Endereço inválido ignorado: " + peer);
            }
        }

        System.out.println("[BOOT] Concluído. Peers conhecidos: " + peerManager.quantidade());
        syncNow();
    }

    public List<String> getPeers() {
        return peerManager.listar();
    }

    // ---------- Fluxo B: sincronização ----------

    public void syncNow() {
        List<String> peers = peerManager.listar();
        if (peers.isEmpty()) {
            System.out.println("[SYNC] Nenhum peer conhecido para sincronizar.");
            return;
        }
        for (String peer : peers) {
            sincronizarCom(peer);
        }
        System.out.println("[SYNC] Concluído. Cadeia local: " + blockchain.size() + " blocos");
    }

    private void sincronizarCom(String peer) {
        try {
            NetworkMessage pedido = new NetworkMessage(MessageType.GET_BLOCKCHAIN, null);
            NetworkMessage resposta = client.enviarEReceber(peer, pedido);

            List<Block> recebida = extrairCadeia(resposta);
            if (recebida == null) {
                System.out.println("[SYNC] Resposta inesperada de " + peer + ": " + resposta);
                return;
            }

            Blockchain.ResultadoTroca resultado = blockchain.tryReplace(recebida);
            if (resultado.aceita) {
                System.out.println("[SYNC] Cadeia de " + peer + " aceita (" + recebida.size() + " blocos)");
            } else {
                System.out.println("[SYNC] Cadeia de " + peer + " recusada: " + resultado.motivo);
            }
        } catch (IOException e) {
            System.out.println("[SYNC] " + peer + " inacessível, removido: " + e.getMessage());
            peerManager.remover(peer);
        } catch (RuntimeException e) {
            System.out.println("[SYNC] Cadeia malformada recebida de " + peer + ": " + e);
        }
    }

    // ---------- Fluxo C: anúncio de nova cadeia ----------

    public void broadcastChain() {
        NetworkMessage mensagem = new NetworkMessage(MessageType.BLOCKCHAIN_COPY, blockchain.getChainCopy());
        System.out.println("[CHAIN] Anunciando cadeia de " + blockchain.size() + " blocos");
        for (String peer : peerManager.listar()) {
            pool.execute(() -> enviarCadeia(peer, mensagem));
        }
    }

    private void enviarCadeia(String peer, NetworkMessage mensagem) {
        try {
            NetworkMessage resposta = client.enviarEReceber(peer, mensagem);
            System.out.println("[CHAIN -> " + peer + "] " + mensagem + " => " + resposta);
        } catch (IOException e) {
            System.out.println("[CHAIN] " + peer + " inacessível, removido: " + e.getMessage());
            peerManager.remover(peer);
        } catch (RuntimeException e) {
            System.out.println("[CHAIN] Erro ao enviar para " + peer + ": " + e);
        }
    }

    // ---------- Auxiliares ----------

    private boolean apresentar(String endereco) {
        try {
            NetworkMessage hello = new NetworkMessage(MessageType.HELLO, payloadHello());
            NetworkMessage resposta = client.enviarEReceber(endereco, hello);
            if (resposta.getType() == MessageType.ACK) {
                System.out.println("[BOOT] HELLO aceito por " + endereco);
                return true;
            }
            System.out.println("[BOOT] HELLO recusado por " + endereco + ": " + resposta.getPayload());
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("[BOOT] " + endereco + " inacessível: " + e.getMessage());
        }
        return false;
    }

    private List<String> pedirPeers(String endereco) {
        try {
            NetworkMessage pedido = new NetworkMessage(MessageType.GET_PEERS, null);
            NetworkMessage resposta = client.enviarEReceber(endereco, pedido);

            if (resposta.getType() == MessageType.PEERS_LIST
                    && resposta.getPayload() instanceof List<?> lista) {
                List<String> enderecos = new ArrayList<>();
                for (Object item : lista) {
                    if (item instanceof String texto) {
                        enderecos.add(texto);
                    }
                }
                return enderecos;
            }
            System.out.println("[BOOT] Resposta inesperada ao GET_PEERS: " + resposta);
        } catch (IOException e) {
            System.out.println("[BOOT] Falha ao pedir peers a " + endereco + ": " + e.getMessage());
        }
        return List.of();
    }

    private List<Block> extrairCadeia(NetworkMessage resposta) {
        if (resposta.getType() != MessageType.BLOCKCHAIN_COPY
                || !(resposta.getPayload() instanceof List<?> lista)) {
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

    private String payloadHello() {
        return NetworkMessage.VERSAO_PROTOCOLO + "|" + idNo + "|" + porta;
    }
}