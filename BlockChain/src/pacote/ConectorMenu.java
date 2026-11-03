package pacote;

import bcp.network.P2PNode;

import java.util.List;

/**
 * Liga o Menu à rede: implementa Menu.NodeActions e repassa cada ação ao P2PNode,
 * que usa o PeerManager (lista de peers), o PeerClient e o restante.
 */
public class ConectorMenu implements Menu.NodeActions {

    private final P2PNode node;

    public ConectorMenu(P2PNode node) {
        this.node = node;
    }

    @Override
    public List<String> getPeers() {
        return node.getPeers();      // PeerManager.listar()
    }

    @Override
    public void syncNow() {
        node.syncNow();              // GET_BLOCKCHAIN aos peers + tryReplace
    }

    @Override
    public void broadcastChain() {
        node.broadcastChain();       // BLOCKCHAIN_COPY para todos os peers
    }
}
