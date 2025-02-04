package ligacao;

public class NodeInfo {
	
    private String address;
    private int port;

    public NodeInfo(String address, int port) {
        this.address = address;
        this.port = port;
    }

    // Getters and setters
    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
    
    public String toString() {
    	return (address + ":" + port);
    }
}
