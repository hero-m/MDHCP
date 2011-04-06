/* todo: dhcprelease
 * todo: refresh
 * */
package mnet;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Vector;
public class DHCPServer{

	private HashMap<byte[], Lease> db;
	private Vector<Integer> ips;
	private HashMap<Integer, byte[]>reserved;
	private DatagramSocket socket;
	private final static int MAX_LEN = 2048;
	private InetAddress dhcpServerIA;
	private byte[] dhcpServerIP, gateway, dns, serverid,
					renewaltime, rebindingtime, leasetime,
					subnetmask;
	public DHCPServer() throws SocketException{
		Vector<InetAddress> ias = Helper.getAvailableInetAddresses();
		dhcpServerIA = ias.firstElement();
		if(ias.size() > 1)
			System.out.println("Multiple IP Addresses detected. Using " + 
								Helper.convertIPtoString(dhcpServerIA.getAddress()));
		else if(ias.size() == 0)
			throw new UnsupportedOperationException("Error: No IP Addresses detected.");
		init();
	}
	public DHCPServer(InetAddress ia) throws SocketException{
		this.dhcpServerIA = ia;
		init();
	}
	private void init() throws SocketException{
		db  = new HashMap<byte[], Lease>(255);
		ips = new  Vector<Integer>(255);
		reserved = new HashMap<Integer, byte[]>(255);
		dhcpServerIP  = dhcpServerIA.getAddress();
		serverid      = dhcpServerIP;
		gateway       = dhcpServerIP;
		dns           = dhcpServerIP;
		renewaltime   = new byte[]{(byte)0  , (byte)1  , (byte)0xfa, (byte)0x40};
		rebindingtime = new byte[]{(byte)0  , (byte)3  , (byte)0x75, (byte)0xf0};
		leasetime     = new byte[]{(byte)0  , (byte)3  , (byte)0xf4, (byte)0x80};
		subnetmask    = new byte[]{(byte)255, (byte)255, (byte)255 , (byte)0   };
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
	
	public void start(){
		System.out.println("Server started listening on port 67...");
		while(true){
			byte[] temp = new byte[MAX_LEN];
			DatagramPacket packet = new DatagramPacket(temp, MAX_LEN);
			try {
				socket.receive(packet);
				ReplyThread reply  = new ReplyThread(packet);
				Thread replyThread = new Thread(reply);
				replyThread.run();
				System.out.println("Packet received at " + Calendar.getInstance().getTime().toString() + ".");
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	private byte[] reserveNextAddress(byte[] hwAddress){
		synchronized(this){
			if(db.containsKey(hwAddress))
				return db.get(hwAddress).getIP();
			if(ips.size() + reserved.size() >= 254){
				//refresh();
				if(ips.size() + reserved.size() >= 254)
					return null;
			}
			for(int i = 2; i < 255; i++){
				if(!ips.contains(i) && !reserved.containsValue(i)){
					byte[] newip = new byte[] {dhcpServerIP[0], dhcpServerIP[1], dhcpServerIP[2], (byte)i};
					reserved.put(i, hwAddress);
					System.out.println("FTail: " + i);
					return newip;
				}
			}
		}
		return null;
	}
	private boolean registerAddress(byte[] ip, byte[] hwAddress){
		synchronized(this){
			int tail = (int)(ip[3]);
			System.out.println("RTail: " + tail);
			System.out.println(reserved.get(tail));
			System.out.println(hwAddress);
			if(ips.contains(tail)){
				if(db.containsKey(hwAddress) && Arrays.equals(ip, db.get(hwAddress).getIP())){
					db.remove(hwAddress);
					db.put(hwAddress, new Lease(ip, hwAddress, leasetime));
					return true;
				}
			}else if(reserved.containsKey(tail) && Arrays.equals(reserved.get(tail), hwAddress)){
				//reserved.remove(hwAddress);
				reserved.remove(tail);
				ips.add(tail);
				db.put(hwAddress, new Lease(ip, hwAddress, leasetime));
				return true;
			}
			return false;
		}
	}
	
	public byte[] capturePacket(){
		byte[] temp = new byte[MAX_LEN];
		DatagramPacket packet = new DatagramPacket(temp, MAX_LEN);
		System.out.println("Listening started on port 67...");
		try{
			socket.receive(packet);
			System.out.println("Packet captured.");
			int len = packet.getLength();
			byte[] data = java.util.Arrays.copyOf(temp, len);
			return data;
		}catch(IOException e){e.printStackTrace();}
		return null;
	}
	
	private class ReplyThread implements Runnable{
		DatagramPacket requestPacket;
		public ReplyThread(DatagramPacket requestdp){
			requestPacket = requestdp;
		}
		@Override
		public void run() {
			int len = requestPacket.getLength();
			byte[] requestdata = java.util.Arrays.copyOf(requestPacket.getData(), len);
			DHCPPacket request = new DHCPPacket();
			request.read(requestdata);
			byte requestType = request.getOption((byte)53)[0];
			if(requestType == Constants.DHCPDISCOVER){
				byte[] reservedIP = reserveNextAddress(request.getChaddr());
				sendReply(request, Constants.DHCPOFFER, reservedIP);
			}else if(requestType == Constants.DHCPREQUEST){
				byte[] reservedIP = request.getOption((byte)50); 
				boolean success = registerAddress(reservedIP, request.getChaddr());
				if(success)
					sendReply(request, Constants.DHCPACK, reservedIP);
				else
					sendReply(request, Constants.DHCPNACK, reservedIP);
			}
		}
		public void sendReply(DHCPPacket request, byte requestType, byte[] reservedIP){
			DHCPPacket reply = new DHCPPacket();
			reply.setOp(Constants.BOOTREQUEST)
				 .setHtype ((byte) 1)
				 .setHlen  ((byte) 6)
				 .setHops  ((byte) 0)
				 .setXid   (request.getXid())
				 .setSecs  (0)
				 .setBroadcastFlag(true)
				 .setCiaddr(0)
				 .setYiaddr(reservedIP)
				 .setSiaddr(dhcpServerIA.getAddress())
				 .setGiaddr(0)
				 .setChaddr(request.getChaddr())
				 .setSname (0)
				 .setFile  (0);
			
			reply.addOption((byte)53, (byte)1, new byte[]{requestType})
				 .addOption((byte)1 , (byte)4, subnetmask   )
				 .addOption((byte)3 , (byte)4, gateway      )
				 .addOption((byte)6 , (byte)4, dns          )
				 .addOption((byte)58, (byte)4, renewaltime  )
				 .addOption((byte)59, (byte)4, rebindingtime)
				 .addOption((byte)51, (byte)4, leasetime    )
				 .addOption((byte)54, (byte)4, serverid     );
			   //.addOption((byte)12, (byte)n, new byte[]{(byte)'y', 'our-pc' ...}) // client host name
			
			byte[] replydata = reply.array();
			DatagramPacket replyPacket = new DatagramPacket(replydata, replydata.length);
			
			DatagramSocket socket;
			try {
				socket = new DatagramSocket();
				replyPacket.setPort(68);
				replyPacket.setAddress(InetAddress.getByAddress(new byte[]{(byte)255, (byte)255, (byte)255, (byte)255}));
				socket.setBroadcast(true);
				socket.send(replyPacket);
			} catch (SocketException      e) { e.printStackTrace();
			} catch (UnknownHostException e) { e.printStackTrace();
			} catch (IOException          e) { e.printStackTrace(); }
		}
	}
}
