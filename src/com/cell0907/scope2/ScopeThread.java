/*
 * This will take data from a buffer and display it in the screen
 * It doesn't really control anything (like the speed of the sweep...), just does
 * the display...
 */
package com.cell0907.scope2;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Log;
import android.view.SurfaceHolder;

public class ScopeThread extends Thread {
    private int  mCanvasWidth;
    private int mCanvasHeight;
	private Q scope_buffer; 		// Circular buffer for the scope in the screen
	int[] intermediate_buffer;			// Will store the display
	private SurfaceHolder holder;
    private boolean running = true;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int refresh_rate=20; 	// How often we update the scope screen, in ms

    public ScopeThread(SurfaceHolder holder, Q scope_buffer) {
        this.holder = holder;
        this.scope_buffer=scope_buffer;
    	paint.setColor(Color.BLUE);
    	paint.setStyle(Style.STROKE);
    	paint.setTextSize(50);
    }

    @Override
    public void run() {
        long previousTime, currentTime;
    	previousTime = System.currentTimeMillis();
        Canvas canvas = null;
        while(running) {
            currentTime=System.currentTimeMillis();
            while ((currentTime-previousTime)<refresh_rate){
            	currentTime=System.currentTimeMillis();
            }
            previousTime=currentTime;
            // Paint right away, so, it is as smooth as can be...
            try {
                canvas = holder.lockCanvas();
                synchronized (holder) {
                	 draw(canvas);                  
                }
            }
            finally {
            	if (canvas != null) {
            		holder.unlockCanvasAndPost(canvas);
            		}
            }
            // Update the scope buffer with info from the audio buffer
			synchronized(scope_buffer){
		    	scope_buffer.set_r_pointer(scope_buffer.get_w_pointer()+1);
		    	intermediate_buffer=scope_buffer.get();	// Reads full buffer
			}
			try {
				Thread.sleep(refresh_rate-5); // Wait some time till I need to display again
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
        }
        Log.d("MyActivity", "Scope Thread stopped");
    }

    // Should be called every ~50ms
    private void draw(Canvas canvas)
    {
    	int current, previous=mCanvasHeight-10;
    	canvas.drawColor(Color.BLACK);
    	paint.setColor(Color.WHITE);
    	canvas.drawRect(new RectF(1,1,mCanvasWidth-1,mCanvasHeight-1), paint);
    	paint.setColor(Color.RED);
    	for(int x=1;x<mCanvasWidth-2;x++){
    		current=mCanvasHeight-intermediate_buffer[x]*(mCanvasHeight-11)/32767-10;
    		canvas.drawLine(x,previous,x+1,current,paint);
    		previous=current;
    	}
    }
    
    public void setRunning(boolean b) {
        running = b;
    }
    
    public void setSurfaceSize(int width, int height) {
	    synchronized (holder){// that we removed
	        mCanvasWidth = width;
	        mCanvasHeight = height;
	    }
	    intermediate_buffer=new int[mCanvasWidth];
    }
}
