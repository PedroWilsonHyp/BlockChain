package bcp.protocolo;

import java.io.Serializable; 
import java.util.Collection;

public final class NetworkMessage implements Serializable  {
    private static final long serialVersionUID = 1L; 
    private final MessageType type;
    private final Serializable payload;
    public enum MessageType{
        HELLO,
        GET_PEERS,
        PEERS_LIST,
        GET_BLOCKCHAIN,
        BLOCKCHAIN_COPY,
        ACK,
        REJECT
    }

    public NetworkMessage( MessageType type, Serializable payload ) {
        if ( type == null) {
            throw new IllegalArgumentException("O tipo da mensagem não pode ser null");
        }
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public Serializable getPayload() {
        return payload;
    }



    @Override
    public String toString() {
        String resumo;
        if (payload == null) {
            resumo = "sem payload";
        } else if (payload instanceof Collection<?> c) {
            resumo = c.size() + " itens";
        } else {
            resumo = payload.toString();
        }
        return type + " [" + resumo + "]";
    }
}