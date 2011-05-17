package mstructs;

import java.io.Serializable;

public class ByteArray implements Serializable{
	private static final long serialVersionUID = -4069696514988341800L;
	
	byte[] data;
	public ByteArray(byte[] d){ data = d; }
	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof ByteArray))
			return false;
		ByteArray other = (ByteArray) obj;
		for(int i = 0; i < other.data.length; i++){
			if(data[i] != other.data[i]) return false;
		}
		return true;
	}
	@Override
	public int hashCode(){
		int hash = data[0];
		for(int i = 1; i < data.length; i++){
			hash = Integer.rotateLeft(hash, 8);
			hash ^= data[i];
		}
		return hash;
	}
	public byte[] getData(){
		return data;
	}
}
