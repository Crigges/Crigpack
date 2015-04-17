package de.crigges.sample;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;

import com.sun.javafx.geom.Vec3d;

import de.crigges.stuff.CrigFile;
import de.crigges.stuff.CrigpackException;

public class TerrainCrigFile{
	
	public static void main(String[] args) throws CrigpackException{
		CrigFile c = new CrigFile("C:\\Users\\Crigges-Pc\\Desktop\\wurst.txt");
		c.setLoadAction((Object o) -> System.out.println(o));
		c.loadData();
//		c.saveData(new byte[10000]);
		c.saveData("käse");
		c.saveData("wurst");
		//c.deleteData("käse");
//		for(int i = 1; i <= 10000; i++){
//			c.saveData("wurstkuchen" + i);
//		}
	}
}
