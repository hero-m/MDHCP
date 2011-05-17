package mnet;

import java.io.BufferedReader    ;
import java.io.EOFException      ;
import java.io.FileInputStream   ;
import java.io.FileNotFoundException;
import java.io.FileOutputStream  ;
import java.io.IOException       ;
import java.io.InputStreamReader ;
import java.io.ObjectInputStream ;
import java.io.ObjectOutputStream;
import java.net.*        ;
import java.util.Arrays  ;
import java.util.Calendar;
import java.util.HashMap ;
import java.util.Random  ;
import java.util.Vector  ;
import java.awt.event.*  ;
import javax.swing.Timer ;
import mstructs.ByteArray;

import mnet.Lease;

public class DHCPServer implements Runnable{
	private HashMap<ByteArray, Lease> db;
	private Vector<ByteArray> ips;
	private HashMap<ByteArray, ReservedLease>reserved;
	private Random rand = new Random();
	private DatagramSocket socket;
	private HashMap<ByteArray, MessageGroup> messages;
	private final static int MAX_LEN = 2048;
	//private InetAddress dhcpServerIA;
	private byte[]  dhcpServerIP, gateway, dns, serverid,
					subnetmask, prefix;
	private long renewaltime, rebindingtime, leasetime;
	private boolean incrementalLease = false, verifyClients = false;
	private long  minRenewal, minRebinding, minLease;
	private Timer timer = new Timer(5 * 60 * 1000, new ActionListener(){
		public void actionPerformed(ActionEvent e){
			revokeExpiredLease();
		}
	});
	public DHCPServer() throws SocketException {// throws SocketException{
		try{
			socket = new DatagramSocket(67);
			System.out.println("DHCP Server Created.\n");
		}catch(SocketException e){
			System.out.println("\nAn error occurred creating socket on port 67.\n" + 
					"If running under linux, try using sudo.\n");
			throw e;
		}
		init();
	}
	public void setConfigurations(byte[] gateway, byte[] dns, long renewaltime,
			long rebindingtime, long leasetime, byte[] subnetmask, byte[] prefix,
			byte[] dhcpServerIP, boolean incrementalLease, boolean verifyClients){
		this.gateway = gateway; this.dns = dns; this.renewaltime = renewaltime;
		this.rebindingtime = rebindingtime; this.leasetime = leasetime;
		this.subnetmask = subnetmask; this.prefix = prefix; this.incrementalLease = incrementalLease;
		this.dhcpServerIP = dhcpServerIP; this.verifyClients = verifyClients;
		serverid = dhcpServerIP;
		
	}
	public void stopServer(){
		saveDB();
		System.out.println("server stopped.");
		socket.close();
	}
	
	private void init(){
		messages = new HashMap<ByteArray, MessageGroup>();
		db  = new HashMap<ByteArray, Lease>(255);
		ips = new  Vector<ByteArray>(255);
		reserved = new HashMap<ByteArray, ReservedLease>(255);
		timer.start();
		loadDB();
		minRenewal   = 5 * 1000;
		minRebinding = 2 * 60 * 1000;
		minLease     = 5 * 60 * 1000;
		doneInit = true;
	}
	private boolean doneInit = false;
	
