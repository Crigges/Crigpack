package de.crigges.sample;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;

import de.crigges.stuff.CrigFile;
import de.crigges.stuff.CrigpackException;

public class TerrainCrigFile{
	
	public static void main(String[] args) throws CrigpackException, InterruptedException{
		CrigFile c = new CrigFile("C:\\Users\\Crigges-Pc\\Desktop\\wurst.txt");
		//c.loadData();
//		c.saveData("stuff");
//		int id = c.saveData("käse");
//		c.deleteDataById(Integer.MIN_VALUE + 1);
		//Thread.sleep(1000);
		//c.deleteData("käse");
		System.out.println(c.loadById(Integer.MIN_VALUE));
		c.close();
	}
}
