package de.crigges.stuff;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;

public class BlockReader{
	MappedByteBuffer buffer;
	int start;
	int length; 
	int pos = 0;

	public BlockReader(MappedByteBuffer buffer, int start, int length) {
		this.buffer = buffer;
		this.start = start;
		this.length = length;
		buffer.position(start);
	}

	public byte read() throws CrigpackException{
		if(pos + 1 <= length){
			pos++;
			return buffer.get();
		}else{
			throw new CrigpackException("End of block reached");
		}
	}
	
	public byte read(byte arr) throws CrigpackException{
		if(arr == null){
			throw new CrigpackException("")
		}
		if(pos + 1 <= length){
			pos++;
			return buffer.get();
		}else{
			throw new CrigpackException("End of block reached");
		}
	}
	
	public short readShort() throws CrigpackException{
		if(pos + 2 <= length){
			pos+=2;
			return buffer.getShort();
		}else{
			throw new CrigpackException("End of block reached");
		}
	}
	
	public int readInt() throws CrigpackException{
		if(pos + 4 <= length){
			pos+=4;
			return buffer.getInt();
		}else{
			throw new CrigpackException("End of block reached");
		}
	}
	
	public long readLong() throws CrigpackException{
		if(pos + 8 <= length){
			pos+=8;
			return buffer.getLong();
		}else{
			throw new CrigpackException("End of block reached");
		}
	}

	
	public float readFloat() throws CrigpackException{
		if(pos + 4 <= length){
			pos+=4;
			return buffer.getFloat();
		}else{
			throw new CrigpackException("End of block reached");
		}
	}
	
	public double readDouble() throws CrigpackException{
		if(pos + 8 <= length){
			pos+=8;
			return buffer.getDouble();
		}else{
			throw new CrigpackException("End of block reached");
		}
	}
	
	public String readString() throws CrigpackException{
		short s = readShort();
		if(pos + s <= length){
			byte[] arr = new byte[s];
			buffer.get(arr);
			return new String(arr);
		}else{
			throw new CrigpackException("End of block reached");
		}
	}

}
