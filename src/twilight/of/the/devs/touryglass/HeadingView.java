package twilight.of.the.devs.touryglass;

import java.io.File;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class HeadingView extends View {

	private static final String TAG = HeadingView.class.getName();
	private float mHeading;
	private Paint mPaint;
	private String mGeofence = "";
	private static final float DIRECTION_TEXT_HEIGHT = 64.0f;
	
	public HeadingView(Context context) {
		super(context);
		mPaint = new Paint();
    	mPaint.setColor(Color.WHITE);
    	//mPaint.setTextSize(32.0f);
    	mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(DIRECTION_TEXT_HEIGHT);
        mPaint.setTypeface(Typeface.createFromFile(new File("/system/glass_fonts",
                "Roboto-Thin.ttf")));
	}
	
	public HeadingView(Context context, AttributeSet attrs, int defStyles) {
		super(context, attrs, defStyles);
		mPaint = new Paint();
    	mPaint.setColor(Color.WHITE);
    	//mPaint.setTextSize(32.0f);
    	mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(DIRECTION_TEXT_HEIGHT);
        mPaint.setTypeface(Typeface.createFromFile(new File("/system/glass_fonts",
                "Roboto-Thin.ttf")));
	}

	public HeadingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mPaint = new Paint();
    	mPaint.setColor(Color.WHITE);
    	//mPaint.setTextSize(32.0f);
    	mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(DIRECTION_TEXT_HEIGHT);
        mPaint.setTypeface(Typeface.createFromFile(new File("/system/glass_fonts",
                "Roboto-Thin.ttf")));
	}
	
	public void setHeading(float head){
		mHeading = head;
	}
	
	public void setGeofence(String g){
		if(g == null) return;
		mGeofence = g;
		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(0, Mode.CLEAR);

		if(mGeofence.length() > 0){
			canvas.drawText(mGeofence.substring(0, 21), 20, 64, mPaint);
			canvas.drawText(mGeofence.substring(21), 20, 128, mPaint);
//			canvas.drawText(mGeofence.substring(42), 20, 192, mPaint);
			
		}
		canvas.drawText("Heading: " + mHeading, 20, 256, mPaint);
	}

}
