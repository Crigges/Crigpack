package de.crigges.stuff;

public enum FileAttributes {
	Exists		(1),
	Compressed	(2),
	Encrypted	(4),
	IsModified	(8);
	
	
	
	private int bit;
	
	FileAttributes(int b){
		bit = b;
	}
	
	public int getBit(){
		return bit;
	}

}
