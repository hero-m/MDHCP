package mnet;

public class Helper {
	public static String getHex(byte b){
		int num = (int)b;
		String str = Integer.toHexString(num);
		if(str.length() == 1) str = '0' + str;
		if(str.length() > 2) str = str.substring(str.length() - 2);
		return str;
	}
	public static String getBinary(byte b){
		int num = (int)b;
		String str = Integer.toBinaryString(num);
		while(str.length() < 8)
			str = '0' + str;
		if(str.length() > 8)
			str = str.substring(str.length() - 8);
		
		return str;
	}

}
