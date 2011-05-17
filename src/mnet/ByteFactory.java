package mnet;


import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

public class ByteFactory {
	public static String getHex(byte b){
		int num = (int)b;
		String str = Integer.toHexString(num);
		if(str.length() == 1) str = '0' + str;
		if(str.length() > 2) str = str.substring(str.length() - 2);
		return str;
	}
	public static int asInt(byte b){
		if(b < 0) return (int)(256 + b);
		else return b;
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
	public static byte[] getIntsAsBytes(int[] nums){
		ByteBuffer buffer = ByteBuffer.allocate(4 * nums.length);
		IntBuffer intBuffer = buffer.asIntBuffer();
		intBuffer.put(nums);
		return buffer.array();
	}
	public static byte[] getIntAsBytes(int num){
		return getIntAsBytes(num, 4);
	}
	public static byte[] getLongAsBytes(long num){
		ByteBuffer buffer = ByteBuffer.allocate(8);
		LongBuffer longBuffer = buffer.asLongBuffer();
		longBuffer.put(num);
		return buffer.array();
	}
	public static byte[] getIntAsBytes(int num, int size){
		ByteBuffer buffer = ByteBuffer.allocate(4);
		IntBuffer intBuffer = buffer.asIntBuffer();
		intBuffer.put(num);
		if(size == 4)
			return buffer.array();
		else{
			return Arrays.copyOfRange(buffer.array(), 4 - size, 4);
		}
	}
	public static String simpleFormatted(byte[] data){
		if(data == null) return null;
		String simpleStr;
		simpleStr = "";
		String binary = getBinary(data);
		String hex = getHex(data);
		byte[] bytes = data;
		int len = hex.length() / 2;
		int step = 4;
		int count = (int) Math.ceil(len / step);
		for(int i = 0; i < count; i++){
			for(int j = 0; j < step; j++){
				int pos = i * step + j;
				if(pos < len)
					simpleStr = simpleStr + binary.substring(pos * 8, pos * 8 + 8) + " ";
				else
					simpleStr = simpleStr + "         ";
			}
			simpleStr = simpleStr + "\t";
			for(int j = 0; j < step; j++){
				int pos = i * step + j;
				if(pos < len)
					simpleStr = simpleStr + hex.substring(pos * 2, pos * 2 + 2) + " ";
				else 
					simpleStr = simpleStr + "   ";
			}
			simpleStr = simpleStr + "\t";
			for(int j = 0; j < step; j++){
				int pos = i * step + j;
				if(pos < len)
					simpleStr = simpleStr + (char)bytes[pos];
				else
					simpleStr = simpleStr + " ";
			}
			simpleStr = simpleStr + "\n";
		}
		
		return simpleStr;
	}	
	public static String getBinary(byte[] data){
		
		String binaryStr = "";
		for(int i = 0; i < data.length; i++)
			binaryStr = binaryStr + ByteFactory.getBinary(data[i]);
		return binaryStr;
	}
	public static String getHex(byte[] data){
		String hexStr = "";
		for(int i = 0; i < data.length; i++)
			hexStr = hexStr + ByteFactory.getHex(data[i]);
		return hexStr;
	}
	public static int getBytesAsInt(byte[] bytes){
		int pow = 1;
		int res = 0;
		for(int i = 0; i < bytes.length; i++){
			int t = bytes[i];
			if(t < 0)
				res += (256 + t) * pow;
			else
				res += t * pow;
			pow *= 256;
		}
		return res;
	}
}
