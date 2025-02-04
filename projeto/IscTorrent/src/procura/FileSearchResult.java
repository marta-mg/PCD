package procura;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("serial")
public class FileSearchResult implements Serializable{
	
	private WordSearchMessage wordSearch;
	private String hash;
	private long fileSize;
	private String fileName;
	private String nodeAddress;
	private int nodePort;
	
	
	public FileSearchResult(WordSearchMessage wordSearch, long fileSize, String fileName, String nodeAddress, int nodePort, String hash) {
		this.wordSearch = wordSearch;
		this.fileSize = fileSize;
		this.fileName = fileName;
		this.nodeAddress = nodeAddress;
		this.nodePort = nodePort;
		this.hash = hash;
		//System.out.println(hash);
	}


	public WordSearchMessage getWordSearch() {
		return wordSearch;
	}
	
	public String getFileHash() {
		return hash;
	}


	public long getFileSize() {
		return fileSize;
	}


	public String getFileName() {
		return fileName;
	}


	public String getNodeAddress() {
		return nodeAddress;
	}


	public int getNodePort() {
		return nodePort;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(hash);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) 
			return true;
	
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		FileSearchResult other = (FileSearchResult) obj;
		return hash.equals(other.hash);
	}
	
	public String toString() {
		return fileName;
	}


}
