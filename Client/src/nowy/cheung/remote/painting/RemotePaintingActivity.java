package nowy.cheung.remote.painting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

public class RemotePaintingActivity extends Activity implements ColorPickerDialog.OnColorChangedListener {

	static final String HOST = "192.168.1.106";
	static final int PORT = 3666;
	
	Thread myThread = null;
	Boolean runThread = false;
	Socket s;
	String serverX ="0",serverY="0",serverMove; 
	//
    static final float TOUCH_TOLERANCE = 4;
    int currentColor = 0xFFFF0000;

    float mX, mY,x,y;
    Path mPath;
    Canvas mCanvas;
    
    View myView = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myView = new MyView(this);
        
        setContentView(myView);
        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(currentColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);

        mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 }, 0.4f, 6, 3.5f);

        mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
		
        try {
			s = new Socket(HOST,PORT);
			s.setKeepAlive(true);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		
        myThread = new Thread(new Runnable() {
        	public void run() {
		    	String result;
				try {
					BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
		            while(true){
		            	if(!runThread)
		            		break;
		            	result = input.readLine();
		            	serverMove = result.charAt(0)+"";
		            	serverX = result.substring(2,result.indexOf(" - "));
		            	serverY = result.substring(result.indexOf("-")+2,result.indexOf("#"));
		            	colorChanged(result.substring(result.indexOf("#")+2));
		            	Log.d("Socket listen ", serverMove+","+serverX+","+serverY);
		            	
		            	if(serverMove.indexOf("D")==0){
		            		Log.d("CURRENT MOVE",serverMove);
		                    touch_start(serverX,serverY);
		                    myView.postInvalidate();
		            	}
		            	if(serverMove.indexOf("M")==0){
		            		Log.d("CURRENT MOVE",serverMove);
		                    touch_move(serverX,serverY);
		                    myView.postInvalidate();
		            	}

		            	if(serverMove.indexOf("U")==0){
		            		Log.d("CURRENT MOVE",serverMove);
		            		touch_up();
		                    myView.postInvalidate();
		            	}
		            }
		            //input.close();
		            //s.close();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Log.d("Socket test","Finish listen");
	        }
	   });
    }

    @Override
    protected void onStop(){
    	//myThread.stop();
    	runThread = false;
    	super.onStop();
    }
    @Override
    protected void onPause(){
    	runThread = false;
        myThread.stop();
    	super.onPause();
    }
    @Override
    protected void onResume(){
    	runThread = true;
        myThread.start();
    	super.onResume();
    }
    
    private Paint       mPaint;
    private MaskFilter  mEmboss;
    private MaskFilter  mBlur;
    public void colorChanged(int color) {
    	currentColor = color;
        mPaint.setColor(currentColor);
    }

    public void colorChanged(String color) {
    	int c = 0;
    	try{
    		c = Integer.parseInt(color);
    		colorChanged(c);
    	}catch(Exception e){
    		colorChanged(currentColor);
    	}
    }
    
    public class MyView extends View {

        private static final float MINP = 0.25f;
        private static final float MAXP = 0.75f;

        private Bitmap  mBitmap;
        private Paint   mBitmapPaint;

        public MyView(Context c) {
            super(c);
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xFFFFFFFF);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(mPath, mPaint);
            Log.d("REMOTE PAINTING","onDraw");
        }


        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sendData("D",x,y);
                    //touch_start(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    sendData("M",x,y);
                    //touch_move(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    sendData("U",x,y);
                    break;
            }
            //invalidate();
            return true;            
        }

    }

    void touch_up() {
        mPath.lineTo(mX, mY);
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        // kill this so we don't double draw
        mPath.reset();
    }
    
    private void touch_start(String _x, String _y) {
    	x =  Float.valueOf(_x.trim()).floatValue();
    	y =  Float.valueOf(_y.trim()).floatValue();
    	
    	mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }
    
    private void touch_move(String _x, String _y) {
    	try{
        	x = Float.valueOf(_x.trim()).floatValue();
        	y = Float.valueOf(_y.trim()).floatValue();
    	}catch(Exception e){
    		Log.e("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE", e.toString());
    		x = mX;
    		y = mY;
    	}
    	
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }
    
    private static final int COLOR_MENU_ID = Menu.FIRST;
    private static final int EMBOSS_MENU_ID = Menu.FIRST + 1;
    private static final int BLUR_MENU_ID = Menu.FIRST + 2;
    private static final int ERASE_MENU_ID = Menu.FIRST + 3;
    private static final int SRCATOP_MENU_ID = Menu.FIRST + 4;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, COLOR_MENU_ID, 0, "Color").setShortcut('3', 'c');
        menu.add(0, EMBOSS_MENU_ID, 0, "Emboss").setShortcut('4', 's');
        menu.add(0, BLUR_MENU_ID, 0, "Blur").setShortcut('5', 'z');
        menu.add(0, ERASE_MENU_ID, 0, "Erase").setShortcut('5', 'z');
        menu.add(0, SRCATOP_MENU_ID, 0, "SrcATop").setShortcut('5', 'z');
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xFF);

        switch (item.getItemId()) {
            case COLOR_MENU_ID:
                new ColorPickerDialog(this, this, mPaint.getColor()).show();
                return true;
            case EMBOSS_MENU_ID:
                if (mPaint.getMaskFilter() != mEmboss) {
                    mPaint.setMaskFilter(mEmboss);
                } else {
                    mPaint.setMaskFilter(null);
                }
                return true;
            case BLUR_MENU_ID:
                if (mPaint.getMaskFilter() != mBlur) {
                    mPaint.setMaskFilter(mBlur);
                } else {
                    mPaint.setMaskFilter(null);
                }
                return true;
            case ERASE_MENU_ID:
                mPaint.setXfermode(new PorterDuffXfermode(
                                                        PorterDuff.Mode.CLEAR));
                return true;
            case SRCATOP_MENU_ID:
                mPaint.setXfermode(new PorterDuffXfermode(
                                                    PorterDuff.Mode.SRC_ATOP));
                mPaint.setAlpha(0x80);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void sendData(final String move,float x, float y){
    	//Socket s = null;
		try {
			//s = new Socket(HOST,PORT);
        PrintWriter output = new PrintWriter(s.getOutputStream());
			        output.println(move+","+x+" - "+y+" # "+currentColor);
			        output.flush(); 
			        //output.close();
	    
	    //s.close();	
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}