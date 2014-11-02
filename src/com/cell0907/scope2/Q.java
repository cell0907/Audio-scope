/*
 * CIRCULAR BUFFER
 */

package com.cell0907.scope2;

import java.util.Arrays;

public class Q {
	private int[] Data; 		// Circular buffer
	private int Buffer_length;	// How long the buffer is before it wraps around
	private int w_pointer;		// Position to the next element to write in the buffer 
	private int r_pointer;		// Position to the next element to read from the buffer
	
	Q(int length){
		Buffer_length=length;
		Data=new int[Buffer_length];
		w_pointer=0;
		r_pointer=Buffer_length-1;
	}
	
	int get_w_pointer(){
		return w_pointer;
	}
	
	void set_r_pointer(int pointer){
		if (pointer<0)
			this.r_pointer=Buffer_length+pointer;
		else if (pointer>=Buffer_length)
			this.r_pointer=pointer%Buffer_length;
		else
			this.r_pointer=pointer;
	}
	
	int get_r_pointer(){
		return r_pointer;
	}
	
	/*
	 * Places an amount of data in the buffer and returns true
	 * if there is a buffer over run (write pointer goes over the
	 * read pointer)
	 */
	public boolean put(int[] data_in){
		int i=0;
		boolean error=false;
		while (i<data_in.length)
		{
			if (w_pointer==r_pointer) error=true;
			Data[w_pointer]=data_in[i];
			i++;
			w_pointer++;
			if (w_pointer>Buffer_length-1) w_pointer=0;	
		}
		return error;
	}
	
	/*
	 * Places an single element of data in the buffer and returns true
	 * if there is a buffer over run (write pointer goes over the
	 * read pointer)
	 */	
	public boolean put(int data_in){
		boolean error=false;
		if (w_pointer==r_pointer) error=true;
		Data[w_pointer]=data_in;
		w_pointer++;
		if (w_pointer>Buffer_length-1) w_pointer=0;
		return error;
	}
	
	/*
	 * Returns all the data available in the buffer,
	 * basically, from r_pointer to w_pointer at the time of the call.
	 */
	public int[] get(){
		int[] data_out=new int[Buffer_length];
		int i=0;	
		while (r_pointer!=w_pointer) // Reads till the end of the buffer
		{
			data_out[i]=Data[r_pointer];
			i++;
			r_pointer++;
			if (r_pointer>Buffer_length-1) r_pointer=0;
		}
		return Arrays.copyOf(data_out,i);
	}
	
	// Reads n elements or none. In the 2nd case, it will still return whatever could 
	// read but will not move the r_pointer
	public int[] get(int n){
		int[] data_out=new int[Buffer_length];
		int i=0;
		int r_pointer_backup = r_pointer;
		while ((r_pointer!=w_pointer) & (i<n))	// Reads till the end of the buffer
												// or till we get n elements
		{
			data_out[i]=Data[r_pointer];
			i++;
			r_pointer++;
			if (r_pointer>Buffer_length-1) r_pointer=0;
		}
		if (i<n) r_pointer=r_pointer_backup; // As you couldn't read the whole array, go back
		return Arrays.copyOf(data_out,i);
	}
	
	// Reads one element.
	public int getone(){
		int i=Data[r_pointer];
		r_pointer++;
		if (r_pointer>Buffer_length-1) r_pointer=0;
		return i;
	}
}