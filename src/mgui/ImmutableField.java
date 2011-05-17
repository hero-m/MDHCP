package mgui;

import java.awt.event.KeyEvent;

import javax.swing.JTextField;

public class ImmutableField extends JTextField {

	private static final long serialVersionUID = -458014490384781073L;

	public ImmutableField(){ super(); }
	public ImmutableField(int i) { super(i); }
	
	@Override
	protected void processKeyEvent(KeyEvent e) { }
}
