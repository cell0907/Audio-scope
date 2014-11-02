/*
 * This is a thread that takes data from a queue and process it to create a new data value
 * that dumps on a new buffer.
 * In the case where the source is audio, it should come visit this source often enough to
 * avoid losing data. In this case, it is actually been notified by the thread that sources
 * the data.
 */
package com.cell0907.scope2;

import java.util.Arrays;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class ProcessorThread extends Thread {
	private Q source_buffer; 			// Where to get the samples from
	private Q destiny_buffer;			// Where to put the samples to
	
    private int speed=200;			    // Number of samples in one pixel
    private boolean running = true;
    
    private int[] reminder;
    
    public Handler mHandler;			// Processor handler
    public static final int DO_PROCESSING = 1;  // Message that the audio capture send to
    											// the processing thread.
        
	ProcessorThread(Q source, Q destiny){
        this.source_buffer=source;
        this.destiny_buffer=destiny;
        reminder=new int[0];
	}
	
	@Override
    public void run() {
		Looper.prepare();
		mHandler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				switch (msg.what)
				{
				case DO_PROCESSING:
					// GOT NEW DATA. ANALYZE
					int[] intermediate_buffer;
					int[] intermediate_buffer_2;
					synchronized(source_buffer){
						intermediate_buffer=source_buffer.get();
					}
					if (Scope.JNI==false) process(intermediate_buffer);
					else {
						intermediate_buffer_2=processjni(intermediate_buffer);
		        		synchronized(destiny_buffer){
		        			destiny_buffer.put(intermediate_buffer_2);
		        		}
					} 
					break;
				}
			} 
		};
		Looper.loop(); 
		while(running){ // This keeps the thread running waiting for a message
		}
		Log.d("MyActivity", "Processor Thread stopped");
    }
	
	private void process(int[] audio){
		int x=0;
		int maximum; 
		// speed is the number of original audio samples that form one
		// pixel in the screen. As long as we got enough for one, we write it
		// in.
		audio=concat(reminder,audio);
		int i=audio.length;
		while (i>=speed){
			maximum=0;
        	for (int j=0;j<speed;j++)
        		if (audio[x+j]>maximum) maximum=audio[x+j];
        		synchronized(destiny_buffer){
        			destiny_buffer.put(maximum);
        		}
        	x+=speed;
        	i-=speed;
		}
		if (x>0) x-=speed;
		reminder=Arrays.copyOf(reminder, i); // Resize reminder
		System.arraycopy(audio, x, reminder, 0, i); // Copy what was left
	}
	
	public static int[] concat(int[] first, int[] second) {
		  int[] result = Arrays.copyOf(first, first.length + second.length);
		  System.arraycopy(second, 0, result, first.length, second.length);
		  return result;
		}
		
    public void setRunning(boolean b) {
    	Log.d("MyActivity", "RUNNING "+b);
        running = b;
    }
    
	private native int[] processjni(int[] audio);
	static {
        System.loadLibrary("JNImodule");
    }    
}
