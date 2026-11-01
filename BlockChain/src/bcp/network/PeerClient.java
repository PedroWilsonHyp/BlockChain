package bcp.network;
import bcp.protocolo.NetworkMessage;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class PeerClient {
	private static final int CONNECT_TIMEOUT = 3000;
	private static final int READ_TIMEOUT =  10000;


	private InetSocketAddress extrairEndereco   (String endereco) {
		String [] parts = endereco.split(":");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Endereço tem que ser IP:porta, recebido:" + endereco);
		}
		return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
	}

	public NetworkMessage sendAndReceive(String endereco, NetworkMessage mensagem) throws IOException {
		InetSocketAddress destino = extrairEndereco(endereco);

		try (Socket socket = new Socket()){
			socket.connect(destino,CONNECT_TIMEOUT);
			socket.setSoTimeout(READ_TIMEOUT);

			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();

			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			out.writeObject(mensagem);
			out.flush();

			Object resposta = in.readObject();
			if (!(resposta instanceof NetworkMessage reply)) {
				throw new IOException ("Respsota recebida não é uma NetworkMessage");
			}
			return reply;
		} catch (ClassNotFoundException e) {
			throw new IOException("Resposta contém uma classe não conhecida", e);
		}
	}


}
