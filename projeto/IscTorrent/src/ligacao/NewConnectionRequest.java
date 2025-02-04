package ligacao;

import java.io.Serializable;

@SuppressWarnings("serial")
public class NewConnectionRequest implements Serializable {

	private String address;
	private int port;

	public NewConnectionRequest(String address, int port) {
		this.address = address;
		this.port = port;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}
	

}
