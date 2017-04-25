package voipClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
/*
 * This class will capture audio from the devices default microphone and sent the data to a server
 */
public class AudioInput {
	
	private AudioFormat format = new AudioFormat(16000.0f,
												16,
												2,
												true,
												false);
	private TargetDataLine line;
	private InetAddress serverAddress = null;
	private int serverPort = 8090;
	private DatagramSocket serverSocket;
	private boolean captureAudio = false;
	private boolean connected = false;
	
	public AudioInput(){
		System.out.println("Started...");
		try {
			serverAddress = InetAddress.getByName("86.45.47.50");
			serverSocket  = new DatagramSocket();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//gains access to the devices default microphone
	public void setup() {
	    try {
	        
	        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
	
	        // checks if system supports the data line
	        if (!AudioSystem.isLineSupported(info)) {
	            System.out.println("Line not supported");
	            System.exit(0);
	        }
	        line = (TargetDataLine) AudioSystem.getLine(info);
	        line.open(format);
	        line.start();   // start capturing
	
	        System.out.println("Microphone ready.");

	        	    	        
	    } catch (LineUnavailableException ex) {
	        ex.printStackTrace();
	    }
	}
	
	//begins capturing audio
	public void start(){
		if(!captureAudio){
			captureAudio = true; 
			Thread sendAudio = new Thread(new send());
			sendAudio.start();
		}
	}
	
	//finishes capturing audio
	public void finish() {
		captureAudio=false;
	}
	
	//sends captured audio to specified address (server)
	class send extends Thread {
		
		byte[] os = new byte[10000];
		
		public void run(){
			try {
				if(!serverAddress.equals(null)){
					while(captureAudio){
						System.out.println("here");
				        line.read(os, 0, os.length);
						byte packetType = Byte.parseByte("00000001", 2);
						
						byte[] bytes = new byte[os.length+1];
						bytes[0] = packetType;
						for(int i = 0; i<os.length; i++){
							bytes[i+1] = os[i];
						}
						
						DatagramPacket p = new DatagramPacket(bytes, bytes.length);
						p.setAddress(serverAddress);
						p.setPort(serverPort);
						
						serverSocket.send(p);
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
	
	public void setServerAddress(InetAddress s){
		this.serverAddress= s ;
	}
	
	public void setServerPort(int i){
		this.serverPort = i;
	}
	
	public void disconnect(){
		this.serverAddress = null;
		this.serverPort = 8090;
		
	}
}
