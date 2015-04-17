package de.crigges.stuff;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.nustaq.serialization.FSTConfiguration;


public class CrigFile{
	
	private static final int versionNumber = 1;
	private static final int headerSize = 32;
	private static final int bufferSize = 2048;
	
	private FileChannel fc;
	private FSTConfiguration serializer = FSTConfiguration.createDefaultConfiguration();
	private MappedByteBuffer headerBuffer;
	private TreeSet<Block> freeBlocks = new TreeSet<>();
	private IdentityHashMap<Object, Block> blockMap = new IdentityHashMap<>();
	private HashMap<Integer, Long> idToPosMap = new HashMap<>();
	private HashMap<Integer, Object> idToObjMap = new HashMap<>();
	private IdentityHashMap<Object, Future<?>> taskMap = new IdentityHashMap<>();
	private ExecutorService taskExecuter = Executors.newSingleThreadExecutor();
	private long endOfFile = headerSize;
	private Block last = null;
	private Block first = null;
	private LoadAction onObjectLoad = null;
	private Thread loadThread = null;
	private Thread actionThread = null;
	private int curId;
	
	public CrigFile(String path) throws CrigpackException{
		try {
			fc = FileChannel.open(new File(path).toPath(), StandardOpenOption.CREATE, 
					StandardOpenOption.WRITE, StandardOpenOption.READ);
			headerBuffer = fc.map(MapMode.READ_WRITE, 0, headerSize);
			if(fc.size() == headerSize){
				addHeader();
			}else{
				readHeader();
			}
		} catch (IOException e) {
			throw new CrigpackException("Could not open or create file for reason:\n", e);
		}
	}
	
	public void close(){
		taskExecuter.shutdown();
	}
	
	public void loadData(){
		loadThread = new Thread(new Runnable() {
			@Override
			public void run() {
				loadDataThreaded();
			}
		});
		loadThread.start();
	}
	
