package perotocoloBPC;

import java.io.Serializable; 
import java.util.Collection;

public class NetworkMessage implements Serializable  {
		 private static final long serialVersionUID = 1L; 
		 private final MessageType type;
		 private final Serializable payload;
		 private int a; 
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
				 throw new IllegalArgumentException("A menssagem não pode ser do tipo null");
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

		

		 
		 
}
