package mgui;

import java.awt.event.KeyEvent;

import javax.swing.JTextArea;
import javax.swing.text.Document;

public class ImmutableTextArea extends JTextArea {

	private static final long serialVersionUID = -352332766992079717L;

	public ImmutableTextArea() { }

	public ImmutableTextArea(String text) { super(text); }

	public ImmutableTextArea(Document doc) { super(doc); }

	public ImmutableTextArea(int rows, int columns) { super(rows, columns); }

	public ImmutableTextArea(String text, int rows, int columns) { super(text, rows, columns); }

	public ImmutableTextArea(Document doc, String text, int rows, int columns) { super(doc, text, rows, columns); }

	@Override
	protected void processKeyEvent(KeyEvent e) { }

}
