package mnet;

import java.io.IOException;
import java.net.*         ;
import java.util.Arrays   ;
import java.util.Calendar ;
import java.util.Date     ;
import java.util.HashMap  ;
import java.util.Vector   ;
import java.awt.event.*   ;
import javax.swing.Timer  ;
public class DHCPServer implements Runnable{
	private HashMap<ByteArray, Lease> db;
	private Vector<Integer> ips;
	private HashMap<Integer, ReservedLease>reserved;
	private DatagramSocket socket;
	private final static int MAX_LEN = 2048;
	private InetAddress dhcpServerIA;
	private byte[] dhcpServerIP, gateway, dns, serverid,
					renewaltime, rebindingtime, leasetime,
					subnetmask;
	private Timer timer = new Timer(5 * 60 * 1000, new ActionListener(){
		public void actionPerformed(ActionEvent e){
			revokeExpiredLease();
		}
	});
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
		db  = new HashMap<ByteArray, Lease>(255);
		ips = new  Vector<Integer>(255);
		reserved = new HashMap<Integer, ReservedLease>(255);
		dhcpServerIP  = dhcpServerIA.getAddress();
		serverid      = dhcpServerIP;
		gateway       = dhcpServerIP;
		dns           = dhcpServerIP;
		renewaltime   = new byte[]{(byte)0  , (byte)1  , (byte)0xfa, (byte)0x40};
		rebindingtime = new byte[]{(byte)0  , (byte)3  , (byte)0x75, (byte)0xf0};
		leasetime     = new byte[]{(byte)0  , (byte)3  , (byte)0xf4, (byte)0x80};
		subnetmask    = new byte[]{(byte)255, (byte)255, (byte)255 , (byte)0   };
		timer.start();
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
	public void run(){
		this.start();
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
			ByteArray hwArray = new ByteArray(hwAddress);
			if(db.containsKey(hwArray))
				return db.get(hwArray).getIP();
			if(ips.size() + reserved.size() >= 254){
				revokeExpiredLease();
				if(ips.size() + reserved.size() >= 254)
					return null;
			}
			for(int i = 2; i < 255; i++){
				if(!ips.contains(i) && !reserved.containsValue(i)){
					byte[] newip = new byte[] {dhcpServerIP[0], dhcpServerIP[1], dhcpServerIP[2], (byte)i};
					reserved.put(i, new ReservedLease(hwAddress, Calendar.getInstance().getTime()));
					return newip;
				}
			}
		}
		return null;
	}
	private boolean registerAddress(byte[] ip, byte[] hwAddress){
		ByteArray hwArray = new ByteArray(hwAddress);
		synchronized(this){
			int tail = (int)(ip[3]);
			System.out.print("IP: ");
			for(int i = 0; i < ip.length; i++)
				System.out.print(ip[i] + " ");
			System.out.println();
			if(ips.contains(tail)){
				System.out.print("DB IP: ");
				
				if(db.containsKey(hwArray) && Arrays.equals(ip, db.get(hwArray).getIP())){
					db.remove(hwArray);
					db.put(hwArray, new Lease(ip, hwAddress, leasetime));
					return true;
				}
			}else if(reserved.containsKey(tail) && Arrays.equals(reserved.get(tail).hwAddress, hwAddress)){
				reserved.remove(tail);
				ips.add(tail);
				db.put(hwArray, new Lease(ip, hwAddress, leasetime));
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
	private void revokeExpiredLease(){
		synchronized(this){
			Vector<Lease> toBeRemoved = new Vector<Lease>();
			Date now = Calendar.getInstance().getTime();
			for(Lease lease : db.values()){
				Date end = add(lease.getLeaseStart(), lease.getLeaseTime());
				if(end.before(now)){
					toBeRemoved.add(lease);
				}
			}
			for(int i = 0; i < toBeRemoved.size(); i++){
				ips.remove((Object)((int)toBeRemoved.elementAt(i).getIP()[3]));
				 db.remove(new ByteArray(toBeRemoved.elementAt(i).getHwAddress()));
			}
			
			long now_seconds = now.getTime();
			Vector<Integer> r = new Vector<Integer>();
			for(int rl : reserved.keySet()){
				if(now_seconds > reserved.get(rl).date + 600 * 1000)
					r.add(rl);
			}
			for(int i = 0; i < r.size(); i++){
				reserved.remove(r.elementAt(i));
			}
		}
	}
	private Date add(Date first, Date other){
		Calendar c = Calendar.getInstance();
		c.setTime(first);
		long firsttime = c.getTimeInMillis();
		c.setTime(other);
		long othertime = c.getTimeInMillis();
		c.setTimeInMillis(firsttime + othertime);
		return c.getTime();
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
			}else if(requestType == Constants.DHCPRELEASE){
				synchronized(this){
					byte[] hwAddress = request.getChaddr();
					Lease lease = db.get(new ByteArray(hwAddress));
					if(lease != null){
						byte[] ip = lease.getIP();
						ips.remove((Object)((int)ip[3]));
						db.remove(new ByteArray(hwAddress));
					}
				}
			}
		}
		public void sendReply(DHCPPacket request, byte requestType, byte[] reservedIP){
			DHCPPacket reply = new DHCPPacket();
			reply.setOp(Constants.BOOTREPLY)
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
			} catch (     SocketException e) { e.printStackTrace();
			} catch (UnknownHostException e) { e.printStackTrace();
			} catch (         IOException e) {
				System.out.println("Error Sending Reply. Are you still connected?");
			}
		}
	}
	private class ReservedLease{
		byte[] hwAddress;
		long date;
		ReservedLease(byte[] hw, Date d){
			hwAddress = hw;
			date = d.getTime();
		}
	}
	private class ByteArray{
		byte[] data;
		ByteArray(byte[] d){
			data = d;
		}
		@Override
		public boolean equals(Object obj){
			if(!(obj instanceof ByteArray))
				return false;
			ByteArray other = (ByteArray) obj;
			for(int i = 0; i < other.data.length; i++){
				if(data[i] != other.data[i]) return false;
			}
			return true;
		}
		@Override
		public int hashCode(){
			int hash = data[0];
			for(int i = 1; i < data.length; i++){
				hash = Integer.rotateLeft(hash, 8);
				hash ^= data[i];
			}
			return hash;
		}
		
	}
}
