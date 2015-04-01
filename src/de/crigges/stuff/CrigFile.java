package de.crigges.stuff;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.TreeSet;


public abstract class CrigFile{
	
	private static final int versionNumber = 1;
	private static final int headerSize = 28;
	private static final int bufferSize = 2048;
	
	private FileChannel fc;
	private MappedByteBuffer headerBuffer;
	private TreeSet<Block> freeBlocks = new TreeSet<>();
	private ArrayList<Block> content = new ArrayList<>();
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
			long firstBlockPos = getFirstBlockPos();
			MappedByteBuffer buf = fc.map(MapMode.READ_WRITE, firstBlockPos, fc.size());
			long prevPos = 0;
			long nextPos = getFirstBlockPos();
			int index = 0;
			int blockCount = getBlockCount();
			while(index < blockCount){
				long blockLength = buf.getLong();
				buf.position((int) (buf.position() + blockLength));
				MappedByteBuffer temp = fc.map(MapMode.READ_WRITE, nextPos, blockLength + 24);
				temp.position(8);
				Block cur = deserialize(index);
				cur.position = nextPos;
				cur.buffer = temp;
				cur.inUse = true;
				endOfFile = nextPos + blockLength + 24;
				prevPos = buf.getLong();
				nextPos = buf.getLong();
				if(nextPos != 0){
					buf.position((int) ((int) nextPos - firstBlockPos));
				}
				cur.deserialize(temp);
				System.out.println(cur);
				content.add(cur);
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
		long temp = endOfFile;
		endOfFile += (b.getBlockSize() + 24);
		return temp;
	}
	
	protected abstract Block deserialize(int blockIndex) throws IOException;
	
	protected abstract class Block{
		private long position;
		private MappedByteBuffer buffer;
		private Block prev, next = null;
		private long blockSize;
		private boolean inUse = false;
		
		public Block(){}
		
		protected final void init() throws CrigpackException{
			if(inUse){
				throw new CrigpackException("Block already initialized (The init function was called twice)");
			}
			this.position = getMatchtingPosition(this);
			this.inUse = true;
			try {
				buffer = fc.map(MapMode.READ_WRITE, position, getBlockSize() + 24);
			} catch (IOException e) {
				throw new CrigpackException("Could not access file for reason:\n", e);
			}
			blockSize = getBlockSize();
			buffer.putLong(blockSize);
			serialize(buffer);
			setPrevBlock(last);
			last = this;
			content.add(this);
			if(last != null){
				last.setNextBlock(this);
			}
			updateBlockCount();
			
		}
		
		protected void delete() throws CrigpackException{
			if(!inUse){
				throw new CrigpackException("Block already deleted");
			}
			inUse = false;
			buffer.clear();
			if(prev != null){
				prev.setNextBlock(next);
			}
			if(next != null){
				next.setPrevBlock(prev);
			}
			content.remove(this);
			updateBlockCount();
		}
		
		private void setNextBlock(Block next){
			if(next != null){
				this.next = next;
				buffer.putLong(getBlockSize() + 16, next.position);
				buffer.force();
			}else{
				this.next = null;
				buffer.putLong(getBlockSize() + 16, 0);
				buffer.force();
			}
		}
		
		private void setPrevBlock(Block prev){
			if(prev != null){
				this.prev = prev;
				buffer.putLong(getBlockSize() + 8, prev.position);
				buffer.force();
			}else{
				first = this;
				updateFirstBlockPos();
				this.prev = null;
				buffer.putLong(getBlockSize() + 8, 0);
				buffer.force();
			}
		}
		
		protected void update(){
			buffer.position(8);
			serialize(buffer);
		}
		
		protected abstract void serialize(MappedByteBuffer b);
		
		protected abstract void deserialize(MappedByteBuffer b);
		
		protected abstract int getBlockSize();
		
		
	}
}
