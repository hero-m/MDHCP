
import mgui.DHCPFrame;
import mnet.Helper;

public class DHCPMain {
	public static void main(String[] args){
		//Helper.fill();
		/*try{
			byte[] data = Helper.sendDiscoverPacket("eth0");
			FileOutputStream file = new FileOutputStream("log.txt");
			file.write(ByteFactory.simpleFormatted(data).getBytes("US-ASCII"));
			file.flush();
		}catch (IOException e) { e.printStackTrace(); } /**/
		/*try{
			DHCPServer server = new DHCPServer();
			byte[] data = server.capturePacket();
			FileOutputStream file = new FileOutputStream("log.txt");
			DHCPPacket packet = new DHCPPacket();
			packet.read(data);
			file.write(("Packet length: "+ packet.array().length + "\n").getBytes("US-ASCII"));
			file.write(ByteFactory.simpleFormatted(packet.array()).getBytes("US-ASCII"));
			file.close();
		}catch(SocketException e){e.printStackTrace();
		}catch(IOException     e){e.printStackTrace();}/**/
		/*Helper.getPacketAsClient("eth0");/**/
		/*try {
			DHCPServer server = new DHCPServer(InetAddress.getByAddress(new byte[]{(byte)192, (byte)168, (byte)1, (byte)1}));
			server.start();
		} catch (SocketException e) { e.printStackTrace();
		} catch (UnknownHostException e) { e.printStackTrace(); } */
		System.out.println("*"+System.getProperty("os.name")+"*");
		new DHCPFrame();
/*		DHCPServer server = frame.getDHCPServer();
		server.setConfigurations(
				config.getAsBytes("gateway", "\\."),
				config.getAsBytes("dns", "\\."),
				config.getAsBytes("renewal", " "),
				config.getAsBytes("rebinding", " "),
				config.getAsBytes("lease", " "),
				config.getAsBytes("subnet-mask", "\\."));*/
	}
}