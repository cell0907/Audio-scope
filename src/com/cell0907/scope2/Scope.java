/*
 * We use AudioRecording library to grab data from the audio buffer. The key call is
 * AR.ReadBytes(number of bytes) which stays there till the buffer is full (so, we better
 * put this in a separate thread), and the data has to be read immediately or it will 
 * get overwritten. AR object can be part of this thread, as anyhow, the thread is created only
 * once and lives through the whole application life. The output of this thread should be
 * written to a buffer or queue. Classical problem explained here:
 * http://www.tutorialspoint.com/java/java_thread_communication.htm
 * 
 * Notice that starting the AR is something that goes on its own, nothing to do with us
 * reading its output inside the thread and putting it in a buffer. I.e., the read operation
 * got to be in a thread because it freezes till it comes back (as explained above).
 *  
 * This is all similar to other "Key" examples. But now we do something a bit different.
 * The application launches a thread for the audio capture (with the AR), as explained
 * above. SurfaceView is the one launching the processing thread. It does that because it
 * needs to pass to it the destiny buffer, which length is dependent on the size of the
 * screen, and it is set at SurfaceChange time.
 * 
 * Nevertheless, this thread doesn't really do anything till a message from the audio
 * capture thread tells it to go and check if it can do anything with that data. The
 * message basically tells it how much new data it added to the audio buffer.
 * 
 * Once the processor feels it has enough data to do something, it'll read the data
 * and call/pass it to the JNI routine for analysis OR will do it directly in java.
 * Both methods are equivalent but they are there for illustration. Whatever it is,
 * it will return what needs to be plotted and also the data to keep that didn't
 * process to put it together with the next chunk of data and keep analyzing.
 * 
 */
package com.cell0907.scope2;

import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.app.Activity;


public class Scope extends Activity {
	public static final boolean JNI=true; // Tells us if we want to use JNI C function or java
	// RECORDING VARIABLES
	private AudioRecord AR=null;
	public int BufferSize;				// Length of the chunks read from the hardware audio buffer
	private Thread Record_Thread=null;  // The thread filling up the audio buffer (queue)
	private boolean isRecording = false;
	public Q audio_buffer=new Q(20000);  // Record_Thread read the AR and puts it in here.
	private static final int AUDIO_SOURCE=android.media.MediaRecorder.AudioSource.MIC;
	public static final int SAMPLE_RATE = 44100;
	private static final int CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO;
	private static final int AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT;	
	private ScopeSurfaceView scope_screen_view;
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MyActivity", "onCreate");
    }
	
    @Override
    public void onPause(){
    	Log.d("MyActivity", "onPause");
		// TODO Auto-generated method stub
		if (null != AR) {
	        isRecording = false;
	        boolean retry = true;
	        while (retry) {
	        	try {
	        		Record_Thread.join();
	        		retry = false;
	        	} catch (InterruptedException e) {}      
	        }
	        AR.stop();
	        AR.release();
	        AR = null;
	        //recordingThread = null;
		}
		scope_screen_view.surfaceDestroyed(scope_screen_view.getHolder());
    	super.onPause();
    }
	
	protected void onResume(){
		super.onResume();
		Log.d("MyActivity", "onResume");
		scope_screen_view=new ScopeSurfaceView(this,audio_buffer);
		setContentView(scope_screen_view);
		BufferSize=AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);			
		isRecording=true;
		Record_Thread=new Thread(new Runnable() {
	        public void run() {
	            Capture_Audio();
	        }
	    },"AudioRecord Thread");
		Record_Thread.start();
	}
		
	/*
	 * This runs in a separate thread reading the data from the AR buffer and dumping it
	 * into the queue (circular buffer) for processing (in java or C).
	 */
	public void Capture_Audio(){
		byte[] AudioBytes=new byte[BufferSize]; //Array containing the audio data bytes
		int[] AudioData=new int[BufferSize/2];  //Array containing the audio samples
		try {
			AR = new AudioRecord(AUDIO_SOURCE,SAMPLE_RATE,CHANNEL_CONFIG,AUDIO_FORMAT,BufferSize);
			try {
				AR.startRecording();
			} catch (IllegalStateException e){
				System.out.println("This didn't work very well");
				return;
				}
			} catch(IllegalArgumentException e){
				System.out.println("This didn't work very well");
				return;
				}
		while (isRecording)
		{
			AR.read(AudioBytes, 0, BufferSize); // This is the guy reading the bytes out of the buffer!!
			//First we will pass the 2 bytes into one sample 
			//It's an extra loop but avoids repeating the same sum many times later
			//during the filter
			int r=0;	
			for (int i=0; i<AudioBytes.length-2;i+=2)
			{// Before the 8 we had the end of the previous data
				if (AudioBytes[i]<0) 
					AudioData[r]=AudioBytes[i]+256; 
				else 
					AudioData[r]=AudioBytes[i];
				AudioData[r]=AudioData[r]+256*AudioBytes[i+1];	
				r++;
			}				
			// Write on the QUEUE		
			synchronized(audio_buffer){
				audio_buffer.put(AudioData);
			}	
			// Not a very pretty way to hit the handler (declaring everything public)
			// but just for the sake of demo.
			Message.obtain(scope_screen_view.ProcessorThread.mHandler, 
					scope_screen_view.ProcessorThread.DO_PROCESSING, "").sendToTarget();
		}
		Log.d("MyActivity", "Record_Thread stopped");
	}
	    	
    @Override
	protected void onStop() {
		super.onStop();
    } 
}
