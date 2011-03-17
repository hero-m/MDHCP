package mnet;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.BitSet;
public class DHCPServer {
	
	private DatagramSocket socket;
	private DatagramPacket packet;
	private byte[] data;
	private String binaryStr, hexStr;
	public DHCPServer() throws SocketException{
		try{
			socket = new DatagramSocket(67);
			System.out.println("DHCP Server Created.\n" +
					   "Use capturePacket to capture an incoming request.");
		}catch(SocketException e){
			System.out.println("\nAn error occurred creating socket on port 67.\n" + 
					"If running under linux, try using sudo.\n");
			throw e;
		}
	}
	public void capturePacket(int packetLength){
		binaryStr = null;
		hexStr = null;
		data = new byte[packetLength];
		DatagramPacket packet = new DatagramPacket(data, packetLength);
		System.out.println("Listening started on port 67...");
		try{
			socket.receive(packet);
			System.out.println("Packet captured.");
		}catch(IOException e){e.printStackTrace();}
	}
	
	public String getBinary(){
		if(data == null) return null;
		if(binaryStr != null)
			return binaryStr;
		binaryStr = "";
		for(int i = 0; i < data.length; i++)
			binaryStr = binaryStr + Helper.getBinary(data[i]);
		return binaryStr;
	}
	public String getHex(){
		if(data == null) return null;
		if(hexStr != null)
			return hexStr;
		hexStr = "";
		for(int i = 0; i < data.length; i++)
			hexStr = hexStr + Helper.getHex(data[i]);
		return hexStr;
	}
	
	public byte[] getData(){
		return data;
	}
	public DatagramPacket getPacket(){
		return packet;
	}
}
