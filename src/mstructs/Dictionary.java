package mstructs;

import java.util.Vector;
import java.io.*;
public class Dictionary {
	private Vector<Object[]> dict;
	private int bufferhi = -1, bufferlo = -1;
	private String bufferstr = null;
	public Dictionary(String filename){
		FileReader inputfile;
		dict = new Vector<Object[]>();
		try {
			inputfile = new FileReader(filename);
			java.io.BufferedReader input = new BufferedReader(inputfile);
			while(input.ready()){
				String line = input.readLine();
				String[] parts = line.split("\t", 2);
				if(parts[0].contains("-")){
					String[] subparts = parts[0].split("-", 2);
					dict.add(new Object[]{Integer.parseInt(subparts[0]),
										  Integer.parseInt(subparts[1]),
										  parts[1]});
				}else{
					dict.add(new Object[]{Integer.parseInt(parts[0]),
										  parts[1]});
				}
			}
		} catch (FileNotFoundException e) {	e.printStackTrace();
		} catch (IOException e) {e.printStackTrace();}
	}
	
	public String get(int num){
		if(num >= bufferlo && num <= bufferhi)
			return bufferstr;
		for(int i = 0; i < dict.size(); i++){
			Object[] pair = dict.elementAt(i);
			if(pair.length == 3){
				int lo = (Integer)pair[0];
				int hi = (Integer)pair[1];
				if(num >= lo && num <= hi){
					bufferlo = lo; bufferhi = hi; bufferstr = (String)pair[2];
					return (String)pair[2];
				}
			}else if(pair.length == 2){
				int key = (Integer)pair[0];
				if(num == key){
					bufferlo = key; bufferhi = key; bufferstr = (String)pair[1];
					return (String)pair[1];
				}
			}
		}
		return null;
	}
	
	public boolean contains(int num){
		if(get(num) != null)
			return true;
		else 
			return false;
	}
}
