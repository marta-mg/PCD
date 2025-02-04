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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadTaskManagerLOCK extends Thread{
	
private static final int BLOCK_SIZE = 10240;
	
	private List<FileBlockRequestMessage> blockRequests = new ArrayList<>();
	private List<FileBlockAnswerMessage> blockAnswers;
	
	private List<ConnectionHandler> handlers;
	private Node node;
    private String fileHash;
    private long fileSize;
    private String fileName;
    
    private Map<Integer, Integer> downloadMap;
    
    private Lock lock = new ReentrantLock();
    private Condition allBlocksReady = lock.newCondition();
    
	
	public DownloadTaskManagerLOCK(List<ConnectionHandler> handlers, String fileHash, long fileSize, String fileName) {
        this.node = Node.getInstance();
        this.handlers = handlers;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.fileName = fileName;
        this.blockRequests = createBlockRequests(fileSize);
        System.out.println("Blocks created: " + blockRequests.size());
        this.blockAnswers = new ArrayList<>();
        this.downloadMap = new HashMap<>();
    }
	

	//método take()
	public FileBlockRequestMessage getBlock() {
		lock.lock();
		try {
			if(blockRequests.size() == 0)
				return null;
			
			return blockRequests.remove(0);			
			
		} finally {
			lock.unlock();
		}
	}
	
	//método put()
	public void submitResult(FileBlockAnswerMessage block) {
		lock.lock();
		try {
			blockAnswers.add(block);
			addToMap(block);
			if (blockAnswers.size() == createBlockRequests(fileSize).size()) {
				allBlocksReady.signalAll(); 
			}
			
		} finally {
			lock.unlock();
		} 
	}
	
	
	
	public void run() {
		long startTime = System.currentTimeMillis();
		
		//1º - cada thread tira um bloco e envia uma mensagem FBRM
		while(!(blockRequests.isEmpty())) {
			
			for(ConnectionHandler handler : handlers) {
				new Thread(() -> { 
					FileBlockRequestMessage block = getBlock();
					handler.sendMessage(block);
				}).start();
			}
		}
		
		//2º - espera que todos os blocos estejam prontos
		lock.lock();
		try {
			while(blockAnswers.size() < createBlockRequests(fileSize).size()) {
				allBlocksReady.await();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			
		} finally {
			lock.unlock();
		}
		
		//3º - assembleFile() e mostra as informações do download na GUI
		try {
			assembleFile();
		    long endTime = System.currentTimeMillis();
		    long elapsedTime = endTime - startTime;
			node.displayOnFrame(downloadMap, elapsedTime);
			
		} catch (IOException e) {
			System.err.println("error assembling file");
		}
		
	}
	
	
	// cria uma lista de block requests de acordo com o tamenho do file
	private List<FileBlockRequestMessage> createBlockRequests(long fileSize) {
		List<FileBlockRequestMessage> requests = new ArrayList<>();
		int offset = 0;
		
		while (offset < fileSize) {
			int length = (int) Math.min(BLOCK_SIZE, fileSize - offset); 	// ultimo bloco pode ser menor
			requests.add(new FileBlockRequestMessage(fileHash, offset, length, fileName));
			offset += length;
		}
		
		return requests;
	}
	
	//adiciona um bloco descarregado e o node que o enviou ao mapa
	public void addToMap(FileBlockAnswerMessage block) {
		int senderPort = block.getSenderNodePort();
		downloadMap.put(senderPort, downloadMap.getOrDefault(senderPort, 0) + 1);
	}
	
	//junta todos os blocos num ficheiro e guarda no disco
	private void assembleFile() throws IOException {
		blockAnswers.sort((a, b) -> Integer.compare(a.getOffset(), b.getOffset()));
		File folder = node.getFolder();
		File outputFile = new File(folder, fileName);
		
	    if (!outputFile.exists()) {
	        outputFile.createNewFile();
	    }
		
		for(FileBlockAnswerMessage block : blockAnswers) {
			
			Files.write(outputFile.toPath(), block.getData());
			
		}
	}

}
