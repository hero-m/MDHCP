package mgui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.*;

import mnet.DHCPServer;
public class DHCPFrame extends JFrame implements ActionListener{
	private static final long serialVersionUID = 129578221875757459L;
	DHCPServer dhcpServer;
	private JMenuItem startItem;

	public DHCPFrame(){
		
		JTree tree = new JTree(new String[]{"Allocated IP Addresses", "Reserved IP Addresses"});
		JTabbedPane tabbed = new JTabbedPane(JTabbedPane.LEFT);
		JPanel configPanel, infoPanel;
		infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		infoPanel.add(tree, BorderLayout.WEST);
		infoPanel.setName("Information");
		
		/* Configuration Panel Creation */
		
		JTextField renewalText, rebindingText, leaseText;
		  renewalText = new JTextField(15);
		rebindingText = new JTextField(15);
		    leaseText = new JTextField(15);
		
		IPAddressField gatewayText, dnsText, subnetText;
		gatewayText = new IPAddressField(15); dnsText = new IPAddressField(15);
		subnetText = new IPAddressField(15);
		
		JLabel    gatewayLabel = new JLabel("Gateway:")       ,     dnsLabel = new JLabel("DNS:"          ),
			       subnetLabel = new JLabel("Subnet:" )       , renewalLabel = new JLabel("Renewal Time:"),
			    rebindingLabel = new JLabel("Rebinding Time:"),   leaseLabel = new JLabel("Lease Time:"  );
		
		JButton restartButton = new JButton("Restart Server"), applyButton = new JButton("Apply Changes");
		JPanel buttonSubPanel = new JPanel();
		buttonSubPanel.add(restartButton); buttonSubPanel.add(applyButton);
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
		configPanel.add(  renewalLabel, constraintsM2); configPanel.add(  renewalText, constraintsR );
		configPanel.add(rebindingLabel, constraintsL ); configPanel.add(rebindingText, constraintsM1);
		configPanel.add(    leaseLabel, constraintsM2); configPanel.add(    leaseText, constraintsR );
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
		
		tabbed.add(infoPanel, 0);
		tabbed.add(configPanel, 1);
		this.getContentPane().add(tabbed);
		this.setMinimumSize(new Dimension(750, 300));
		this.setSize(900, 600);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}
	
	public void actionPerformed(ActionEvent event){
		if(event.getActionCommand().equals("Start Server")){
			final SwingWorker<Object, Object> worker =
				new SwingWorker<Object, Object>() {
				@Override
				public Object doInBackground() {
					try {
						dhcpServer = new DHCPServer(
								InetAddress.getByAddress(new byte[]{(byte)192, (byte)168, (byte)1, (byte)1}));
						dhcpServer.start();
					} catch (SocketException e) { JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					} catch (UnknownHostException e) { e.printStackTrace();
					}
					return null;
				}
			};
			worker.execute();
			startItem.setText("Stop Server");
		}else if(event.getActionCommand().equals("Clear Database")){
			//clear the database file
		}else if(event.getActionCommand().equals("Stop Server")){
			
		}else if(event.getActionCommand().equals("Exit")){
			this.dispose();
		}
	}

}
