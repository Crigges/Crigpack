package de.crigges.sample;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;

import de.crigges.stuff.CrigFile;
import de.crigges.stuff.CrigpackException;

public class TerrainCrigFile{
	
	public static void main(String[] args) throws CrigpackException, InterruptedException{
		CrigFile c = new CrigFile("C:\\Users\\Crigges-Pc\\Desktop\\wurst.txt");
		c.saveData("käse");
		c.saveData("kädsfde");
		c.saveData("kädfsse");
		int id = c.saveData("wurst");
		c.deleteDataById(id);
		
	}
}
