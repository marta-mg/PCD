package gui;

import java.io.File;  
import java.io.IOException;
import ligacao.Node;

public class IscTorrent {
	
	public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Uso: java IscTorrent <porto> <path>");
            return;
        }
        
        try {
            int port = Integer.parseInt(args[0]);
            String path = args[1];
            
            File folder = new File("resources/" + path);
            Frame window = new Frame("node " + port);
            Node node = Node.getInstance();
            
            node.setNodeAddress("localhost");
            node.setNodePort(port);
            node.setFolder(folder);
            node.setFrame(window);

    		window.open();
    		node.startServing();            
            
            
        } catch (NumberFormatException e) {
            System.out.println("O primeiro argumento deve ser um número inteiro (porto).");
        }
	}

}

