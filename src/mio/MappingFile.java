package mio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class MappingFile {
	HashMap<String, String> map;
	String filename;
	public MappingFile(){
		map = new HashMap<String, String>();
	}
	public boolean load(String filename, boolean create){
		try {
			File file = new File(filename);
			this.filename = filename;
			if(file.exists()){
				FileReader fileReader = new FileReader(filename);
				BufferedReader input = new BufferedReader(fileReader);
				readMappings(input);
				return true;
			}else{
				file.createNewFile();
				return false;
			}
		}catch (IOException e) { e.printStackTrace(); }
		return false;
	}
	
	private  void  readMappings(BufferedReader input){
		try {
			while(input.ready()){
				String line = input.readLine();
				String[] pieces = line.split(": ", 0);
				map.put(pieces[0], pieces[1]);
			}
		} catch (IOException e) { e.printStackTrace(); }
	}
	public   void writeMappings(){
		try {
			FileWriter writer = new FileWriter(filename, false);
			for(String item : map.keySet()){
				writer.write(item + ": " + map.get(item) + "\n");
			}
			writer.close();
		} catch (IOException e) { e.printStackTrace(); }
	}
	public String get(String key){ return map.get(key); }
	public   void set(String key, String value){
		map.put(key, value);
	}
	
	public byte[] getAsBytes(String key, String delimeter){
		String value = this.get(key);
		String[] pieces = value.split(delimeter);
		byte[] data = new byte[pieces.length];
		for(int i = 0; i < pieces.length; i++){
			data[i] = (byte) Integer.parseInt(pieces[i]);
		}
		return data;
	}
}
