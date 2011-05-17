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
import java.util.Enumeration;
import java.util.Vector;

import mstructs.ByteArray;

public class Helper {
	public static byte[] getMacAddress(String hw, int len){
		try {
			byte[] mac = NetworkInterface.getByName(hw).getHardwareAddress();
			return Arrays.copyOf(mac, len);
		} catch (SocketException e) { e.printStackTrace(); }
		
		return null;
	}
	static byte[] mac;
	public static void randomize(){
		mac = new byte[16];
		for(int i = 0; i < 6; i++){
			mac[i] = (byte)(Math.random() * 256);
		}
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
			  .setChaddr(mac)
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
			socket.close();
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
			DHCPPacket packet = new DHCPPacket();
			packet.read(data);
			sendRequestPacket(packet);

			temp = new byte[500];
			p = new DatagramPacket(temp, 500);
			System.out.println("Listening started on port 68...");
			socket.receive(p);
			System.out.println("Packet captured.");
			len = p.getLength();
			data = java.util.Arrays.copyOf(temp, len);
			packet = new DHCPPacket();
			packet.read(data);
		} catch (SocketException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
	}
	public static void   sendRequestPacket(DHCPPacket offer){
		DHCPPacket packet = new DHCPPacket();
		packet.setOp(Constants.BOOTREQUEST)
			  .setHtype((byte) 1)
			  .setHlen ((byte) 6)
			  .setHops ((byte) 0)
			  .setXid(offer.getXid())
			  .setSecs  (0)
			  .setBroadcastFlag(true)
			  .setCiaddr(0)
			  .setYiaddr(0)
			  .setSiaddr(0)
			  .setGiaddr(0)
			  .setChaddr(mac)
			  .setSname (0)
			  .setFile  (0);
		
		packet.addOption((byte)53, (byte)1, new byte[]{3})
			  .addOption((byte)50, (byte)4, offer.getYiaddr());
		
		byte[] data = packet.array();
		
		try{	
			DatagramSocket socket   = new DatagramSocket();
			DatagramPacket dgpacket = new DatagramPacket(data, data.length);
			dgpacket.setPort(67);
			dgpacket.setAddress(InetAddress.getByAddress(new byte[]{(byte)255, (byte)255, (byte)255, (byte)255}));
			socket.setBroadcast(true);
			socket.send(dgpacket);
			socket.close();
		}catch (SocketException e)      { e.printStackTrace();
		}catch (UnknownHostException e) { e.printStackTrace();
		}catch (IOException e)          { e.printStackTrace(); }
		
		//return data;
	}
	public static void fill(){
		while(true){
			randomize();
			getPacketAsClient("eth0");
		}
	}
	public static Vector<InetAddress> getAvailableInetAddresses(){
		Vector<InetAddress> found = new Vector<InetAddress>();
		try {
			Enumeration<NetworkInterface> nis =
				NetworkInterface.getNetworkInterfaces();
			while(nis.hasMoreElements()){
				NetworkInterface ni = nis.nextElement();
				Enumeration<InetAddress> ias = ni.getInetAddresses();
				while(ias.hasMoreElements()){
					InetAddress ia = ias.nextElement();
					if(!ia.isLoopbackAddress() && ia.isSiteLocalAddress() && !found.contains(ia))
						found.add(ia);
				}
			}
		} catch (SocketException e) { e.printStackTrace(); }
		if(found.size() > 0)
			return found;
		else
			return null;
	}
	public static String ipToString(byte[] ip){
		if(ip.length != 4) return null;
		return ByteFactory.asInt(ip[0]) + "." + ByteFactory.asInt(ip[1]) + "." +
			   ByteFactory.asInt(ip[2]) + "." + ByteFactory.asInt(ip[3]);
	}
	public static String xidToString(byte[] xid){
		String xidStr = "0x";
		for(int i = 0; i < xid.length; i++)
			xidStr = xidStr + ByteFactory.getHex(xid[i]);
		return xidStr;
	}
	public static String secsToString(byte[] secs){
		return ByteFactory.getBytesAsInt(secs)+"";
	}
	public static String flagsToString(byte[] flags){
		return ByteFactory.getBinary(flags);
	}
	public static String chaddrToString(byte[] chaddr){
		return ByteFactory.getHex(chaddr);
	}
	public static String cookieToString(byte[] cookie){
		return ByteFactory.getHex(cookie);
	}
	public static ByteArray stringToMac(String str){
		byte[] mac = new byte[16];
		String[] parts = str.split(":");
		for(int i = 0; i < 6; i++){
			mac[i] = (byte) Integer.parseInt(parts[i], 16);
		}
		return new ByteArray(mac);
	}
}
