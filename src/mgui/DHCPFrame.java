package mgui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Vector;

import javax.swing.*;

import mio.MappingFile;
import mnet.ByteFactory;
import mnet.DHCPServer;
import mstructs.ByteArray;
public class DHCPFrame extends JFrame implements ActionListener, WindowListener{
	private static final long serialVersionUID = 129578221875757459L;
	DHCPServer dhcpServer;
	private MappingFile config;
	
	private JMenuItem startItem;

	private IPAddressField gatewayText, dnsText, subnetText, prefixText;
	private     JTextField renewalText, rebindingText, leaseText;
	JTree ipTree;
	public DHCPFrame(){
		loadConfigurations();
		
		ipTree = new JTree(new String[]{"Allocated IP Addresses", "Reserved IP Addresses"});
		JTabbedPane tabbed = new JTabbedPane(JTabbedPane.LEFT);
		JPanel configPanel, infoPanel;
		infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		infoPanel.add(ipTree, BorderLayout.WEST);
		infoPanel.setName("Information");
		
		/* Configuration Panel Creation */
		
		  renewalText = new JTextField(15);
		rebindingText = new JTextField(15);
		    leaseText = new JTextField(15);
		
		gatewayText = new IPAddressField(15);    dnsText = new IPAddressField(15);
		 subnetText = new IPAddressField(15); prefixText = new IPAddressField(15);
		
		initTextBoxes();
		
		JLabel    gatewayLabel = new JLabel("Gateway:")       ,       dnsLabel = new JLabel("DNS:"           ),
			       subnetLabel = new JLabel("Subnet:" )       ,    prefixLabel = new JLabel("Prefix:"        ),
		          renewalLabel = new JLabel("Renewal Time:")  , rebindingLabel = new JLabel("Rebinding Time:"),
		            leaseLabel = new JLabel("Lease Time:"  )  ;
		
		JButton restartButton = new JButton("Restart Server"), applyButton = new JButton("Apply Changes"),
				   saveButton = new JButton("Save Config"   );
		restartButton.addActionListener(this); applyButton.addActionListener(this);
		   saveButton.addActionListener(this);
		JPanel buttonSubPanel = new JPanel();
		buttonSubPanel.add(restartButton); buttonSubPanel.add(applyButton);
		buttonSubPanel.add(   saveButton);
		configPanel = new JPanel();
		configPanel.setName("Configurations");
		GridBagLayout gbLayout = new GridBagLayout();
		GridBagConstraints  constraintsL = new GridBagConstraints(),  constraintsR = new GridBagConstraints();
		GridBagConstraints constraintsM1 = new GridBagConstraints(), constraintsM2 = new GridBagConstraints();
		constraintsL.gridx = 0; constraintsM1.gridx = 1; constraintsM2.gridx = 2; constraintsR.gridx = 3;
		constraintsL.anchor  = GridBagConstraints.EAST;
		constraintsM2.anchor = GridBagConstraints.EAST;
		constraintsM2.insets.left = 15;
		constraintsR.fill  = GridBagConstraints.HORIZONTAL;
		constraintsM1.fill = GridBagConstraints.HORIZONTAL;
		configPanel.setLayout(gbLayout);
		configPanel.add(  gatewayLabel, constraintsL ); configPanel.add(  gatewayText, constraintsM1);
		configPanel.add(   subnetLabel, constraintsM2); configPanel.add(   subnetText, constraintsR );
		configPanel.add(      dnsLabel, constraintsL ); configPanel.add(      dnsText, constraintsM1);
		configPanel.add(   prefixLabel, constraintsM2); configPanel.add(   prefixText, constraintsR );
		configPanel.add(  renewalLabel, constraintsL ); configPanel.add(  renewalText, constraintsM1);
		configPanel.add(rebindingLabel, constraintsM2); configPanel.add(rebindingText, constraintsR );
		configPanel.add(    leaseLabel, constraintsL ); configPanel.add(    leaseText, constraintsM1);
		GridBagConstraints constraintsBP = new GridBagConstraints();
		constraintsBP.gridx = 0; constraintsBP.gridwidth = 4; constraintsBP.insets.top = 50;
		configPanel.add(buttonSubPanel, constraintsBP);
		
		/* end of Configuration Panel Creation */

		/* creation of the menubar */
		JMenuBar  menubar   = new JMenuBar();
		JMenu     menu      = new JMenu("Server");
		startItem = new JMenuItem("Start Server");
		startItem.addActionListener(this);
		JMenuItem clearButton = new JMenuItem("Clear Database");
		clearButton.addActionListener(this);
		menu.add(startItem);
		JMenuItem exitItem  = new JMenuItem("Exit");
		exitItem.addActionListener(this);
		menu.add(exitItem);
		menubar.add(menu);
		this.setJMenuBar(menubar);
		/* end of menubar creation */
		this.addWindowListener(this);
		tabbed.add(infoPanel, 0);
		tabbed.add(configPanel, 1);
		this.getContentPane().add(tabbed);
		this.setMinimumSize(new Dimension(750, 300));
		this.setSize(900, 600);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}
	private int state = 0;
	//TODO: db save
	//TODO: guess os (chatter)
	//TODO: show ips in gui
	public void actionPerformed(ActionEvent event){
		if(event.getActionCommand().equals("Start Server")){
			startDHCPServer();
		}else if(event.getActionCommand().equals("Clear Database")){
			dhcpServer.clearDB();
		}else if(event.getActionCommand().equals("Stop Server")){
			dhcpServer.stopServer();
			startItem.setText("Start Server");
		}else if(event.getActionCommand().equals("Exit")){
			if(dhcpServer != null){
				dhcpServer.stopServer();
			}
			this.dispose();
		}else if(event.getActionCommand().equals("Save Config")){
			saveConfigurations();
		}else if(event.getActionCommand().equals("Restart Server")){
			if(state == 1){
				dhcpServer.stopServer();
				dhcpServer.clearDB();
			}
			startDHCPServer();
		}
	}
	private int ipCount = 0;
	Vector<ByteArray> ips;
	private Timer ipTimer = new Timer(200, new ActionListener(){
		public void actionPerformed(ActionEvent evt){
			if(ipCount != dhcpServer.getIPCount()){
				ips = dhcpServer.getIPs();
				for(int i = 0; i < ips.size(); i++){
					//ipTree.getModel().
				}
			}
		}
	});
	private void startDHCPServer(){
		final SwingWorker<Object, Object> worker =
			new SwingWorker<Object, Object>() {
			@Override
			public Object doInBackground() {
				try {
					state = 0;
					dhcpServer = new DHCPServer(
							InetAddress.getByAddress(new byte[]{(byte)192, (byte)168, (byte)1, (byte)1}));
					saveConfigurations();
					dhcpServer.setConfigurations(
							config.getAsBytes("gateway", "\\."),
							config.getAsBytes("dns", "\\."),
							ByteFactory.getIntAsBytes(
									(int)Long.parseLong(config.get("renewal"))),
							ByteFactory.getIntAsBytes(
									(int)Long.parseLong(config.get("rebinding"))),
							ByteFactory.getIntAsBytes(
									(int)Long.parseLong(config.get("lease"))),
							config.getAsBytes("subnet-mask", "\\."),
							config.getAsBytes("prefix", "\\."));
					state = 1;
					dhcpServer.start();
				} catch (SocketException e) {
					state = -1;
					error(e, null);
				} catch (UnknownHostException e) {
					state = -1;
					error(e, null);
				}
				
				return null;
			}
		};
		worker.execute();
		while(state == 0){
			try { Thread.sleep(100L);}
			catch (InterruptedException e) { error(e, null);}
		}
		if(state == 1){
			ipTimer.start();
			startItem.setText("Stop Server");
		}
	}
	private void error(Exception e, String text){
		e.printStackTrace();
	}
	private void loadConfigurations(){
		config = new MappingFile();
		boolean prev = config.load("configs", true);
		if(!prev){
			config.set("dns"        , "4.2.2.4"      );
			config.set("gateway"    , "192.168.0.1"  );
			config.set("subnet-mask", "255.255.255.0");
			config.set("prefix"     , "192.168.0.0"  );
			config.set("renewal"    ,  "86400"  );
			config.set("rebinding"  , "129600"  );
			config.set("lease"      , "259200"  );
			config.writeMappings();
			System.out.flush();
		}
	}
	private void initTextBoxes(){
		  gatewayText.setText(config.get("gateway"));
		      dnsText.setText(config.get("dns"));
		   subnetText.setText(config.get("subnet-mask"));
		   prefixText.setText(config.get("prefix"));
		  renewalText.setText(config.get("renewal"));
		rebindingText.setText(config.get("rebinding"));
		    leaseText.setText(config.get("lease"));
	}
	private void saveConfigurations(){
		config.set("dns"        ,       dnsText.getText());
		config.set("subnet-mask",    subnetText.getText());
		config.set("gateway"    ,   gatewayText.getText());
		config.set("prefix"     ,    prefixText.getText());
		config.set("renewal"    ,   renewalText.getText());
		config.set("rebinding"  , rebindingText.getText());
		config.set("lease"      ,     leaseText.getText());
		config.writeMappings();
	}
	@Override
	public void windowActivated(WindowEvent e) { }
	@Override
	public void windowClosed(WindowEvent e) { }
	@Override
	public void windowClosing(WindowEvent e) {
		dhcpServer.stopServer();
	}
	@Override
	public void windowDeactivated(WindowEvent e) { }
	@Override
	public void windowDeiconified(WindowEvent e) { }
	@Override
	public void windowIconified(WindowEvent e) { }
	@Override
	public void windowOpened(WindowEvent e) { }
}