package de.crigges.stuff;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;

public class TerrainCrigFile extends CrigFile {
	
	private HeaderData header;
	private ArrayList<TerrainData> terrainData = new ArrayList<>();

	public static void main(String[] args) throws CrigpackException,
			IOException {
		TerrainCrigFile c = new TerrainCrigFile(
				"C:\\Users\\Crigges-Pc\\Desktop\\wurst.txt");
//		c.new TerrainData(1, 2, 5);
		TerrainData s = c.new TerrainData(88, 88, 88);
		c.new TerrainData(2123, 1234, 1);
//		c.new TerrainData(1111, 431, 2);
		s.delete();
	}


	public TerrainCrigFile(String path) throws CrigpackException {
		super(path);
	}

	@Override
	protected Block deserialize(int blockIndex) throws IOException {
		if (blockIndex == 300) {
			header = new HeaderData();
			return header;
		} else {
			TerrainData d = new TerrainData();
			//terrainData.add(d);
			return d;
		}

	}

	class TerrainData extends Block {
		float x, y;
		int tiletype;

		public TerrainData(float x, float y, int tiletype) throws IOException {
			super();
			this.x = x;
			this.y = y;
			this.tiletype = tiletype;
			init();
		}

		public TerrainData(){}

		@Override
		protected int getBlockSize() {
			// 2 floats each 4 bytes + 1 int -> 12 bytes
			return 12;
		}

		@Override
		protected void serialize(MappedByteBuffer b) {
			b.putFloat(x);
			b.putFloat(y);
			b.putInt(tiletype);
		}

		@Override
		protected void deserialize(MappedByteBuffer b) {
			x = b.getFloat();
			y = b.getFloat();
			tiletype = b.getInt();
		}
		
		@Override
		public String toString(){
			return "[" + x + ", " + y + "] Tiletype:" + tiletype; 
		}
	}

	class HeaderData extends Block {
		int versionNumber = 1;
		int dimensionX = 512;
		int dimensionY = 1024;

		/**
		 * Create a new HeaderData block
		 * 
		 * @param dimensionX
		 * @param dimensionY
		 * @throws IOException
		 */
		public HeaderData(int versionNumber, int dimensionX, int dimensionY)
				throws IOException {
			super();
			this.versionNumber = versionNumber;
			this.dimensionX = dimensionX;
			this.dimensionY = dimensionY;
			init();
		}
	

		public HeaderData() {
		}

		@Override
		protected int getBlockSize() {
			// 3 ints each 4 bytes -> 12 bytes
			return 12;
		}

		@Override
		protected void serialize(MappedByteBuffer b) {
			b.putInt(versionNumber);
			b.putInt(dimensionX);
			b.putInt(dimensionY);
		}

		@Override
		protected void deserialize(MappedByteBuffer b) {
			versionNumber = b.getInt();
			dimensionX = b.getInt();
			dimensionY = b.getInt();
		}
		
		@Override
		public String toString(){
			return "[" + dimensionX + ", " + dimensionY + "] Version:" + versionNumber; 
		}
	}
}
