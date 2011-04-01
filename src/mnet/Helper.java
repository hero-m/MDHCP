package mnet;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Helper {
	public static byte[] getMacAddress(String hw, int len){
		try {
			byte[] mac = NetworkInterface.getByName(hw).getHardwareAddress();
			return Arrays.copyOf(mac, len);
		} catch (SocketException e) { e.printStackTrace(); }
		
		return null;
	}
	
	public static byte[] sendDiscoverPacket(String hw){
		DHCPPacket packet = new DHCPPacket();
		packet.setOp(Constants.BOOTREQUEST)
			  .setHtype((byte) 1)
			  .setHlen ((byte) 6)
			  .setHops ((byte) 0)
			  .generateXid()
			  .setSecs  (0)
			  .setBroadcastFlag(true)
			  .setCiaddr(0)
			  .setYiaddr(0)
			  .setSiaddr(0)
			  .setGiaddr(0)
			  .setChaddr(getMacAddress(hw, 16))
			  .setSname (0)
			  .setFile  (0);
			
		packet.addOption((byte)53, (byte)1, new byte[]{1});
		
		byte[] data = packet.array();
		
		try{	
			DatagramSocket socket   = new DatagramSocket();
			DatagramPacket dgpacket = new DatagramPacket(data, data.length);
			dgpacket.setPort(67);
			dgpacket.setAddress(InetAddress.getByAddress(new byte[]{(byte)255, (byte)255, (byte)255, (byte)255}));
			socket.setBroadcast(true);
			socket.send(dgpacket);
		}catch (SocketException e)      { e.printStackTrace();
		}catch (UnknownHostException e) { e.printStackTrace();
		}catch (IOException e)          { e.printStackTrace(); }
		
		return data;
	}
	public static void getPacketAsClient(String hw){
		sendDiscoverPacket(hw);
		try {
			DatagramSocket socket = new DatagramSocket(68);
			byte[] temp = new byte[500];
			DatagramPacket p = new DatagramPacket(temp, 500);
			System.out.println("Listening started on port 68...");
			socket.receive(p);
			System.out.println("Packet captured.");
			int len = p.getLength();
			byte[] data = java.util.Arrays.copyOf(temp, len);
			FileOutputStream file = new FileOutputStream("clientlog.txt");
			DHCPPacket packet = new DHCPPacket();
			packet.read(data);
			file.write(("Packet length: "+ packet.array().length + "\n").getBytes("US-ASCII"));
			file.write(ByteFactory.simpleFormatted(packet.array()).getBytes("US-ASCII"));
			file.close();
		} catch (SocketException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
	}
}
