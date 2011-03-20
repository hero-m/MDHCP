package mnet;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Helper {
	public static byte[] getMacAddress(int len){
		InetAddress ip;
		try {
				ip = InetAddress.getLocalHost();
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
		
			byte[] mac = network.getHardwareAddress();
			return Arrays.copyOf(mac, len);
		} catch (UnknownHostException e) { e.printStackTrace();
		} catch (SocketException e) { e.printStackTrace(); }	
		
		return null;
	}
	
	public static byte[] sendDiscoverPacket(){
		DHCPPacket packet = new DHCPPacket();
		packet.setOp(Constants.BOOTREQUEST)
			  .setHtype((byte) 1)
			  .setHlen ((byte) 6)
			  .setHops ((byte) 0)
			  .generateXid()
			  .setSecs (0)
			  .setBroadcastFlag(true)
			  .setCiaddr(0)
			  .setYiaddr(0)
			  .setSiaddr(0)
			  .setGiaddr(0)
			  .setChaddr(getMacAddress(16))
			  .setSname(0)
			  .setFile(0);
			
		packet.addOption((byte)53, (byte)1, new byte[]{1});
		
		byte[] data = packet.array();
		
		try{	
			DatagramSocket socket = new DatagramSocket();
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
}
