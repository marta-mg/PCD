package gui;

import java.awt.BorderLayout;  
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import ligacao.ConnectionHandler;
import ligacao.Node;
import procura.FileSearchResult;

public class Frame {
	
	private JFrame frame; 
	private JPanel centerPanel;
	private Node node;
	
    private JList<String> jList;
    private DefaultListModel<String> listModel;
    private List<FileSearchResult> fileSearchResults;
    
    private static final Color PINK = new Color(252, 188, 218);	
	
	
	public Frame(String title) {
		this.node = Node.getInstance();
		frame = new JFrame(title);
		frame.setSize(600, 300);
		frame.setLocation(200, 100);
				
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);	
		addFrameContent();
		
	}
	
	public void open() {
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);
	}

	
	private void addFrameContent() {
	   //CIMA
		JPanel northPanel = pinkJPanel();
        northPanel.setLayout(new GridLayout(1, 3)); 

        northPanel.add(new JLabel("Texto a procurar: "));
        JTextField text = new JTextField("");
        northPanel.add(text);
        
        JButton search = new JButton("Procurar");
        search.setBackground(PINK);
        search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				node.search(text.getText());
				
			}
		});
        northPanel.add(search);

        // adicionar o painel à região norte
        frame.add(northPanel, BorderLayout.NORTH);
        
       //DIREITA
        JPanel eastPanel = pinkJPanel();
        eastPanel.setLayout(new GridLayout(2,1));
        
        JButton download = new JButton("Descarregar");
        download.setBackground(PINK);
        download.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				int selectedFile = jList.getSelectedIndex();
				if(selectedFile != -1) {
					FileSearchResult selectedResult = fileSearchResults.get(selectedFile);
					node.download(selectedResult);
				}
				
			}

		});
        eastPanel.add(download);
        
        JButton connect = new JButton("Ligar a Node");
        connect.setBackground(PINK);
        connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addConnectFrame();		
			}

		});
        eastPanel.add(connect);
        
        // adicionar o painel à região este
        frame.add(eastPanel, BorderLayout.EAST);
        
        //CENTRO
        centerPanel = pinkJPanel();
        centerPanel.setLayout(new BorderLayout());
        
        listModel = new DefaultListModel<>();
        jList = new JList<>(listModel);
        jList.setBackground(PINK);
        
        centerPanel.add(new JScrollPane(jList), BorderLayout.CENTER);
        frame.add(centerPanel, BorderLayout.CENTER);
        
	}
	
	private void addConnectFrame() {
		JFrame connectFrame = new JFrame("Ligar a Node remoto");
		
		JPanel backgroundPanel = pinkJPanel();
	    backgroundPanel.setLayout(new FlowLayout());
		
		JLabel end = new JLabel("Endereço: ");
		backgroundPanel.add(end);
		
		JTextField text = new JTextField("localhost");
		text.setPreferredSize(new Dimension(100, 20));
		backgroundPanel.add(text);
		
		JLabel port = new JLabel("Porta: ");
		backgroundPanel.add(port);
		
		JTextField text2 = new JTextField();
		text2.setPreferredSize(new Dimension(50, 20));
		backgroundPanel.add(text2);
		
		JButton cancelar = new JButton("Cancelar");
		cancelar.setBackground(PINK);
        cancelar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				connectFrame.dispose();	
			}

		});
        backgroundPanel.add(cancelar);
        
        JButton ok = new JButton("OK");
        ok.setBackground(PINK);
        ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				node.connectToNode(text.getText(), Integer.parseInt(text2.getText()));
			}
		});
        backgroundPanel.add(ok);
        
        connectFrame.setContentPane(backgroundPanel);
        connectFrame.pack();
		connectFrame.setVisible(true);		
		connectFrame.setLocationRelativeTo(null);
	}

	public void addDownloadCompletedFrame(Map<Integer, Integer> map, long time) {
		JFrame downloadFrame = new JFrame();
		downloadFrame.setUndecorated(true);
		
		JPanel backgroundPanel = pinkJPanel();
		backgroundPanel.setLayout(new GridLayout(0, 1));
		
		downloadFrame.setSize(300, 100);
		
		JLabel text1 = new JLabel("Descarga Completa.\n" );
		backgroundPanel.add(text1);
		
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			JLabel text2 = new JLabel("Fornecedor [porto=" + entry.getKey() + "]:" + entry.getValue());
			backgroundPanel.add(text2);
		}
		
		JLabel timeText = new JLabel("Tempo decorrido:" + String.format("%.1f", time*0.001) + "s" );
		backgroundPanel.add(timeText);
		
		JButton closeButton = new JButton(" OK ");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				downloadFrame.dispose();
			}
		});
		backgroundPanel.add(closeButton);
		
		downloadFrame.setContentPane(backgroundPanel);
		downloadFrame.setVisible(true);		
		downloadFrame.setLocationRelativeTo(null);
	}
	

	public void displayResults(Map<FileSearchResult, List<ConnectionHandler>> map) {
		
		listModel = new DefaultListModel<>();
        fileSearchResults = new ArrayList<>(); 

        for (Map.Entry<FileSearchResult, List<ConnectionHandler>> entry : map.entrySet()) {
            FileSearchResult file = entry.getKey();
            int handlerCount = entry.getValue().size();            
         
            listModel.addElement(file + " <" + handlerCount + ">");            
            fileSearchResults.add(file);
        }

        jList.setModel(listModel);
    }


	private JPanel pinkJPanel() { 
		ImageIcon backgroundIcon = new ImageIcon(getClass().getResource("/resources/background.jpg")); 
		
		@SuppressWarnings("serial")
		JPanel backgroundPanel = new JPanel() {
	        @Override
	        protected void paintComponent(Graphics g) {
	            super.paintComponent(g);
	            g.drawImage(backgroundIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
	        }
	    };
	    return backgroundPanel;
	}

	

}
