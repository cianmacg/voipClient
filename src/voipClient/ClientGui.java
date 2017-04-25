package voipClient;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;

/* 
 * This class handles everything to do with the client, including creating the gui, creating an "AudioInput" object to
 * access and use the devices microphone, creating an "AudioOutput" object to access and use the devices speakers, receives 
 * packets from a server and decides what to do with them.
 */
public class ClientGui extends JFrame implements KeyListener{
	
	private int pttKey = KeyEvent.VK_T;
	private ArrayList<String> otherUsers = new ArrayList<String>();
	private final AudioOutput speakers = new AudioOutput();
	private final AudioInput microphone = new AudioInput();
	private final byte voiceByte = Byte.parseByte("00000001", 2);
	private final byte newUser = Byte.parseByte("00000010", 2);
	private final byte removeUser = Byte.parseByte("00000011", 2);
	private final byte connSuccess = Byte.parseByte("00000100", 2);
	private final byte connEnd = Byte.parseByte("00000101", 2);
	private final int defaultClientPort = 8150;
	private InetAddress serverAddress;
	private int serverPort = 8090;
	private boolean connected = false;
	private JFrame frame = new JFrame();
	private JButton btnConnect = new JButton("Connect");
	private JButton btnDisconnect =  new JButton("Disconnect");
	private JTextArea msgListArea = new JTextArea();
	private JTextArea userListArea = new JTextArea();
	private boolean isPtt = true;
	private String userN = null;
	
	public ClientGui(){
		initUI();
		microphone.setup();
		Thread receivePackets = new Thread(new getAudio());
		receivePackets.start();
	}
	
