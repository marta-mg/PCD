package descarregamento;

import java.io.File; 
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ligacao.ConnectionHandler;
import ligacao.Node;

public class DownloadTaskManager extends Thread{
	
	private static final int BLOCK_SIZE = 10240;
	
	private List<FileBlockRequestMessage> blockRequests = new ArrayList<>();
	private List<FileBlockAnswerMessage> blockAnswers;
	
	private List<ConnectionHandler> handlers;
	private Node node;
    private String fileHash;
    private long fileSize;
    private String fileName;
    
    private Map<Integer, Integer> downloadMap;
	
	public DownloadTaskManager(List<ConnectionHandler> handlers, String fileHash, long fileSize, String fileName) {
        this.node = Node.getInstance();
        this.handlers = handlers;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.fileName = fileName;
        this.blockRequests = createBlockRequests(fileSize);
        this.blockAnswers = new ArrayList<>();
        this.downloadMap = new HashMap<>();
    }
	
	
	 // cria uma lista de block requests para todo o file
    private List<FileBlockRequestMessage> createBlockRequests(long fileSize) {
    	List<FileBlockRequestMessage> requests = new ArrayList<>();
    	int offset = 0;

    	while (offset < fileSize) {
    		int length = (int) Math.min(BLOCK_SIZE, fileSize - offset); 	// ultimo bloco pode ser menor
    		requests.add(new FileBlockRequestMessage(fileHash, offset, length, fileName));
    		offset += length;
    	}
    	System.out.println(requests.size());
    	return requests;
    }

	public synchronized FileBlockRequestMessage getBlock() {
		//dá um bloco da lista blockRequests e retira-o da lista
		if(blockRequests.size() == 0)
			return null;
		
		FileBlockRequestMessage aux = blockRequests.get(0);
		blockRequests.remove(0);
		return aux;
	}
	
	public synchronized void submitResult(FileBlockAnswerMessage block) {
		blockAnswers.add(block);
	    addToMap(block);
	    if (blockAnswers.size() == createBlockRequests(fileSize).size()) {
	        notifyAll(); 
	    }
	}
	
	public void addToMap(FileBlockAnswerMessage block) {
		int senderPort = block.getSenderNodePort();
		downloadMap.put(senderPort, downloadMap.getOrDefault(senderPort, 0) + 1);
	}
	
	
	public void run() {
		long startTime = System.currentTimeMillis();
		while(!(blockRequests.isEmpty())) {

			for(ConnectionHandler handler : handlers) {
				new Thread(() -> { 
					FileBlockRequestMessage block = getBlock();
					handler.sendMessage(block);
				}).start();
			}

		}

		synchronized (this) {
			while(blockAnswers.size() < createBlockRequests(fileSize).size()) {
				try {
					wait();
					System.err.println("waiting");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} 
		}


		try {
			assembleFile();
		    long endTime = System.currentTimeMillis();
		    long elapsedTime = endTime - startTime;
			node.displayOnFrame(downloadMap, elapsedTime);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void assembleFile() throws IOException {
		System.err.println("entrou no assemble");
		blockAnswers.sort((a, b) -> Integer.compare(a.getOffset(), b.getOffset()));
		File folder = node.getFolder();
		File outputFile = new File(folder, fileName);
		
	    if (!outputFile.exists()) {
	        outputFile.createNewFile();
	    }
		
	    System.out.println(outputFile.toPath());
		for(FileBlockAnswerMessage block : blockAnswers) {
			
			Files.write(outputFile.toPath(), block.getData());
			
		}
	}
	
}