	public Object loadById(int id) throws CrigpackException{
		boolean b = false;
		try {
			taskExecuter.shutdown();
			b = taskExecuter.awaitTermination(1000, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new CrigpackException("Could no terminate task executor for reason:", e);
		}
		taskExecuter = Executors.newSingleThreadExecutor();
		taskExecuter.submit(new Runnable() {	
			@Override
			public void run() {			}
		});
		if(b){
			Object o = idToObjMap.get(id);
			if(o == null){
				Long pos = idToPosMap.get(id);
				idToPosMap.remove(id);
				Block cur = null;
				try {
					cur = createBlockAtPos(pos);
				} catch (IOException e) {
					throw new CrigpackException("Could not create block for reason:", e);
				}
				idToObjMap.put(id, cur.content);
				blockMap.put(cur.content, cur);
				return cur.content;
			}else{
				return o;
			}
		}else{
			return null;
		}
	}
	
	private void executeLoadActionThreaded(Object o){
		if(actionThread != null){
			try {
				actionThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		actionThread = new Thread(new Runnable() {
			@Override
			public void run() {
				if(onObjectLoad != null){
					onObjectLoad.onLoad(o);
				}
			}
		});
		actionThread.start();
	}
	
	
	/**
	 * @throws CrigpackException 
	 * 
	 */
	private void loadDataThreaded(){
		long nextPos = getFirstBlockPos();
		int index = 0;
		int blockCount = getBlockCount();
		try {
			while(index < blockCount){
				Block cur = createBlockAtPos(nextPos);
				Object con = cur.content;
				endOfFile = nextPos + cur.blockSize + 24;
				nextPos = cur.nextPos;
				blockMap.put(con, cur);
				cur.prev = last;
				if(last != null){
					last.next = cur;
				}
				last = cur;
				index++;
				executeLoadActionThreaded(con);
			}
		} catch (IOException e) {
			//ignore exceptions
		}
	}
	
	public void setLoadAction(LoadAction action){
		this.onObjectLoad = action;
	}
	
	/**
	 * Writes the given object to the file
	 * @param data		Object to save 
	 * @throws CrigpackException 
	 */
	public int saveData(Object data) throws CrigpackException{
		if(loadThread != null){
			try {
				loadThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//check for existing save action of this object
		Future<?> saveTask = taskMap.get(data);
		int id = curId++;
		idToObjMap.put(id, data);
		if(saveTask == null || saveTask.isDone()){
			taskMap.put(data, taskExecuter.submit(new SaveTask(data, id)));
		}
		return id;
	}
	
	/**
	 * Deletes the given object in the file
	 * @param data		Object to save 
	 * @throws CrigpackException 
	 */
	public void deleteDataById(int id) throws CrigpackException{
		if(loadThread != null){
			try {
				loadThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Object o = idToObjMap.get(id);
		if(o == null){
			Long pos = idToPosMap.get(id);
			idToPosMap.remove(id);
			if(pos == null){
				throw new CrigpackException("Du hast was kaputt gemacht herr präsident");
			}
			taskExecuter.submit(new DeleteAtPosTask(pos));
		}else{
			deleteData(o);
		}
	}
	
	private Block createBlockAtPos(long pos) throws IOException{
		MappedByteBuffer temp;
		temp = fc.map(MapMode.READ_WRITE, pos, 4);
		int blockLength = temp.getInt();
		temp = fc.map(MapMode.READ_WRITE, pos, blockLength + 24);
		Block cur = new Block();
		cur.readExisting(pos, temp);
		return cur;
	}
	
	/**
	 * Deletes the given object in the file
	 * @param data		Object to save 
	 * @throws CrigpackException 
	 */
	public void deleteData(Object data) throws CrigpackException{
		if(loadThread != null){
			try {
				loadThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Future<?> saveTask = taskMap.get(data);
		if(saveTask != null){
			saveTask.cancel(false);
		}
		idToObjMap.remove(data);
		taskExecuter.submit(new DeleteTask(data));
	}
	
	/**
	 * Updates the given object in the file
	 * @param data		Object to save 
	 * @throws CrigpackException 
	 */
	public void updateData(Object data) throws CrigpackException{
		if(loadThread != null){
			try {
				loadThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//TODO
	}
	
	private int getBlockCount(){
		return headerBuffer.getInt(16);
	}
	
	private void incBlockCount(){
		headerBuffer.putInt(16, headerBuffer.getInt(16) + 1);
		headerBuffer.force();
	}
	
	private void decBlockCount(){
		headerBuffer.putInt(16, headerBuffer.getInt(16) - 1);
		headerBuffer.force();
	}
	
	private long getFirstBlockPos(){
		return headerBuffer.getLong(20);
	}
	
	private void updateFirstBlockPos(){
		headerBuffer.putLong(20, first.position);
		headerBuffer.force();
	}
	
	private void setLastUniqueId(int i){
		headerBuffer.putInt(28, i+1);
	}
	
	private void addHeader() throws IOException{
		headerBuffer.put("Crigfile".getBytes());
		headerBuffer.putInt(versionNumber);
		headerBuffer.putInt(FileAttributes.Exists.getBit());
		headerBuffer.putInt(0);
		headerBuffer.putLong(headerSize);
		headerBuffer.putInt(Integer.MIN_VALUE);
		curId = Integer.MIN_VALUE;
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
		if((flags & FileAttributes.IsModified.getBit()) == FileAttributes.IsModified.getBit()){
			throw new CrigpackException("Crigpack crashed while writing to disc your file is courpted");
		}
		curId = headerBuffer.getInt(28);
	}
	
	private long getMatchtingPosition(Block b){
		Block free = freeBlocks.higher(b);
		if(free != null){
			freeBlocks.remove(free);
			return free.position;
		}else{
			long temp = endOfFile;
			endOfFile += (b.blockSize + 24);
			return temp;
		}
	}
	
	private class Block implements Comparable<Block>{
		private Object content = null; 
		private long position;
		private MappedByteBuffer buffer;
		private Block prev, next = null;
		private long nextPos = 0;
		private int blockSize;
		private int id;
		
		private Block(){}
		
		private void initNew(Object o, int id){
			this.content = o;
			byte[] temp = serializer.asByteArray(o);
			blockSize = temp.length;
			this.position = getMatchtingPosition(this);
			try {
				buffer = fc.map(MapMode.READ_WRITE, position, blockSize + 24);
			} catch (IOException e) {
				System.out.println(e);
			}
			buffer.putInt(blockSize);
			setLastUniqueId(id);
			buffer.putInt(id);
			buffer.put(temp);
			setPrevBlock(last);
			if(last != null){
				last.setNextBlock(this);
			}
			last = this;
			incBlockCount();
			idToPosMap.put(id, this.position);
		}
		
		private Object readExisting(long pos, MappedByteBuffer buffer){
			this.blockSize = buffer.getInt();
			this.position = pos;
			this.buffer = buffer;
			this.id = buffer.getInt();
			byte[] temp = new byte[blockSize];
			buffer.get(temp);
			nextPos = buffer.getLong(buffer.position() + 8);
			return content;	
		}
		
		public final void delete(){
			buffer.clear();
			if(prev != null){
				prev.setNextBlock(next);
			}
			if(next != null){
				next.setPrevBlock(prev);
			}
			if(this == last){
				last = prev;
			}
			if(this == first){
				first = next;
			}
			decBlockCount();
		}
		
		private void setNextBlock(Block next){
			if(next != null){
				this.next = next;
				buffer.putLong(blockSize + 16, next.position);
				buffer.force();
			}else{
				this.next = null;
				buffer.putLong(blockSize + 16, 0);
				buffer.force();
			}
		}
		
		private void setPrevBlock(Block prev){
			if(prev != null){
				this.prev = prev;
				buffer.putLong(blockSize + 8, prev.position);
				buffer.force();
			}else{
				first = this;
				updateFirstBlockPos();
				this.prev = null;
				buffer.putLong(blockSize + 8, 0);
				buffer.force();
			}
		}

		@Override
		public int compareTo(Block o) {
			return this.blockSize - o.blockSize;
		}	
	}
	
	class SaveTask implements Callable<Void>{
		private Object toSave;
		private int id;
		
		public SaveTask(Object toSave, int id){
			this.toSave = toSave;
			this.id = id;
		}

		@Override
		public Void call() throws Exception {
			if(!blockMap.containsKey(toSave)){
				Block b = new Block();
				b.initNew(toSave, id);
				blockMap.put(toSave, b);
			}
			return null;
		}
	}
	
	class DeleteTask implements Callable<Void>{
		private Object toDelete;
		
		public DeleteTask(Object toDelete){
			this.toDelete = toDelete;
		}

		@Override
		public Void call() throws Exception {
			Block b = blockMap.get(toDelete);
			if(b != null){
				b.delete();
				b.blockSize--;
				freeBlocks.add(b);
				blockMap.remove(toDelete);
			}
			return null;
		}
	}
	
	class DeleteAtPosTask implements Callable<Void>{
		private long pos;
		
		public DeleteAtPosTask(long pos){
			this.pos = pos;
		}

		@Override
		public Void call() throws Exception {
			Block b = createBlockAtPos(pos);
			b.delete();
			b.blockSize--;
			freeBlocks.add(b);
			return null;
		}
	}
}
