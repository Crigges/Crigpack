package de.crigges.sample;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;

import de.crigges.stuff.CrigFile;
import de.crigges.stuff.CrigpackException;

public class TerrainCrigFile{
	
	public static void main(String[] args) throws CrigpackException{
		CrigFile c = new CrigFile("C:\\Users\\Crigges-Pc\\Desktop\\wurst.txt");
		c.setLoadAction((Object o) -> System.out.println(o));
		c.loadData();
//		for(int i = 1; i<= 100000; i++){
//			c.saveData("jojojojomotherfuckingbiatchesyeahrma" + i);
//		}
	}
}
