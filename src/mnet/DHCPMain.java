package mnet;

public class DHCPMain {
	public static void main(String[] args){
		System.out.println("DHCP-Server Started...");
		DHCPServer server = new DHCPServer();
		server.listen();
	}

}
