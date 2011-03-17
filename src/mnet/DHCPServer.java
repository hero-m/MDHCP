package mnet;
import java.io.IOException;
import java.net.*;
public class DHCPServer {
	private static final int PACKET_LENGTH = 1, MINI_PACKET = 1;
	private DatagramSocket socket;
	public DHCPServer(){
		try{
			socket = new DatagramSocket(67);
		}catch(SocketException e){e.printStackTrace();}
	}
	public void listen(){
		byte[] data = new byte[PACKET_LENGTH];
		DatagramPacket packet = new DatagramPacket(data, PACKET_LENGTH);
		try {
			System.out.println("Listening Started.");
			System.out.println("packet size to expect: " + PACKET_LENGTH);
			socket.receive(packet);
			System.out.println(new String(data));
			System.out.println("Packet Received.");
		} catch (IOException e) { e.printStackTrace(); }
	}
	public void countListen(){
		byte[] data = new byte[MINI_PACKET];
		DatagramPacket packet = new DatagramPacket(data, MINI_PACKET);
		try {
			System.out.println("Listening Started.");
			int count = 0;
			while(true){
				count++;
				socket.receive(packet);
				System.out.println(count);
			}
		} catch (IOException e) { e.printStackTrace(); }
	}

}
