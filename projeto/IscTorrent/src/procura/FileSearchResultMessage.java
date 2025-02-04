package procura;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class FileSearchResultMessage implements Serializable{
	
	private List<FileSearchResult> results;
	
	public FileSearchResultMessage(List<FileSearchResult> results) {
		this.results = results;
	}

	public List<FileSearchResult> getResults() {
		return results;
	}
	
	

}
