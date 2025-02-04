package descarregamento;

import java.io.Serializable;

@SuppressWarnings("serial")
public class FileBlockRequestMessage implements Serializable {
	
	private String fileHash;
    private int offset;
    private int length;
    private String fileName;

    public FileBlockRequestMessage(String fileHash, int offset, int length, String fileName) {
        this.fileHash = fileHash;
        this.offset = offset;
        this.length = length;
        this.fileName = fileName;
    }

	public String getFileHash() {
		return fileHash;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}
	
	public String getFileName() {
		return fileName;
	}
    
    

}
