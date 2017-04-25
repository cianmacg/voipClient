package voipClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
/*
 * This class will convert received data to sound and play through devices default speakers
 */

public class AudioOutput {
	private ArrayList<DatagramPacket> audioBuffer = new ArrayList<DatagramPacket>();
	private boolean playback = false;
	
	//adds received audio the audio buffer
	public void addToBuffer(DatagramPacket audio){
		audioBuffer.add(audio);
		if(!playback){
			playback = true;
			Thread playAudio = new Thread(new play());
			playAudio.start();
		}
	}
	
	//this class plays audio from the audio buffer
	class play extends Thread {
		
		public void run(){			
			try {
				
				AudioFormat format = new AudioFormat(16000.0f, 16, 2, true, false);
				DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
				SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info);
				AudioInputStream audioStream = null;
				audioLine.open(format);
				audioLine.start();
				
				while(playback){
					sleep(new Long(100));
					while(!audioBuffer.isEmpty()){
						InputStream audioData = new ByteArrayInputStream(Arrays.copyOfRange(audioBuffer.get(0).getData(), 1, audioBuffer.get(0).getData().length-1));
						audioStream = new AudioInputStream(audioData, format, audioBuffer.remove(0).getData().length-1 / format.getFrameSize());
						byte[] bytesBuffer = new byte[10000];
			            int bytesRead = -1;
			 
			            while ((bytesRead = audioStream.read(bytesBuffer)) != -1) {
			                audioLine.write(bytesBuffer, 0, bytesRead);
			            }
					}
					
					playback = false;
				}
				
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}
}
