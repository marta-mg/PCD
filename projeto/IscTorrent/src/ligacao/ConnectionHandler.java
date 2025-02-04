package ligacao;

import java.io.File; 
import java.io.IOException;  
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import descarregamento.FileBlockAnswerMessage;
import descarregamento.FileBlockRequestMessage;
import procura.WordSearchMessage;
import procura.FileSearchResult;
import procura.FileSearchResultMessage;



public class ConnectionHandler extends Thread {
	
	private Node currentNode;
	private Socket connection;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	
	private static final int NUM_THREADS = 5;
	private ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);

	public ConnectionHandler(Socket connection, Node node) {
		this.connection = connection;
		this.currentNode = node;
		try {
			getStreams();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public void run() {
		try {			
			while(true) {				
				receiveMessage();
			}

		} finally {
			System.out.println("while done" + this.currentNode.getNodePort());
			closeConnection();
		}
	}

	private void getStreams() throws IOException {
		out = new ObjectOutputStream(connection.getOutputStream());
		in = new ObjectInputStream(connection.getInputStream());
	}
	
	private void closeConnection() {
		try {
			if(in != null)
				in.close();
			if(out != null)
				out.close();
			if(connection != null)
				connection.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendMessage(Object obj) {
		try {
        	synchronized(out) {
        		out.writeObject(obj);
        		out.flush();
        	}
        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem: " + e.getMessage());
            e.printStackTrace();
        }
	}
	
	public synchronized void receiveMessage() {
		
			try {
				Object obj = in.readObject();

				//se NewConnectionRequest adiciona à lista de connectedNodes
				if(obj instanceof NewConnectionRequest) {
					NewConnectionRequest request = (NewConnectionRequest) obj;
					System.out.println("New Connection Request from " + request.getPort());
					currentNode.onNewConnectionEstablished(request.getAddress(), request.getPort(), this);					
				}
				
				//se WordSearchMessage procura nos seus files pela palavra e envia uma mensagem com a lista de resultados
				if(obj instanceof WordSearchMessage) {
					System.out.println("WSM received ");
					WordSearchMessage msg = (WordSearchMessage) obj;				
					currentNode.searchkeywordInFiles(msg, this);
				}
				
				//se FileSearchResultMessage processa os resultados e mostra a lista de resultados na GUI
				if(obj instanceof FileSearchResultMessage) {
					System.out.println("FSRM received ");
					FileSearchResultMessage list = (FileSearchResultMessage) obj;
					List<FileSearchResult> results = list.getResults();
					currentNode.processResults(results, this);
				}
				
				//se FileBlockRequestMessage a threadpool começa a trabalhar
				if(obj instanceof FileBlockRequestMessage) {
					FileBlockRequestMessage blockRequest = (FileBlockRequestMessage) obj;
					threadPool.submit(() -> {
						//task - load and send
						byte[] blockData = loadBlock(blockRequest);
						FileBlockAnswerMessage answer = new FileBlockAnswerMessage(blockRequest.getFileHash(), blockRequest.getOffset(), blockData, currentNode.getNodePort());
						sendMessage(answer);
					});
				}		
				//se FileBlockAnswerMessage adiciona o bloco à lista de blocos descarregados
				if(obj instanceof FileBlockAnswerMessage) {
					FileBlockAnswerMessage blockAnswer = (FileBlockAnswerMessage) obj;
					currentNode.getDownloadTaskManager().submitResult(blockAnswer);
				}


			} catch (IOException | ClassNotFoundException | NoSuchAlgorithmException e) {
				//e.printStackTrace();
				
			}
		
	}
	
	private byte[] loadBlock(FileBlockRequestMessage blockRequest) {
		String filePath = currentNode.getFolder() + "/" + blockRequest.getFileName();
		
		byte[] blockData = new byte[blockRequest.getLength()];
		
		File file = new File(filePath);
	    if (!file.exists()) {
	        System.err.println("File not found at path: " + filePath);
	        return null; 
	    }
		
		try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
			raf.seek(blockRequest.getOffset());
			raf.readFully(blockData);
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return blockData;
	}
	
	
	public static String hexToString(String hexString) {
		byte[] bytes = HexFormat.of().parseHex(hexString);
		return new String(bytes, Charset.forName("SHA-256"));
	}


}
