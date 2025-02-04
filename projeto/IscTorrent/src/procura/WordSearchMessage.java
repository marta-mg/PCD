package procura;

import java.io.Serializable;

@SuppressWarnings("serial")
public class WordSearchMessage implements Serializable {
	private String keyword;
	
	
	public WordSearchMessage(String keyword) {
		this.keyword = keyword;
	}
	
	
	public String getKeyword() {
		return keyword;
	}
	

}
