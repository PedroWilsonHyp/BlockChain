package pacote;
import java.util.List;

/**
 * Contrato entre o Menu (sua parte) e o P2PNode (parte do resto do grupo).
 * O P2PNode deve implementar esta interface e passá-la ao criar o Menu:
 *
 *     new Menu(blockchain, p2pNode, config.getNodeId()).run();
 */
public interface NodeActions {

    /** Peers conhecidos, no formato "IP:Porta" (vem do PeerManager). */
    List<String> getPeers();

    /** Sincronização manual: GET_BLOCKCHAIN para os peers e aplica a regra da maior cadeia válida. */
    void syncNow();

    /** Broadcast de BLOCKCHAIN_COPY com a cadeia local para todos os peers (Fluxo C). */
    void broadcastChain();
}
