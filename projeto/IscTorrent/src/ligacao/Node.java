package ligacao;

import java.io.File;  
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gui.Frame;
import descarregamento.DownloadTaskManagerLOCK;
import procura.FileSearchResult;
import procura.FileSearchResultMessage;
import procura.WordSearchMessage;


public class Node {

	private static final Node NODE_INSTANCE = new Node();
	
	private String nodeAddress;
	private int nodePort;
	private ServerSocket serverSocket;
	private File folder;
	private Frame frame;
	private DownloadTaskManagerLOCK downloadManager;
	
	private Map<NodeInfo, ConnectionHandler> handlers;
	private Map<FileSearchResult, List<ConnectionHandler>> nodesWithFile; 
	
	private List<NodeInfo> connectedNodes;
	private List<File> availableFiles;
	
	private int expectedResponses = 0;
	private int responsesReceived = 0;
	
	private static final int NUM_THREADS = 5;
	public ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);

	
	private Node() {
		this.connectedNodes = new ArrayList<>();
		
		this.nodesWithFile = new HashMap<>();
		this.handlers = new HashMap<>();
	}
	
	
		/***  SETS & GETS  ***/
	
	public static Node getInstance() {		
		return NODE_INSTANCE;
	}

	public void setNodeAddress(String nodeAddress) {
		this.nodeAddress = nodeAddress;
	}

	public void setNodePort(int nodePort) { 
		this.nodePort = nodePort;
	}
	
	public void setFolder(File folder) throws IOException {
		this.folder = folder;
		this.availableFiles = new ArrayList<File>(Arrays.asList(folder.listFiles()));
	}
	
	public void setFrame(Frame frame) { 
		this.frame = frame;
	}

	public String getNodeAddress() {
		return nodeAddress;
	}

	public int getNodePort() {
		return nodePort;
	}
	
	public File getFolder() {
		return folder;
	}
	
	public List<NodeInfo> getConnectedNodes(){
		return connectedNodes;
	}
	
	public List<File> getAvailableFiles() {
		return availableFiles;
	}
	
	public DownloadTaskManagerLOCK getDownloadTaskManager() {
		return downloadManager;
	}

	
	public void startServing() {
		try {
			serverSocket = new ServerSocket(this.nodePort);			
			while(true) {
				waitForConnection();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			if(serverSocket != null)
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			
		}
		
	}	
	
		/***      MÉTODOS CHAMADOS POR UM BOTÃO        ***/
	
	//this tenta conectar-se ao Node(destinationAddress, destinationPort)
	public void connectToNode(String destinationAddress, int destinationPort){
		
		try {
			Socket socket = new Socket(destinationAddress, destinationPort);
			ConnectionHandler handler = new ConnectionHandler(socket, this);
			handler.sendMessage(new NewConnectionRequest(this.nodeAddress,this.nodePort));
			
			onNewConnectionEstablished(destinationAddress, destinationPort, handler);
			handler.start();
			System.out.println("Node " + this.nodePort + " connected to: " + destinationPort);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Connection failed");
		}
	}
	
	//envia uma mensagem com a keyword para todos os Nodes da sua rede
	public void search(String keyword) {
		nodesWithFile.clear();
		expectedResponses = handlers.size();
		WordSearchMessage message = new WordSearchMessage(keyword);
		
		for (Map.Entry<NodeInfo, ConnectionHandler> entry : handlers.entrySet()) {
			NodeInfo nodeInfo = entry.getKey();
			ConnectionHandler handler = entry.getValue();
			
			try {
				handler.sendMessage(message);
				System.out.println("WSM sent");
				
			} catch (Exception e) {
				System.err.println("Failed to send search request to Node " + nodeInfo.getAddress() + ":" + nodeInfo.getPort());
				e.printStackTrace();
			}
		}
	}
	
	//começa o download do file
	public void download(FileSearchResult file) {
		List<ConnectionHandler> ch = findFileProviders(file);
		downloadManager = new DownloadTaskManagerLOCK(ch, file.getFileHash().toString(), file.getFileSize(), file.getFileName());
		downloadManager.start();
	}
	

		/*** OUTROS MÉTODOS E FUNÇÕES ***/
	
	private void waitForConnection() throws IOException {
		System.out.println(this.nodePort + ": ready to receive connections");
		Socket connection = serverSocket.accept();
		
		ConnectionHandler handler = new ConnectionHandler(connection, this);
		handler.start();
		
	}
	
	public void onNewConnectionEstablished(String address, int port, ConnectionHandler ch) {
	    NodeInfo nodeInfo = new NodeInfo(address, port);
	    connectedNodes.add(nodeInfo);
	    handlers.put(nodeInfo, ch);
	}
	
	public void searchkeywordInFiles(WordSearchMessage msg, ConnectionHandler requesterHandler) throws IOException, NoSuchAlgorithmException{
		List<FileSearchResult> results = new ArrayList<>();
		String searchWord = msg.getKeyword().toLowerCase();
		
		for (File file : availableFiles) {
			if (file.getName().toLowerCase().contains(searchWord)) {
				byte[] fileContents = Files.readAllBytes(file.toPath());
				byte[] hash = MessageDigest.getInstance("SHA-256").digest(fileContents);
				//System.out.println(file.getName() + " hash:" + bytesToHex(hash));
				
				FileSearchResult result = new FileSearchResult(msg, file.length(), file.getName(), this.getNodeAddress(), this.getNodePort(), bytesToHex(hash));
				results.add(result);
			}
		}
		
		FileSearchResultMessage resultMessage = new FileSearchResultMessage(results);
		requesterHandler.sendMessage(resultMessage);
		System.out.println("FSRM sent");
	}
	
	public synchronized void processResults(List<FileSearchResult> results,  ConnectionHandler ch) {

		for(FileSearchResult file : results) {			
			if (isKeyInMap(nodesWithFile, file)) {
	            for (FileSearchResult existingKey : nodesWithFile.keySet()) {
	                if (existingKey.equals(file)) {
	                    nodesWithFile.get(existingKey).add(ch);
	                    break;
	                }
	            }
	            
			} else {
				List<ConnectionHandler> handlers = new ArrayList<>();
				handlers.add(ch);
				nodesWithFile.put(file, handlers);
			}
		}

		responsesReceived++;

		if (responsesReceived == expectedResponses) {

			frame.displayResults(nodesWithFile);
			responsesReceived = 0;

		}else {
			System.err.println("waiting for more results, responses received: " + responsesReceived + " / " + expectedResponses);
		}
	}
	
	public boolean isKeyInMap(Map<FileSearchResult, List<ConnectionHandler>> map , FileSearchResult key) {		
		for (FileSearchResult existingKey : map.keySet()) {
	        if (existingKey.equals(key)) {
	            return true;
	        }
	    }
		return false;
	}

	public List<ConnectionHandler> findFileProviders(FileSearchResult file){
		
		for (Map.Entry<FileSearchResult, List<ConnectionHandler>> entry : nodesWithFile.entrySet()) {
			if(entry.getKey().equals(file))
				return entry.getValue();
		}
		
		System.err.println("findFileProviders falhou");
		List<ConnectionHandler> m = new ArrayList<>();
        return m;
	}	
	
	public static String bytesToHex(byte[] bytes) {
	    StringBuilder hexString = new StringBuilder();
	    for (byte b : bytes) {
	        String hex = Integer.toHexString(0xff & b);
	        if (hex.length() == 1) {
	            hexString.append('0');
	        }
	        hexString.append(hex);
	    }
	    return hexString.toString();
	}

		
	public void displayOnFrame(Map<Integer, Integer> map, long time) {
		frame.addDownloadCompletedFrame(map, time);
	}

}