	public void initUI(){

		frame = new JFrame();
		frame.setBounds(100, 100, 593, 424);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
        frame.setVisible(true);
        frame.setFocusable(true);
        frame.setResizable(false);
        frame.addKeyListener(this);
		
		btnDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				disconnect();
			}
		});
		btnDisconnect.setEnabled(false);
		btnDisconnect.setBounds(478, 36, 89, 23);
		frame.getContentPane().add(btnDisconnect);
		
		
		
		
		JPanel connPanel = new JPanel();
		JButton connect = new JButton("Connect");
		JButton back = new JButton("Back");
		JTextField userName = new JTextField("", 10);
		JTextField ipAdd = new JTextField("", 16);
		JTextField port = new JTextField("8090");
		JLabel user = new JLabel("Username:");
		JLabel ipText = new JLabel(" IP Address:");
		JLabel portText = new JLabel(" Port:");
		
		
		connPanel.add(user);
		connPanel.add(userName);
		connPanel.add(ipText);
		connPanel.add(ipAdd);
		connPanel.add(portText);
		connPanel.add(port);
		
		connPanel.add(connect);
		connPanel.add(back);
		JFrame connFrame = new JFrame();	//this frame is created when we attempt to connect to a server
		connFrame.getContentPane().add(connPanel);
        connFrame.setSize(500, 100);
        connFrame.setResizable(true);
        connFrame.setLocationRelativeTo(frame.getContentPane());
		connFrame.setVisible(false);
        
		
		
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				connFrame.setVisible(true);
				frame.setVisible(false);
			}
		});
		
		btnConnect.setBounds(359, 36, 89, 23);
		frame.getContentPane().add(btnConnect);
		
		connect.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				
				userN = userName.getText();
				tryConnection(userName.getText(), ipAdd.getText(), Integer.parseInt(port.getText()));
				btnConnect.setEnabled(false);
				btnDisconnect.setEnabled(true);
				connFrame.setVisible(false);
				frame.setVisible(true);
				
			}
			
		});
		
		back.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				connFrame.setVisible(false);
				frame.setVisible(true);
			}
			
		});
		
		
		
		JLabel lblVoiceInputType = new JLabel("Voice Input Type:");
		lblVoiceInputType.setBounds(20, 19, 97, 14);
		frame.getContentPane().add(lblVoiceInputType);
		
	       JPanel pttPanel = new JPanel();		//Panel for the Push to talk(ptt) frame
	        pttPanel.add(new JLabel("Push the Button you would like to be your Push-To-Talk key now!"));
			
			JButton btnClickToChange = new JButton("Set Push-to-Talk button (currently set to '"+ (char)pttKey + "')");
			btnClickToChange.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFrame pttFrame = new JFrame();		//this frame is created when we want to set a new Push To Talk Key
					pttFrame.setVisible(true);
					frame.setVisible(false);
					
					pttFrame.addKeyListener(new KeyListener(){		//Listens for the key we want as our Push to talk key

						@Override
						public void keyPressed(KeyEvent e) {
							pttKey = e.getKeyCode();
							btnClickToChange.setText("Set Push-to-Talk button (currently set to '"+ (char)pttKey + "')");
							frame.setVisible(true);
							pttFrame.dispose();
						}

						@Override
						public void keyReleased(KeyEvent e) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void keyTyped(KeyEvent e) {
							// TODO Auto-generated method stub
							
						}		        	
			        });
					
			        pttFrame.getContentPane().add(pttPanel);
			        pttFrame.setSize(400, 75);
			        pttFrame.setResizable(true);
			        pttFrame.setLocationRelativeTo(frame.getContentPane());
				}
			});
			btnClickToChange.setBounds(20, 96, 258, 35);
			btnClickToChange.addKeyListener(this);
			frame.getContentPane().add(btnClickToChange);
		
		ButtonGroup buttonGroup = new ButtonGroup();
		
		JRadioButton rdbtnContinuous = new JRadioButton("Continuous");
		rdbtnContinuous.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnClickToChange.setEnabled(false);
				isPtt = false;
				microphone.start();
			}
		});
		buttonGroup.add(rdbtnContinuous);
		rdbtnContinuous.setBounds(20, 40, 109, 23);
		frame.getContentPane().add(rdbtnContinuous);
		
		JRadioButton rdbtnPushtotalk = new JRadioButton("Push-To-Talk");
		rdbtnPushtotalk.addKeyListener(this);
		rdbtnPushtotalk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isPtt = true;
				btnClickToChange.setEnabled(true);
				microphone.finish();
			}
		});
		buttonGroup.add(rdbtnPushtotalk);
		rdbtnPushtotalk.setSelected(true);
		rdbtnPushtotalk.setBounds(20, 66, 109, 23);
		frame.getContentPane().add(rdbtnPushtotalk);
				
		JLabel lblServerUserList = new JLabel("Server User List:");
		lblServerUserList.setBounds(32, 142, 97, 14);
		frame.getContentPane().add(lblServerUserList);
		
		
		userListArea.setEditable(false);
		userListArea.setWrapStyleWord(true);


		
		JLabel lblServerMessageList = new JLabel("Server Message List:");
		lblServerMessageList.setBounds(359, 70, 145, 14);
		frame.getContentPane().add(lblServerMessageList);
		msgListArea.setLineWrap(true);
		
		JScrollPane msgsPane = new JScrollPane(msgListArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JScrollPane usersPane = new JScrollPane(userListArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		msgListArea.setEditable(false);
		msgListArea.setWrapStyleWord(true);
		msgsPane.setBounds(341, 96, 226, 281);		
		usersPane.setBounds(20, 159, 258, 218);
		frame.getContentPane().add(msgsPane);
		frame.getContentPane().add(usersPane);
		frame.setTitle("Voip Client");
		for(Component c : frame.getComponents()){
			c.addKeyListener(this);
		}
	}
	
	
	
	
	
	public static void main(String[] args) {
		try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
		
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ClientGui ex = new ClientGui();
                ex.setVisible(true);
            }
		});
		
    }
	

	//Keys can be used for Push-to-talk(ptt) option
	@Override
	public void keyPressed(KeyEvent e) {
		if((e.getKeyCode() == pttKey) && isPtt){
			microphone.start();
		}

	}

	@Override
	public void keyReleased(KeyEvent e) {
		if((e.getKeyCode() == pttKey)&& isPtt){
			microphone.finish();
		}
		
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
	}
	
	//Updates the list of users which are currently connected to the server (in gui)
	public void updateUsers(){
		String list = new String("");
		for(String s : otherUsers){
			list = new String(list +"\n"+ s);
		}
		userListArea.setText(list);
	}
	
	
	//attemp to connect to a server
	private void tryConnection(String name, String address, int port){
		try {
			serverAddress = InetAddress.getByName(address);
			serverPort = port;
			otherUsers.add(userN);
			
			if(!serverAddress.equals(null)&& serverPort!=0){
				DatagramSocket serverSocket;
				serverSocket = new DatagramSocket();
				byte[] conn = new byte[51];
				conn[0] = newUser;
				int shorter = name.getBytes().length;
				if(conn.length<shorter) shorter = conn.length-1;
				for(int i = 0; i<shorter; i++) conn[i+1] = name.getBytes()[i];
				msgListArea.setText(msgListArea.getText() + "Connecting... \n");
				serverSocket.send(new DatagramPacket(conn, conn.length, serverAddress, serverPort));
			}
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void disconnect(){	//done when we chose to disconnect from a server

		try {
			byte[] pack = new byte[1];
			pack[0] = removeUser;
			DatagramSocket serverSocket;
			serverSocket = new DatagramSocket();
			serverSocket.send(new DatagramPacket(pack, pack.length, serverAddress, serverPort));
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		disconnection();
	}
	
	private void disconnection(){	//done when we disconnect from a server OR when a server stops running
		try {
			if(!serverAddress.equals(null)&& serverPort!=0){
				DatagramSocket serverSocket;
				serverSocket = new DatagramSocket();
				byte[] disconn = new byte[1];
				disconn[0] = removeUser;
				serverSocket.send(new DatagramPacket(disconn, disconn.length, serverAddress, serverPort));
				msgListArea.setText(msgListArea.getText() + "Disconnected from Server. \n");
				microphone.finish();
			}
			
			btnConnect.setEnabled(true);
			btnDisconnect.setEnabled(false);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		serverAddress = null;
		connected = false;
		otherUsers = new ArrayList<String>();
		updateUsers();
	}
		
	//this class deals with any packet received
	class getAudio extends Thread {
		public void run(){

			try {
				DatagramSocket serverSocket = new DatagramSocket(defaultClientPort);
				while(true){
					DatagramPacket p = new DatagramPacket(new byte[10000], 10000);
					serverSocket.receive(p);
					
					if((p.getData()[0] == voiceByte) && connected)		speakers.addToBuffer(p);		//received when another user on the server speaks

					else if((p.getData()[0] == newUser) && connected){									//received if a new user has connected to the server
						byte[] user = Arrays.copyOfRange(p.getData(), 1, p.getData().length);
						msgListArea.setText(msgListArea.getText() + "User Connected from Server: " + new String(user) + "\n");
						otherUsers.add(new String(user));
						updateUsers();
					}
					else if((p.getData()[0] == removeUser) && connected){								//received if another user has disconnected from the server
						byte[] user = Arrays.copyOfRange(p.getData(), 1, p.getData().length);
						if(otherUsers.contains(new String(user))){
							otherUsers.remove(new String(user));
							msgListArea.setText(msgListArea.getText() + "User Disconnected from Server: " + user + "\n");
							updateUsers();
						}
					}
					
					else if((p.getData()[0] == connSuccess)&&(p.getAddress().equals(serverAddress))){	//checking for a response from the server indicating we are connected
						connected = true;
						microphone.setServerAddress(serverAddress);
						microphone.setServerPort(serverPort);
						updateUsers();
						msgListArea.setText(msgListArea.getText() + "Connected to Server("+ serverAddress.getHostName() + ":" + serverPort + "). \n");
						if(!isPtt) microphone.start();
						
					}
					
					else if((p.getData()[0] == connEnd)&& connected){	//checking for a response from the server indicating we are connected
						connected = false;
						disconnection();
					}
					
					else{
						System.out.println("Unknown Packet Received...");
					}
				}
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
