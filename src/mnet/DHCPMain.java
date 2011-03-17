package mnet;

import java.net.SocketException;

public class DHCPMain {
	public static void main(String[] args){
		try{
			DHCPServer server = new DHCPServer();
			server.capturePacket(300);
			System.out.println("Packet content in binary: ");
			System.out.println(server.getBinary());
			System.out.println("===========================\n" +
					"Packet Content in Hex: ");
			System.out.println(server.getHex());
		}catch(SocketException e){e.printStackTrace();};
	}

}
