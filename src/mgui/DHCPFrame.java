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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import mio.MappingFile;
import mnet.ByteFactory;
import mnet.Constants;
import mnet.DHCPPacket;
import mnet.DHCPServer;
import mnet.DHCPServer.MessageGroup;
import mnet.Helper;
import mstructs.ByteArray;
public class DHCPFrame extends JFrame implements ActionListener, WindowListener, TreeSelectionListener{
	private static final long serialVersionUID = 129578221875757459L;
	DHCPServer dhcpServer;
	private MappingFile config;
	
	private JMenuItem startItem;

	private IPAddressField gatewayText, dnsText, subnetText, prefixText;
	private     JTextField renewalText, rebindingText, leaseText, dhcpServerText;
	
	private ImmutableField    opField,  htypeField,   hlenField,   hopsField,
					     xidField,   secsField,  flagsField, ciaddrField,
					  yiaddrField, siaddrField, giaddrField, chaddrField,
					   snameField,   fileField,  magicField;
	private JCheckBox incrementalCheck, verifyCheck;
			
	private ImmutableTextArea optionsArea;
	
	JTree infoTree;
	DefaultMutableTreeNode rootNode, allocatedNode, reservedNode, messageNode;
	DefaultTreeModel treeModel;
	public DHCPFrame(){
		loadConfigurations();
		try {
			dhcpServer = new DHCPServer();
		} catch (SocketException e) { error(e, null); return;}
		/* create tree */
		rootNode = new DefaultMutableTreeNode("Recorded Information");
		 reservedNode = new DefaultMutableTreeNode( "Reserved IP Addresses", true);
		 reservedNode.setAllowsChildren(true);
		allocatedNode = new DefaultMutableTreeNode("Allocated IP Addresses", true);
		  messageNode = new DefaultMutableTreeNode("Transmitted Messages"  , true);
		//TODO: remove this
		  //DefaultMutableTreeNode nextNode = new DefaultMutableTreeNode("a message", false);
	 //messageNode.add(     nextNode);
		rootNode.add( reservedNode);
		rootNode.add(allocatedNode);
		rootNode.add(  messageNode);
		treeModel = new DefaultTreeModel(rootNode);

		infoTree = new JTree(treeModel);
		infoTree.setEditable(true);
		infoTree.getSelectionModel().setSelectionMode
		        (TreeSelectionModel.SINGLE_TREE_SELECTION);
		infoTree.setShowsRootHandles(true);
		infoTree.addTreeSelectionListener(this);
		infoTree.setPreferredSize(new Dimension(200, 500));
		infoTree.setMinimumSize(new Dimension(200, 500));
		JScrollPane treeScrollPane = new JScrollPane();
		treeScrollPane.setMinimumSize(new Dimension(100, 600));
		treeScrollPane.setPreferredSize(new Dimension(100, 600));
		//treeScrollPane.setLayout(new BorderLayout(10, 10));
		treeScrollPane.add(infoTree, JScrollPane.CENTER_ALIGNMENT);
		/*  end create tree */
		
		JTabbedPane tabbed = new JTabbedPane(JTabbedPane.LEFT);
		JPanel configPanel, infoPanel;
		
		/* info panel creation */
		infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		infoPanel.add(treeScrollPane, BorderLayout.WEST);
		infoPanel.setName("Information");
		
		JPanel packetPanel = new JPanel();
		GridBagLayout packetGbLayout = new GridBagLayout();
		packetGbLayout.columnWidths = new int[]{100, 100, 100, 100};
		//packetGbLayout.rowWeights = new double[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 8};
		packetGbLayout.rowHeights = new    int[]{10, 10, 10, 20, 20, 20, 20, 20, 20, 20, 20, 160};
		GridBagConstraints     oneGBC = new GridBagConstraints(),
						    twoEndGBC = new GridBagConstraints(),
							  fourGBC = new GridBagConstraints(),
							 twoStGBC = new GridBagConstraints(),
							  areaGBC = new GridBagConstraints();
		  oneGBC.gridwidth = 1; twoEndGBC.gridwidth = 2; fourGBC.gridwidth = 4;
		twoStGBC.gridwidth = 2;  twoStGBC.fill = GridBagConstraints.HORIZONTAL;
		 areaGBC.gridheight = 8; areaGBC.gridwidth = 4; areaGBC.fill = GridBagConstraints.BOTH;
		 areaGBC.gridx = 0; areaGBC.anchor = GridBagConstraints.WEST;
		twoStGBC.gridx = 0; twoEndGBC.gridx = 2;
		oneGBC.fill = GridBagConstraints.HORIZONTAL;
		twoEndGBC.fill = GridBagConstraints.HORIZONTAL;
		fourGBC.anchor = GridBagConstraints.WEST;
		fourGBC.gridx = 0;
		fourGBC.fill = GridBagConstraints.HORIZONTAL;
		
		optionsArea = new ImmutableTextArea(80, 10);
		
		    opField = new ImmutableField();  htypeField = new ImmutableField();   hlenField = new ImmutableField();
		  hopsField = new ImmutableField();    xidField = new ImmutableField();   secsField = new ImmutableField();
		 flagsField = new ImmutableField(); ciaddrField = new ImmutableField(); yiaddrField = new ImmutableField();
		siaddrField = new ImmutableField(); giaddrField = new ImmutableField(); chaddrField = new ImmutableField();
		 snameField = new ImmutableField();   fileField = new ImmutableField();  magicField = new ImmutableField();

		center(opField); center(htypeField); center(hlenField);	center(hopsField);
		center(xidField); center(secsField); center(flagsField); center(ciaddrField);
		center(yiaddrField); center(siaddrField); center(giaddrField); center(chaddrField);
		center(snameField);	center(fileField); center(magicField);
		
		packetPanel.setLayout(packetGbLayout);
		packetPanel.add(    opField,    oneGBC); packetPanel.add( htypeField,   oneGBC);
		packetPanel.add(  hlenField,    oneGBC); packetPanel.add(  hopsField,   oneGBC);
		packetPanel.add(   xidField,   fourGBC); packetPanel.add(  secsField, twoStGBC);
		packetPanel.add( flagsField, twoEndGBC); packetPanel.add(ciaddrField,  fourGBC);
		packetPanel.add(yiaddrField,   fourGBC); packetPanel.add(siaddrField,  fourGBC);
		packetPanel.add(giaddrField,   fourGBC); packetPanel.add(chaddrField,  fourGBC);
		packetPanel.add( snameField,   fourGBC); packetPanel.add(  fileField,  fourGBC);
		packetPanel.add( magicField,   fourGBC); packetPanel.add(optionsArea,  areaGBC);
		
		JButton legendButton = new JButton("Show Legend");
		legendButton.addActionListener(this);
		GridBagConstraints legendGBC = new GridBagConstraints();
		legendGBC.gridx = 1; legendGBC.gridwidth = 1; legendGBC.insets.top = 10;
		packetPanel.add(legendButton, legendGBC);
		infoPanel.add(packetPanel, BorderLayout.CENTER);
		/* end info panel creation */
		
		
		/* Configuration Panel Creation */
		
		  renewalText = new JTextField(15);
		rebindingText = new JTextField(15);
		    leaseText = new JTextField(15);
		
		   gatewayText = new IPAddressField(15);    dnsText = new IPAddressField(15);
		    subnetText = new IPAddressField(15); prefixText = new IPAddressField(15);
		dhcpServerText = new IPAddressField(15);

		incrementalCheck = new JCheckBox("Incremental Lease"); 
		verifyCheck = new JCheckBox("Verify Clients (linux only)");
		initTextBoxes();
		
		JLabel    gatewayLabel = new JLabel("Gateway:")       ,       dnsLabel = new JLabel("DNS:"           ),
			       subnetLabel = new JLabel("Subnet:" )       ,    prefixLabel = new JLabel("Prefix:"        ),
		          renewalLabel = new JLabel("Renewal Time:")  , rebindingLabel = new JLabel("Rebinding Time:"),
		            leaseLabel = new JLabel("Lease Time:"  )  , dhcpServerLabel = new JLabel("DHCP Server IP:");
		
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
		configPanel.add(dhcpServerLabel, constraintsM2); configPanel.add(dhcpServerText, constraintsR);
		configPanel.add(incrementalCheck, constraintsM1); configPanel.add(verifyCheck, constraintsM2);
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
		this.setMinimumSize(new Dimension(800, 300));
		this.setSize(900, 600);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		showLegend();
		this.setVisible(true);

		infoTimer.start();
	}
	private int state = 0;
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
			System.exit(0);
		}else if(event.getActionCommand().equals("Save Config")){
			saveConfigurations();
		}else if(event.getActionCommand().equals("Restart Server")){
			if(state == 1){
				dhcpServer.stopServer();
				dhcpServer.clearDB();
			}
			startDHCPServer();
		}else if(event.getActionCommand().equals("Show Legend")){
			showLegend();
		}
	}
	public void showLegend(){
		    opField.setText("op"    );  htypeField.setText("htype" );   hlenField.setText("hlen"  );
		  hopsField.setText("hops"  );    xidField.setText("xid"   );   secsField.setText("secs"  );
		 flagsField.setText("flags" ); ciaddrField.setText("ciaddr"); yiaddrField.setText("yiaddr");
		siaddrField.setText("siaddr"); giaddrField.setText("giaddr"); chaddrField.setText("chaddr");
		 snameField.setText("sname" );   fileField.setText("file"  );  magicField.setText("magic cookie");
		optionsArea.setText("options");
		
	}
	public void showMessage(DHCPPacket packet){
		    opField.setText(packet.getOp()   + ""); htypeField.setText(packet.getHtype() + "");
		  hlenField.setText(packet.getHlen() + "");  hopsField.setText(packet.getHops()  + "");
		   xidField.setText(Helper.xidToString(packet.getXid()));
		  secsField.setText(Helper.secsToString(packet.getSecs()));
		 flagsField.setText(Helper.flagsToString(packet.getFlags()));
		ciaddrField.setText(Helper.ipToString(packet.getCiaddr()));
		yiaddrField.setText(Helper.ipToString(packet.getYiaddr()));
		siaddrField.setText(Helper.ipToString(packet.getSiaddr()));
		giaddrField.setText(Helper.ipToString(packet.getGiaddr()));
		chaddrField.setText(Helper.chaddrToString(packet.getChaddr()));
		 snameField.setText(" 64 bytes ignored (probably zero).");
		  fileField.setText("128 bytes ignored (probably zero).");
		 magicField.setText(Helper.cookieToString(packet.getCookie()));
		 
		 String optionsStr = "Options\n";
		 byte[] optionNums = new byte[]{53, 50, 54, 1, 3, 6, 58, 59, 51, 12, 55, 61, (byte)255};
		 for(int i = 0; i < optionNums.length; i++){
			 byte[] option = packet.getOption(optionNums[i]);
			 if(option != null){
				 optionsStr += Constants.DHCPOptions.get(optionNums[i]) + ": " +
				 							packet.getOptionString(optionNums[i]) + "\n";
			 }
		 }
		 optionsArea.setText(optionsStr);
	}
	public void center(JTextField field){
		field.setHorizontalAlignment(JTextField.CENTER);
	}
	private int ipCount = 0, reserveCount = 0, messageCount = 0;
	Vector<ByteArray> ips, reserves;
	HashMap<ByteArray, MessageGroup> messages;
	private Timer infoTimer = new Timer(300, new ActionListener(){
		public void actionPerformed(ActionEvent evt){
			if(dhcpServer == null) return;
			if(ipCount != dhcpServer.getIPCount()){
				ips = dhcpServer.getIPs();
				allocatedNode.removeAllChildren();
				for(int i = 0; i < ips.size(); i++){
					allocatedNode.add(new DefaultMutableTreeNode(
							Helper.ipToString(ips.elementAt(i).getData())));
				}
				ipCount = dhcpServer.getIPCount();
				treeModel.nodeChanged(allocatedNode);
				treeModel.nodeStructureChanged(allocatedNode);
			}
			
			if(reserveCount != dhcpServer.getReserveCount()){
				reserves = dhcpServer.getReserves();
				reservedNode.removeAllChildren();
				for(int i = 0; i < reserves.size(); i++){
					reservedNode.add(new DefaultMutableTreeNode(
							Helper.ipToString(reserves.elementAt(i).getData())));
				}
				reserveCount = dhcpServer.getReserveCount();
				treeModel.nodeChanged(reservedNode);
				treeModel.nodeStructureChanged(reservedNode);
			}
			if(messageCount != dhcpServer.getMessageCount()){
				messages = dhcpServer.getMessages();
				messageNode.removeAllChildren();
				Iterator<ByteArray> iterator = messages.keySet().iterator();
				while(iterator.hasNext()){
					ByteArray key = iterator.next();
					DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(
							ByteFactory.getHex(key.getData()));
					MessageGroup group = messages.get(key);
					for(int i = 0; i < group.getSize(); i++){
						groupNode.add(new DefaultMutableTreeNode(
								group.getMessage(i).getMessageType()));
					}
					messageNode.add(groupNode);
				}
				System.out.println("Rebuilding messages: "+messageCount);
				messageCount = dhcpServer.getMessageCount();
				treeModel.nodeChanged(messageNode);
				treeModel.nodeStructureChanged(messageNode);
			}
		}
	});
	private void startDHCPServer(){
		final SwingWorker<Object, Object> worker =
			new SwingWorker<Object, Object>() {
			@Override
			public Object doInBackground() {
				state = 0;
				saveConfigurations();
				dhcpServer.setConfigurations(
						config.getAsBytes("gateway", "\\."),
						config.getAsBytes("dns", "\\."),
						Long.parseLong(config.get("renewal")) * 1000,
						Long.parseLong(config.get("rebinding")) * 1000,
						Long.parseLong(config.get("lease")) * 1000,
						config.getAsBytes("subnet-mask", "\\."),
						config.getAsBytes("prefix", "\\."),
						config.getAsBytes("dhcp-server", "\\."),
						Boolean.parseBoolean(config.get("incremental-lease")),
						Boolean.parseBoolean(config.get("verify-clients")));
				state = 1;
				dhcpServer.start();
			
				return null;
			}
		};
		try{
			worker.execute();
		}catch(Exception e){e.printStackTrace();}
		startItem.setText("Stop Server");
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
			config.set("dhcp-server", "192.168.0.1");
			config.set("incremental-lease", "false");
			config.set("verify-clients", "false");
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
	   dhcpServerText.setText(config.get("dhcp-server"));
	 incrementalCheck.setSelected(Boolean.parseBoolean(config.get("incremental-lease")));
	      verifyCheck.setSelected(Boolean.parseBoolean(config.get("verify-clients")));
	}
	private void saveConfigurations(){
		config.set("dns"        ,       dnsText.getText());
		config.set("subnet-mask",    subnetText.getText());
		config.set("gateway"    ,   gatewayText.getText());
		config.set("prefix"     ,    prefixText.getText());
		config.set("renewal"    ,   renewalText.getText());
		config.set("rebinding"  , rebindingText.getText());
		config.set("lease"      ,     leaseText.getText());
		config.set("dhcp-server",dhcpServerText.getText());
		config.set("incremental-lease", incrementalCheck.isSelected() + "");
		config.set("verify-clients"   ,      verifyCheck.isSelected() + "");
		config.writeMappings();
	}
	@Override
	public void windowActivated(WindowEvent e) { }
	@Override
	public void windowClosed(WindowEvent e) { }
	@Override
	public void windowClosing(WindowEvent e) {
		if(dhcpServer != null)
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
	@Override
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode selNode =
			(DefaultMutableTreeNode)e.getPath().getLastPathComponent();
		if(messageNode.isNodeDescendant(selNode) && selNode.getLevel() > 2){
			String message = (String)selNode.getUserObject();
			
			System.out.println("Selected a message");
			//TODO: show selected message in message-fields.
		}else{
			showLegend();
		}
		
	}
}