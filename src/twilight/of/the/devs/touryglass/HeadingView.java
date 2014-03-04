package twilight.of.the.devs.touryglass;

import java.io.File;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.google.android.gms.location.LocationClient;

import twilight.of.the.devs.mylibrary.Marker;
import twilight.of.the.devs.utils.MathUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.PorterDuff.Mode;
import android.location.Location;
import android.text.DynamicLayout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

public class HeadingView extends View {

	private static final String TAG = HeadingView.class.getName();
	private float mHeading;
	private Paint mPaint;
	private static final float DESCRIPTION_TEXT_HEIGHT = 64.0f;
	private static final float PLACE_TEXT_HEIGHT = 26.0f;
	private TextPaint mTextPaint;
	private List<DynamicLayout> mTextLayout;
	private LiveCardRenderer mRenderer;
	private HashMap<DynamicLayout, Marker> mMarkerMap;
	private Location mCurrentLocation;
	private NumberFormat mNumberFormat;
	private Bitmap mPlaceBitmap;
	private DynamicLayout mDescLayout;
	private Marker mTriggeredMarker;
	private Paint mDescPaint;
	private TextPaint mTextPaint2;
	private float[] mDistance;
	
	public LiveCardRenderer getRenderer() {
		return mRenderer;
	}

	public void setRenderer(LiveCardRenderer mRenderer) {
		this.mRenderer = mRenderer;
	}

	public HeadingView(Context context) {
		super(context);
		setupUi(context);
	}
	
	public HeadingView(Context context, AttributeSet attrs, int defStyles) {
		super(context, attrs, defStyles);
		setupUi(context);
	}

	public HeadingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupUi(context);
	}
	
	private void setupUi(Context context){
		mPaint = new Paint();
    	mPaint.setColor(Color.WHITE);
    	mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(PLACE_TEXT_HEIGHT);
        mPaint.setTypeface(Typeface.createFromFile(new File("/system/glass_fonts",
                "Roboto-Thin.ttf")));
        mDescPaint = new Paint();
    	mDescPaint.setColor(Color.WHITE);
    	mDescPaint.setStyle(Paint.Style.FILL);
        mDescPaint.setAntiAlias(true);
        mDescPaint.setTextSize(DESCRIPTION_TEXT_HEIGHT);
        mDescPaint.setTypeface(Typeface.createFromFile(new File("/system/glass_fonts",
                "Roboto-Thin.ttf")));

        mTextPaint = new TextPaint(mPaint);
        mTextPaint2 = new TextPaint(mDescPaint);
        mTextLayout = new LinkedList<DynamicLayout>();
        mMarkerMap = new HashMap<DynamicLayout, Marker>();
        mNumberFormat = NumberFormat.getNumberInstance();
        mNumberFormat.setMaximumFractionDigits(2);
        mPlaceBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.place_mark);
        mTriggeredMarker = new Marker();
        mTriggeredMarker.setDescription("");
        mDistance = new float[]{0.0f};
        mDescLayout = new DynamicLayout(mTriggeredMarker.getDescription(), mTextPaint, 640, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        
	}
	
	public void setHeading(float head){
		mHeading = head;
		if(mHeading < 0) mHeading = 360 + mHeading;
	}
	
	public void setMarkerHeading(float heading){
	}
	
	public void setTriggeredMarker(Marker m){
		mTriggeredMarker = m;
		mDescLayout = new DynamicLayout(mTriggeredMarker.getDescription(), new TextPaint(mDescPaint), 640, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
	}
	
	public void setDescription(String desc){
	}
	
	public void setGeofence(List<Marker> list){
		if(list == null){ Log.d(TAG, "List is null"); return;}
		mMarkerMap = new HashMap<DynamicLayout, Marker>();

		for(Marker m : list){
			DynamicLayout d = new DynamicLayout(m.getTitle(), mTextPaint, 640, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
			mMarkerMap.put(d, m);
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(0, Mode.CLEAR);
		
		if(mMarkerMap.size()==0){
			canvas.drawText("Welcome to Toury", 20, canvas.getHeight()/2, mDescPaint);
		} else {
			
			if(mTriggeredMarker.getTitle()!= null && isWithinTenDegrees((int)mTriggeredMarker.getDirection(), (int)mHeading)){
				canvas.save();
				canvas.translate(20, 20);
				mDescLayout.draw(canvas);
				canvas.restore();
			}
	
			for(DynamicLayout d : mMarkerMap.keySet()){
				canvas.save();

				//Calculate bearing from my current location to the location of each marker.
				Location loc = new Location(mCurrentLocation);
				loc.setLatitude(mMarkerMap.get(d).getMarkerLatitude());
				loc.setLongitude(mMarkerMap.get(d).getMarkerLongitude());
				float bearing = mCurrentLocation.bearingTo(loc);

				if(bearing < 0) bearing = 360 + bearing;

				if((bearing - mHeading > 270))
					canvas.translate((int)((((bearing - mHeading)-360) / 90)*canvas.getWidth())+15 + canvas.getWidth()/2, 320);
				else
					canvas.translate((int)((((bearing - mHeading)) / 90)*canvas.getWidth())+15 + canvas.getWidth()/2, 320);

				if(mCurrentLocation != null){
					Location.distanceBetween(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), mMarkerMap.get(d).getMarkerLatitude(), mMarkerMap.get(d).getMarkerLongitude(), mDistance);
				}
				d.draw(canvas);
				canvas.restore();

				//Draw a fainter image for markers that are further away.
				int alpha = (int)((10 / mDistance[0])*255);
				alpha = alpha > 255 ? 255 : alpha;
				mPaint.setAlpha(alpha);
				if((bearing - mHeading > 270))
					canvas.drawBitmap(mPlaceBitmap, (int)((((bearing - mHeading)-360) / 90)*canvas.getWidth()) - 15 + canvas.getWidth()/2, 320, mPaint);		
				else
					canvas.drawBitmap(mPlaceBitmap, (int)((((bearing - mHeading)) / 90)*canvas.getWidth()) - 15 + canvas.getWidth()/2, 320, mPaint);
			}
		}
		//Debugging: Draws the current heading and location
		
//		canvas.drawText("Heading: " + mHeading, 20, 256, mPaint);
//		if(mCurrentLocation!= null)
//			canvas.drawText("Loc: " + mNumberFormat.format(mCurrentLocation.getLatitude()) + ", " + mNumberFormat.format(mCurrentLocation.getLongitude()), 20, 300, mPaint);
	}

	public Location getCurrentLocation() {
		return mCurrentLocation;
	}

	public void setCurrentLocation(Location mCurrentLocation) {
		this.mCurrentLocation = mCurrentLocation;
	}
	
	public boolean isWithinTenDegrees(int one, int two){
		return Math.abs(one - two) <= 10;
	}
}
