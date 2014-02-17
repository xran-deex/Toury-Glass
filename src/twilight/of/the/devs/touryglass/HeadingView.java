package twilight.of.the.devs.touryglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class HeadingView extends View {

	private static final String TAG = HeadingView.class.getName();
	private float mHeading;
	private Paint mPaint;
	
	public HeadingView(Context context) {
		super(context);
		mPaint = new Paint();
    	mPaint.setColor(Color.WHITE);
    	mPaint.setTextSize(32.0f);
	}
	
	public HeadingView(Context context, AttributeSet attrs, int defStyles) {
		super(context, attrs, defStyles);
		mPaint = new Paint();
    	mPaint.setColor(Color.WHITE);
    	mPaint.setTextSize(32.0f);
	}

	public HeadingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mPaint = new Paint();
    	mPaint.setColor(Color.WHITE);
    	mPaint.setTextSize(32.0f);
	}
	
	public void setHeading(float head){
		mHeading = head;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		//super.onDraw(canvas);
		canvas.drawColor(0, Mode.CLEAR);
    	//Log.d(TAG, "Heading: " + mHeading);
		canvas.drawText("Current Heading: " + mHeading, 20, 40, mPaint);
		
	}

}
