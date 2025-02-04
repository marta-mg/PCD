package descarregamento;

import java.io.Serializable;

@SuppressWarnings("serial")
public class FileBlockAnswerMessage implements Serializable {
	
	private final String fileHash;
    private final int offset;
	private byte[] data;
	private int senderNodePort;

	public FileBlockAnswerMessage(String fileHash, int offset, byte[] data, int senderNodePort) {
        this.fileHash = fileHash;
        this.offset = offset;
        this.data = data;
        this.senderNodePort = senderNodePort;
    }

    public byte[] getData() {
        return data;
    }

	public String getFileHash() {
		return fileHash;
	}

	public int getOffset() {
		return offset;
	}
	
	public int getSenderNodePort() {
		return senderNodePort;
	}
    

}
