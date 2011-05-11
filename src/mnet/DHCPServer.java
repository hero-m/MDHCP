package mnet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*         ;
import java.util.Arrays   ;
import java.util.Calendar ;
import java.util.Date     ;
import java.util.HashMap  ;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector   ;
import java.awt.event.*   ;
import javax.swing.Timer  ;
import mstructs.ByteArray;

import mnet.Lease;

public class DHCPServer implements Runnable{
	private HashMap<ByteArray, Lease> db;
	private Vector<ByteArray> ips;
	private HashMap<ByteArray, ReservedLease>reserved;
	private Random rand = new Random();
	private DatagramSocket socket;
	private final static int MAX_LEN = 2048;
	private InetAddress dhcpServerIA;
	private byte[] dhcpServerIP, gateway, dns, serverid,
					renewaltime, rebindingtime, leasetime,
					subnetmask, prefix;
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
	public void setConfigurations(byte[] gateway, byte[] dns, byte[] renewaltime,
			byte[] rebindingtime, byte[] leasetime, byte[] subnetmask, byte[] prefix){
		this.gateway = gateway; this.dns = dns; this.renewaltime = renewaltime;
		this.rebindingtime = rebindingtime; this.leasetime = leasetime;
		this.subnetmask = subnetmask; this.prefix = prefix;
		
	}
	public void stopServer(){
		saveDB();
		System.out.println("server stopped.");
		socket.close();
	}
	
	private void init() throws SocketException{
		db  = new HashMap<ByteArray, Lease>(255);
		ips = new  Vector<ByteArray>(255);
		reserved = new HashMap<ByteArray, ReservedLease>(255);
		//TODO: dhcpServerIP  = dhcpServerIA.getAddress();
		serverid = new byte[]{(byte)192, (byte)168, 0, 1};
		//serverid      = dhcpServerIP;
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
		loadDB();
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
				if(socket.isClosed()) return;
				socket.receive(packet);
				ReplyThread reply  = new ReplyThread(packet);
				Thread replyThread = new Thread(reply);
				replyThread.run();
				System.out.println("Packet received at " + Calendar.getInstance().getTime().toString() + ".");
			} catch(SocketException e){ //this is not an error; stop server was pushed
			} catch (IOException e) { error(e, null); }
		}
	}
	private byte[] reserveNextAddress(byte[] hwAddress){
		synchronized(this){
			ByteArray hwArray = new ByteArray(hwAddress);
			if(db.containsKey(hwArray))
				return db.get(hwArray).getIP();
			/*if(ips.size() + reserved.size() >= 254){
				revokeExpiredLease();
				if(ips.size() + reserved.size() >= 254)
					return null;
			}*/ //check number of clients;
			byte[] next = new byte[4];
			int attempts = 0;
			while(attempts < 1000){
				attempts++;
				if(attempts == 1000)
					return null;
				rand.nextBytes(next);
				bitmask(next);
				ByteArray ipArray = new ByteArray(next);
				if(!ips.contains(ipArray) && !reserved.containsValue(ipArray)){
					reserved.put(ipArray, new ReservedLease(hwAddress, Calendar.getInstance().getTime()));
					return next;
				}
			}
		}
		return null;
	}
	public Vector<ByteArray> getIPs(){
		return ips;
	}
	public int getIPCount(){
		return ips.size();
	}
	private void bitmask(byte[] ip){
		byte[] temp = Arrays.copyOf(subnetmask, 4);
		for(int i = 0; i < temp.length; i++){
			temp[i] = (byte) ~temp[i];
			temp[i] = (byte) (ip[i] & temp[i]);
			  ip[i] = (byte) (temp[i] | (subnetmask[i] & prefix[i]));
		}
	}
	private boolean registerAddress(byte[] ip, byte[] hwAddress){
		ByteArray hwArray = new ByteArray(hwAddress);
		ByteArray ipArray = new ByteArray(ip);
		synchronized(this){
			if(ips.contains(ipArray)){
				if(db.containsKey(hwArray) && Arrays.equals(ip, db.get(hwArray).getIP())){
					db.remove(hwArray);
					db.put(hwArray, new Lease(ip, hwAddress, leasetime));
					return true;
				}
			}else if(reserved.containsKey(ipArray)
					&& Arrays.equals(reserved.get(ipArray).hwAddress, hwAddress)){
				reserved.remove(ipArray);
				ips.add(ipArray);
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
				ips.remove(new ByteArray(toBeRemoved.elementAt(i).getIP()));
				 db.remove(new ByteArray(toBeRemoved.elementAt(i).getHwAddress()));
			}
			
			long now_seconds = now.getTime();
			Vector<ByteArray> r = new Vector<ByteArray>();
			for(ByteArray rl : reserved.keySet()){
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
			}else if(requestType == Constants.DHCPREQUEST){
				if(!Arrays.equals(request.getOption((byte) 54), serverid))
					return;
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
						ips.remove(new ByteArray(ip));
						db.remove(new ByteArray(hwAddress));
					}
				}
			}
			//TODO: Decline & Inform.
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
			} catch (     SocketException e) { error(e, null);
			} catch (UnknownHostException e) { error(e, null);
			} catch (         IOException e) {
				System.out.println("Error Sending Reply. Are you still connected?");
			}
		}
	}
	//TODO: siaddr 192.168.1.1 ?????
	private void saveDB(){
		try{
			ObjectOutputStream objStream = new ObjectOutputStream(new FileOutputStream("db", false));
			for(Entry<ByteArray, mnet.Lease> lease : db.entrySet()){
				objStream.writeObject(lease);
			}
			objStream.close();
		}catch(IOException e){ error(e, null); }
	}
	private void loadDB(){
		try{
			ObjectInputStream objStream = new ObjectInputStream(new FileInputStream("db"));
			while(objStream.available() > 0){
				Entry<ByteArray, mnet.Lease> lease;
				lease = (Entry<ByteArray, mnet.Lease>)objStream.readObject();
				db.put(lease.getKey(), lease.getValue());
			}
			objStream.close();
		} catch ( FileNotFoundException e) { // no error. create a new file on saveDB. 
		} catch (           IOException e) { error(e, null);
		} catch (ClassNotFoundException e) { error(e, null); }
	}
	public void clearDB(){
		synchronized(this){
			db.clear();
			ips.clear();
			reserved.clear();
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

	private void error(Exception e, String text){
		e.printStackTrace();
	}
}
