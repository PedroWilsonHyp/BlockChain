package bcp.network;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PeerManager {

    private final String enderecoProprio;
    private final Set<String> peers = ConcurrentHashMap.newKeySet();

    public PeerManager(String enderecoProprio) {
        this.enderecoProprio = normalizar(enderecoProprio);
    }

    public boolean adicionar(String endereco) {
        String normalizado = normalizar(endereco);
        if (normalizado.equals(enderecoProprio)) {
            return false;
        }
        return peers.add(normalizado);
    }

    public void remover(String endereco) {
        peers.remove(normalizar(endereco));
    }

    public ArrayList<String> listar() {
        return new ArrayList<>(peers);
    }

    public int quantidade() {
        return peers.size();
    }

    private String normalizar(String endereco) {
        String[] partes = endereco.trim().split(":");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Endereço deve ser um IP :porta, recebido: " + endereco);
        }
        String ip = partes[0].trim();
        int porta = Integer.parseInt(partes[1].trim());
        if (porta < 1 || porta > 65535) {
            throw new IllegalArgumentException("Porta não válida: " + porta);
        }
        return ip + ":" + porta;
    }
}