	public void run(){
		this.start();
	}
	public void start(){
		if(!doneInit)
			init();
		doneInit = false;
		//catch (SocketException e) { error(e, null);}
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
			} catch (SocketException e) { //this is not an error; stop server was pushed
				timer.stop();
			} catch (    IOException e) { error(e, null); }
		}
	}
	private byte[] reserveNextAddress(byte[] hwAddress){
		synchronized(this){
			ByteArray hwArray = new ByteArray(hwAddress);
			if(db.containsKey(hwArray))
				return db.get(hwArray).getIP();
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
					reserved.put(ipArray, new ReservedLease(hwAddress, System.currentTimeMillis()));
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
	public Vector<ByteArray> getReserves(){
		return new Vector<ByteArray>(reserved.keySet());
	}
	public int getReserveCount(){
		return reserved.size();
	}
	private void bitmask(byte[] ip){
		byte[] temp = Arrays.copyOf(subnetmask, 4);
		for(int i = 0; i < temp.length; i++){
			temp[i] = (byte) ~temp[i];
			temp[i] = (byte) (ip[i] & temp[i]);
			  ip[i] = (byte) (temp[i] | (subnetmask[i] & prefix[i]));
		}
	}
	private boolean isReturning(byte[] ip, byte[] hwAddress){
		ByteArray hwArray = new ByteArray(hwAddress);
		ByteArray ipArray = new ByteArray(ip);
		if(ips.contains(ipArray)){
			if(db.containsKey(hwArray) && Arrays.equals(ip, db.get(hwArray).getIP())){
				return true;
			}
		}
		return false;
	}
	private boolean registerAddress(byte[] ip, byte[] hwAddress){
		ByteArray hwArray = new ByteArray(hwAddress);
		ByteArray ipArray = new ByteArray(ip);
		synchronized(this){
			if(ips.contains(ipArray)){
				if(db.containsKey(hwArray) && Arrays.equals(ip, db.get(hwArray).getIP())){
					if(incrementalLease){
						long prevLease = db.get(hwArray).getLeaseTime(),
							 prevRenewal = db.get(hwArray).getRenewalTime(),
							 prevRebinding = db.get(hwArray).getRebindingTime();
						long newLease = Math.min(prevLease * 2, leasetime),
							 newRenewal = Math.min(prevRenewal * 2, renewaltime),
							 newRebinding = Math.min(prevRebinding * 2, rebindingtime);
						db.remove(hwArray);
						db.put(hwArray, new Lease(ip, hwAddress, newLease, newRenewal, newRebinding));
					} else {
						db.remove(hwArray);
						db.put(hwArray, new Lease(ip, hwAddress, leasetime, renewaltime, rebindingtime));
					}
					return true;
				}
			}else if(reserved.containsKey(ipArray)
					&& Arrays.equals(reserved.get(ipArray).hwAddress, hwAddress)){
				reserved.remove(ipArray);
				ips.add(ipArray);
				if(incrementalLease)
					db.put(hwArray, new Lease(ip, hwAddress, minLease, minRenewal, minRebinding));
				else
					db.put(hwArray, new Lease(ip, hwAddress, leasetime, renewaltime, rebindingtime));
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
			int len = packet.getLength();
			byte[] data = java.util.Arrays.copyOf(temp, len);
			return data;
		}catch(IOException e){e.printStackTrace();}
		return null;
	}
	private void revokeExpiredLease(){
		synchronized(this){
			Vector<Lease> toBeRemoved = new Vector<Lease>();
			long now = System.currentTimeMillis();
			for(Lease lease : db.values()){
				long end = lease.getLeaseStart() + lease.getLeaseTime();
				if(end < now){
					toBeRemoved.add(lease);
				}
			}
			for(int i = 0; i < toBeRemoved.size(); i++){
				ips.remove(new ByteArray(toBeRemoved.elementAt(i).getIP()));
				 db.remove(new ByteArray(toBeRemoved.elementAt(i).getHwAddress()));
			}
			
			Vector<ByteArray> r = new Vector<ByteArray>();
			for(ByteArray rl : reserved.keySet()){
				if(now > reserved.get(rl).date + 600 * 1000)
					r.add(rl);
			}
			for(int i = 0; i < r.size(); i++){
				reserved.remove(r.elementAt(i));
			}
		}
	}
	private class ReplyThread implements Runnable{
		DatagramPacket requestPacket;
		boolean prev = false;
		Timer timer;
		Process process;
		public ReplyThread(DatagramPacket requestdp){
			requestPacket = requestdp;
		}
		@Override
		public void run() {
			int len = requestPacket.getLength();
			byte[] requestdata = java.util.Arrays.copyOf(requestPacket.getData(), len);
			DHCPPacket request = new DHCPPacket();
			request.read(requestdata);
			
			ByteArray xidArray = new ByteArray(request.getXid());
			if(messages.containsKey(xidArray)){
				messages.get(xidArray).add(request);

			}
			else{
				MessageGroup newGroup = new MessageGroup();
				newGroup.add(request);
				messages.put(xidArray, newGroup);

			}

			byte requestType = request.getOption((byte)53)[0];
			if(requestType == Constants.DHCPDISCOVER){
				byte[] reservedIP = reserveNextAddress(request.getChaddr());
				sendReply(request, Constants.DHCPOFFER, reservedIP);
			}else if(requestType == Constants.DHCPREQUEST){
				if(request.getOption((byte)54) != null && 
						!Arrays.equals(request.getOption((byte) 54), new byte[]{0,0,0,0}) &&
						!Arrays.equals(request.getOption((byte) 54), serverid))
					return;
				byte[] reservedIP = request.getOption((byte)50);
				if(reservedIP == null)
					reservedIP = request.getCiaddr();
				prev = isReturning(reservedIP, request.getChaddr());
				boolean success = registerAddress(reservedIP, request.getChaddr());
				System.out.println("Registering Address successful");
				if(success){
					sendReply(request, Constants.DHCPACK, reservedIP);
					if(verifyClients && System.getProperty("os.name").equals("Linux")){
						try { Thread.sleep(100); }
						catch (InterruptedException e) {error (e, null);}
						String cmd = "arping " + Helper.ipToString(reservedIP);
						try {
							process = Runtime.getRuntime().exec(cmd);
							BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
							boolean matched = false;
							String macStr = null;
							System.out.println("Running "+cmd);
							String str;
							timer = new Timer(5000, new ActionListener(){
								public void actionPerformed(ActionEvent e){
									System.out.println("ARPing thread killed.");
									process.destroy();
									timer.stop();
								}
							});
							timer.start();
							outer: while((str = br.readLine()) != null){
								String[] pieces = str.split("\\s");
								for(int i = 0; i < pieces.length; i++){
									System.out.print(pieces[i]+"*");
									if(pieces[i].matches("\\[\\p{XDigit}*:\\p{XDigit}*:\\p{XDigit}*:"+
															"\\p{XDigit}*:\\p{XDigit}*:\\p{XDigit}*\\]")){
										System.out.println("Item Found (" + pieces[i] + ").");
										macStr = pieces[i];
										matched = true;
										timer.stop();
										process.destroy();
										break outer;
									}
								}
							}
							if(!matched){
								ByteArray mac = Helper.stringToMac(macStr);
								ByteArray ip = new ByteArray(db.get(mac).getIP());
								ips.remove(ip);
								db.remove(mac);
							}else{
								ByteArray mac = Helper.stringToMac(macStr);
								ByteArray ip = new ByteArray(db.get(mac).getIP());
								ByteArray resIP = new ByteArray(reservedIP);
								if(!resIP.equals(ip)){
									ips.remove(ip);
									db.remove(mac);
								}
							}
						} catch (IOException e) { error(e, null); }
					}
				}
				else
					sendReply(request, Constants.DHCPNAK, reservedIP);
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
			}else if(requestType == Constants.DHCPINFORM){
				sendReply(request, Constants.DHCPACK, request.getCiaddr());
			}
			System.out.println("finished receiving");
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
				 .setSiaddr(dhcpServerIP)
				 .setGiaddr(0)
				 .setChaddr(request.getChaddr())
				 .setSname (0)
				 .setFile  (0)
				 .setCookie( );
			reply.addOption((byte)53, (byte)1, new byte[]{requestType})
				 .addOption((byte)1 , (byte)4, subnetmask   )
				 .addOption((byte)3 , (byte)4, gateway      )
				 .addOption((byte)6 , (byte)4, dns          )
				 .addOption((byte)54, (byte)4, serverid     );
			if(request.getOption((byte)53)[0] != 8){
				if(incrementalLease){
					if(!prev){
						try{
						reply.addOption((byte)58, (byte)4, minRenewal / 1000)
							 .addOption((byte)59, (byte)4, minRebinding / 1000)
							 .addOption((byte)51, (byte)4, minLease    / 1000);
						}catch(Exception e){ e.printStackTrace(); }
					}else{
						ByteArray hwArray = new ByteArray(request.getChaddr());
						reply.addOption((byte)58, (byte)4, db.get(hwArray).getRenewalTime() / 1000 )
							 .addOption((byte)59, (byte)4, db.get(hwArray).getRebindingTime()/ 1000)
							 .addOption((byte)51, (byte)4, db.get(hwArray).getLeaseTime()  / 1000 );
					}
				}else{
					 reply.addOption((byte)58, (byte)4, renewaltime / 1000 )
						  .addOption((byte)59, (byte)4, rebindingtime/ 1000)
						  .addOption((byte)51, (byte)4, leasetime   / 1000 );
				}
			}
			
			byte[] replydata = reply.array();
			ByteArray xidArray = new ByteArray(reply.getXid());
			if(messages.containsKey(xidArray))
				messages.get(xidArray).add(reply);
			else{
				MessageGroup newGroup = new MessageGroup();
				newGroup.add(reply);
				messages.put(xidArray, newGroup);
			}
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
	private void saveDB(){
		try{
			ObjectOutputStream objStream = new ObjectOutputStream(new FileOutputStream("db", false));
			for(ByteArray key : db.keySet()){
				objStream.writeObject(key);
				objStream.writeObject(db.get(key));
			}
			objStream.close();
		}catch(IOException e){ error(e, null); }
	}
	private void loadDB(){
		ObjectInputStream objStream = null;
		try{
			FileInputStream fileStream = new FileInputStream("db");
			objStream = new ObjectInputStream(fileStream);
			while(true){
				ByteArray key = (ByteArray)objStream.readObject();
				Lease value = (Lease)objStream.readObject();
				db.put(key, value);
				ips.add(new ByteArray(value.getIP()));
			}
		} catch (          EOFException e) { error(e, null); //no error, just end of file reached.
		} catch ( FileNotFoundException e) { error(e, null); //no error. create a new file on saveDB. 
		} catch (           IOException e) { error(e, null);
		} catch (ClassNotFoundException e) { error(e, null);
		} finally {
			try {if(objStream != null) objStream.close(); }
			catch (IOException e) { error(e, null); }
		}
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
		ReservedLease(byte[] hw, long d){
			hwAddress = hw;
			date = d;
		}
	}

	private void error(Exception e, String text){
		e.printStackTrace();
	}
	
	public class MessageGroup{
		private Vector<DHCPPacket> messages;
		public MessageGroup(){
			messages = new Vector<DHCPPacket>();
		}
		public void add(DHCPPacket m){
			messages.add(m);
		}
		public DHCPPacket getMessage(int index){
			return messages.get(index);
		}
		public int getSize(){
			return messages.size();
		}
	}
	
	public int getMessageCount(){
		int sum = 0;
		for(MessageGroup mg : messages.values())
			sum += mg.messages.size();
		return sum;
	}
	public HashMap<ByteArray, MessageGroup> getMessages(){
		return messages;
	}
}
