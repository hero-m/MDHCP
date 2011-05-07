package mgui;

import java.util.regex.Pattern;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class IPAddressField extends JTextField {

	private static final long serialVersionUID = -2590704644564492679L;

	public IPAddressField(int i) {
		super(i);
	}

	@Override
    protected Document createDefaultModel()
    {
        return new NumericDocument();
    }

    private static class NumericDocument extends PlainDocument
    {

		private static final long serialVersionUID = -9114276116387276313L;

        private final static Pattern IPCHARSET = Pattern.compile("[\\d\\.]*");
        
        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException
        {
            // Only insert the text if it matches the regular expression
            if (str != null && IPCHARSET.matcher(str).matches()){
            	String before = this.getText(0, this.getLength());
            	String all = before + str;
            	if((before.endsWith(".") || before.equals("")) && str.startsWith(".")) return;
            	if (before.matches(".*\\..*\\..*\\..*") && str.contains(".")) return;
            	int pos = all.lastIndexOf('.');
            	if((pos >= 0 || all.length() > 0) && pos != all.length() - 1){
            		int num = Integer.parseInt(all.substring(Math.max(all.lastIndexOf('.') + 1, 0)));
            		if(num > 255) return;
            	}
            	super.insertString(offs, str, a);
            }
        }
    }
}
