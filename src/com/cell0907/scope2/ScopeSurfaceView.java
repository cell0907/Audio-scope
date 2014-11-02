package com.cell0907.scope2;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

// We extend SurfaceView. Internally (private) SurfaceView creates an object SurfaceHolder
// effectively defining the methods of the SurfaceHolder interface. Notice that it does
// not create a new class or anything, it just defines it right there. When we extend
// the SurfaceView with the SurfaceHolder.Callback interface, we need to add in that extension
// the methods of that interface.

public class ScopeSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder holder;	// This is no instantiation. Just saying that holder
									// will be of a class implementing SurfaceHolder
	private ScopeThread ScopeThread;// The thread that displays the data
	public ProcessorThread ProcessorThread; // The thread that reads audio and creates the
									// scope samples
	private Q source_buffer;		// Audio data
	private Q scope_buffer=null; 	// Buffer for the screen of the scope

	public ScopeSurfaceView(Context context){
		super(context);
	}
	
	public ScopeSurfaceView(Context context, Q source) {
		super(context);
		source_buffer=source;		// Where to get the samples to display
		holder = getHolder();		// Holder is now the internal/private mSurfaceHolder object 
									// in the SurfaceView object, which is from an anonymous
									// class implementing SurfaceHolder interface.
		holder.addCallback(this);
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}
	
	@Override
	// This is always called at least once, after surfaceCreated
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    	if (scope_buffer==null){
    		scope_buffer=new Q(width);    		
			ScopeThread = new ScopeThread(holder,scope_buffer);
			ScopeThread.setRunning(true);
			ScopeThread.setSurfaceSize(width, height);
			ScopeThread.start();
			Log.v("MyActivity","Screen width "+width);
			ProcessorThread = new ProcessorThread(source_buffer, scope_buffer);
			ProcessorThread.setRunning(true);
			ProcessorThread.start();
    	}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("MyActivity", "DESTROY SURFACE");
		boolean retry = true;
		ScopeThread.setRunning(false);
		while (retry) {
			try {
				ScopeThread.join();
				retry = false;
			} catch (InterruptedException e) {}
		}
		retry = true;
		Log.d("MyActivity", "Going to stop the processor thread");
		ProcessorThread.setRunning(false);
		while (retry) {
			try {
				ProcessorThread.mHandler.getLooper().quit();
				ProcessorThread.join();
				retry = false;
				Log.d("MyActivity", "FAIL");
			} catch (InterruptedException e) {
				Log.d("MyActivity", "FULL FAIL");
			}
		}
    }
	
    public Thread getThread() {
        return ScopeThread;
    }
}