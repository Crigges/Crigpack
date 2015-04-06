package de.crigges.stuff;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.TreeSet;


import org.nustaq.serialization.FSTConfiguration;


public class CrigFile{
	
	private static final int versionNumber = 1;
	private static final int headerSize = 28;
	private static final int bufferSize = 2048;
	
	private FileChannel fc;
	private FSTConfiguration serializer = FSTConfiguration.createDefaultConfiguration();
	private MappedByteBuffer headerBuffer;
	private TreeSet<Block> freeBlocks = new TreeSet<>();
	private ArrayList<Block> content = new ArrayList<>();
	private IdentityHashMap<Object, Block> blockMap = new IdentityHashMap<>();
	private long endOfFile = headerSize;
	private Block last = null;
	private Block first = null;
	
	public CrigFile(String path) throws CrigpackException{
		try {
			fc = FileChannel.open(new File(path).toPath(), StandardOpenOption.CREATE, 
					StandardOpenOption.WRITE, StandardOpenOption.READ);
			headerBuffer = fc.map(MapMode.READ_WRITE, 0, headerSize);
			if(fc.size() == headerSize){
				addHeader();
			};
			long nextPos = getFirstBlockPos();
			int index = 0;
			int blockCount = getBlockCount();
			while(index < blockCount){
				MappedByteBuffer temp = fc.map(MapMode.READ_WRITE, nextPos, 4);
				int blockLength = temp.getInt();
				temp = fc.map(MapMode.READ_WRITE, nextPos, blockLength + 20);
				temp.position(4);
				Block cur = new Block();
				Object con = cur.readExisting(blockLength, nextPos, temp);
				endOfFile = nextPos + blockLength + 20;
				nextPos = cur.nextPos;
				System.out.println(nextPos);
				System.out.println(con);
				content.add(cur);
				blockMap.put(con, cur);
				cur.setPrevBlock(last);
				if(last != null){
					last.setNextBlock(cur);
				}
				last = cur;
				index++;
			}
		} catch (IOException e) {
			throw new CrigpackException("Could not open or create file for reason:\n", e);
		}
	}
	
	
	/**
	 * Writes the given object to the file
	 * @param data		Object to save 
	 * @throws CrigpackException 
	 */
	public void saveData(Object data) throws CrigpackException{
		if(blockMap.containsKey(data)){
			throw new CrigpackException("The file already contains the given object");
		}else{
			Block b = new Block();
			b.initNew(data);
			blockMap.put(data, b);
		}
	}
	
	/**
	 * Updates the given object in the file
	 * @param data		Object to save 
	 * @throws CrigpackException 
	 */
	public void updateData(Object data) throws CrigpackException{
		if(!blockMap.containsKey(data)){
			throw new CrigpackException("The file does not contain the given object");
		}else{
			
		}
	}
	
	/**
	 * Updates the given object in the file
	 * @param data		Object to save 
	 * @throws CrigpackException 
	 */
	public void deleteData(Object data) throws CrigpackException{
		if(!blockMap.containsKey(data)){
			throw new CrigpackException("The file does not contain the given object");
		}else{
			Block b = blockMap.get(data);
			b.delete();
			b.blockSize--;
			freeBlocks.add(b);
			blockMap.remove(data);
		}
	}
	
	private int getBlockCount(){
		return headerBuffer.getInt(16);
	}
	
	private void updateBlockCount(){
		headerBuffer.putInt(16, content.size());
		headerBuffer.force();
	}
	
	private long getFirstBlockPos(){
		return headerBuffer.getLong(20);
	}
	
	private void updateFirstBlockPos(){
		headerBuffer.putLong(20, first.position);
		headerBuffer.force();
	}
	
	private void addHeader() throws IOException{
		headerBuffer.put("Crigfile".getBytes());
		headerBuffer.putInt(versionNumber);
		headerBuffer.putInt(FileAttributes.Exists.getBit());
		headerBuffer.putInt(content.size());
		headerBuffer.putLong(headerSize);
	}
	
	private void readHeader() throws CrigpackException{
		byte[] arr = new byte[8];
		headerBuffer.get(arr);
		if(!new String(arr).equals("Crigfile")){
			throw new CrigpackException("Invaild Fileformat");
		}
		int version = headerBuffer.getInt();
		if(version != versionNumber){
			throw new CrigpackException("Unsupported Crigpack version:" + version);
		}
		int flags = headerBuffer.getInt();
		if((flags & FileAttributes.Exists.getBit()) != FileAttributes.Exists.getBit()){
			throw new CrigpackException("Exist flag was not set, this is an internal Crigpack error");
		}
		if((flags & FileAttributes.IsModified.getBit()) != FileAttributes.IsModified.getBit()){
			throw new CrigpackException("Crigpack crashed while writing to disc your file is courpted");
		}
	}
	
	private long getMatchtingPosition(Block b){
		Block b = freeBlocks.higher(b);
		long temp = endOfFile;
		endOfFile += (length + 20);
		return temp;
	}
	
	private class Block implements Comparable<Block>{
		private Object content = null; 
		private long position;
		private MappedByteBuffer buffer;
		private Block prev, next = null;
		private long nextPos = 0;
		private int blockSize;
		
		private Block(){}
		
		private void initNew(Object o) throws CrigpackException{
			byte[] temp = serializer.asByteArray(o);
			blockSize = temp.length;
			this.position = getMatchtingPosition(this);
			try {
				buffer = fc.map(MapMode.READ_WRITE, position, blockSize + 20);
			} catch (IOException e) {
				throw new CrigpackException("Could not access file for reason:\n", e);
			}
			buffer.putInt(blockSize);
			buffer.put(temp);
			setPrevBlock(last);
			if(last != null){
				last.setNextBlock(this);
			}
			last = this;
			CrigFile.this.content.add(this);
			updateBlockCount();
		}
		
		private Object readExisting(int size, long pos, MappedByteBuffer buffer){
			this.blockSize = size;
			this.position = pos;
			this.buffer = buffer;
			byte[] temp = new byte[size];
			buffer.get(temp);
			content = serializer.asObject(temp);
			nextPos = buffer.getLong(buffer.position() + 8);
			return content;	
		}
		
		public final void delete() throws CrigpackException{
			buffer.clear();
			if(prev != null){
				prev.setNextBlock(next);
			}
			if(next != null){
				next.setPrevBlock(prev);
			}
			CrigFile.this.content.remove(this);
			updateBlockCount();
		}
		
		private void setNextBlock(Block next){
			if(next != null){
				this.next = next;
				buffer.putLong(blockSize + 12, next.position);
				buffer.force();
			}else{
				this.next = null;
				buffer.putLong(blockSize + 12, 0);
				buffer.force();
			}
		}
		
		private void setPrevBlock(Block prev){
			if(prev != null){
				this.prev = prev;
				buffer.putLong(blockSize + 4, prev.position);
				buffer.force();
			}else{
				first = this;
				updateFirstBlockPos();
				this.prev = null;
				buffer.putLong(blockSize + 4, 0);
				buffer.force();
			}
		}

		@Override
		public int compareTo(Block o) {
			return this.blockSize - o.blockSize;
		}	
	}
}
