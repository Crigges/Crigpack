package de.crigges.stuff;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class BlockBuilder {
	private static final int inititalCap = 16384;
	private static final int resizeCap = 4096;
	
	private static ByteBuffer content = ByteBuffer.allocate(inititalCap);
	private static int pos = 0;
	
	protected BlockBuilder() {
		content.clear();
	}
	
	private void resize(){
		ByteBuffer temp = content;
		content = ByteBuffer.allocate(temp.capacity() + resizeCap);
		content.put(temp.array());
	}
	
	protected void writeToMappedByteBuffer(){
		
	}

	public void put(byte b) throws CrigpackException{
		try{
			content.put(b);
		}catch(BufferOverflowException e){
			resize();
			put(b);
		}
	}
	
	public void put(byte[] arr) throws CrigpackException{
		if(arr == null){
			throw new CrigpackException("The given array was null");
		}
		try{
			content.put(arr);
		}catch(BufferOverflowException e){
			resize();
			put(arr);
		}
	}
	
	public void putShort(short s) throws CrigpackException{
		try{
			content.putShort(s);
		}catch(BufferOverflowException e){
			resize();
			putShort(s);
		}
	}
	
	public void putInt(int i) throws CrigpackException{
		try{
			content.putInt(i);
		}catch(BufferOverflowException e){
			resize();
			putInt(i);
		}
	}
	
	public void putLong(long l) throws CrigpackException{
		try{
			content.putLong(l);
		}catch(BufferOverflowException e){
			resize();
			putLong(l);
		}
	}

	
	public void putFloat(float f) throws CrigpackException{
		try{
			content.putFloat(f);
		}catch(BufferOverflowException e){
			resize();
			putFloat(f);
		}
	}
	
	public void putDouble(double d) throws CrigpackException{
		try{
			content.putDouble(d);
		}catch(BufferOverflowException e){
			resize();
			putDouble(d);
		}
	}
	
	public void putString(String s) throws CrigpackException{
		putShort((short) s.length());
		put(s.getBytes());
	}

}
