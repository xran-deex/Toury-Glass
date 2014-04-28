package twilight.of.the.devs.touryglass.provider;

import java.util.HashMap;

import twilight.of.the.devs.touryglass.provider.TouryProvider.TouryProviderMetaData.MarkersTableMetaData;
import twilight.of.the.devs.touryglass.provider.TouryProvider.TouryProviderMetaData.ToursTableMetaData;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/*
 * Stores tours and markers in the database
 */
public class TouryProvider extends ContentProvider {
	
	public static class TouryProviderMetaData {

		public static final String AUTHORITY = "twilight.of.the.devs.touryglass.provider.TouryProvider";
		
		public static final String DATABASE_NAME = "toury.db";
		public static final int DATABASE_VERSION = 5;
		public static final String MARKERS_TABLE_NAME = "markers";
		
		private TouryProviderMetaData() {}
		
		
		
		public final static class MarkersTableMetaData implements BaseColumns {
			
			private MarkersTableMetaData(){}
			
			public static final String TABLE_NAME = "markers";
			
//			public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/markers");
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.toury.marker";
			public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.toury.marker";
			
			//Column names
			public static final String DEFAULT_SORT_ORDER = "_id DESC";
			public static final String DESCRIPTION = "description";
			public static final String TRIGGER_LATITUDE = "trigger_latitude";
			public static final String TRIGGER_LONGITUDE = "trigger_longitude";
			public static final String MARKER_LATITUDE = "marker_latitude";
			public static final String MARKER_LONGITUDE = "marker_longitude";
			public static final String DIRECTION = "direction";
			public static final String RADIUS = "radius";
			public static final String ORDER = "ordering";
			public static final String TITLE = "title";
			public static final String TOUR_ID = "tour_id";
			public static final String UNSYNCED = "unsynced";
			
		}
		
		public final static class ToursTableMetaData implements BaseColumns {
			
			private ToursTableMetaData(){}
			
			public static final String TABLE_NAME = "tours";
			
			public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
			public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.toury.tour";
			public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.toury.tour";
			
			//Column names
			public static final String DEFAULT_SORT_ORDER = "_id DESC";
			public static final String NAME = "name";
			public static final String UNSYNCED = "unsynced";
			
		}
		
	}
	
	private static final String TAG = TouryProvider.class.getName();
	
	private static HashMap<String, String> sMarkersProjectionMap;
	private static HashMap<String, String> sToursProjectionMap;
	
	static {
		sMarkersProjectionMap = new HashMap<String, String>();
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData._ID, TouryProviderMetaData.MarkersTableMetaData._ID);
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData.TITLE, TouryProviderMetaData.MarkersTableMetaData.TITLE);
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData.RADIUS, TouryProviderMetaData.MarkersTableMetaData.RADIUS);
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData.ORDER, TouryProviderMetaData.MarkersTableMetaData.ORDER);
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData.TRIGGER_LATITUDE, TouryProviderMetaData.MarkersTableMetaData.TRIGGER_LATITUDE);
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData.TRIGGER_LONGITUDE, TouryProviderMetaData.MarkersTableMetaData.TRIGGER_LONGITUDE);
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData.MARKER_LATITUDE, TouryProviderMetaData.MarkersTableMetaData.MARKER_LATITUDE);
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData.MARKER_LONGITUDE, TouryProviderMetaData.MarkersTableMetaData.MARKER_LONGITUDE);
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData.DIRECTION, TouryProviderMetaData.MarkersTableMetaData.DIRECTION);
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData.DESCRIPTION, TouryProviderMetaData.MarkersTableMetaData.DESCRIPTION);
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData.TOUR_ID, TouryProviderMetaData.MarkersTableMetaData.TOUR_ID);
		sMarkersProjectionMap.put(TouryProviderMetaData.MarkersTableMetaData.UNSYNCED, TouryProviderMetaData.MarkersTableMetaData.UNSYNCED);
		sToursProjectionMap = new HashMap<String, String>();
		sToursProjectionMap.put(TouryProviderMetaData.ToursTableMetaData._ID, ToursTableMetaData._ID);
		sToursProjectionMap.put(ToursTableMetaData.NAME, ToursTableMetaData.NAME);
		sToursProjectionMap.put(ToursTableMetaData.UNSYNCED, ToursTableMetaData.UNSYNCED);
		
	}
	
	private static final UriMatcher sUriMatcher;
	private static final int TOUR = 1;
	private static final int TOUR_ID = 2;
	private static final int MARKERS = 3;
	private static final int MARKER_ID = 4;
	private static final int ALL_MARKERS = 5;
	private static final int ALL_MARKERS_ID = 6;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(TouryProviderMetaData.AUTHORITY, "tour", TOUR);
		sUriMatcher.addURI(TouryProviderMetaData.AUTHORITY, "tour/#", TOUR_ID);
		sUriMatcher.addURI(TouryProviderMetaData.AUTHORITY, "tour/#/markers", MARKERS);
		sUriMatcher.addURI(TouryProviderMetaData.AUTHORITY, "tour/#/markers/#", MARKER_ID);
		sUriMatcher.addURI(TouryProviderMetaData.AUTHORITY, "markers", ALL_MARKERS);
		sUriMatcher.addURI(TouryProviderMetaData.AUTHORITY, "markers/#", ALL_MARKERS_ID);
	}
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, TouryProviderMetaData.DATABASE_NAME, null, TouryProviderMetaData.DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TouryProviderMetaData.MarkersTableMetaData.TABLE_NAME + " ("
					+ TouryProviderMetaData.MarkersTableMetaData._ID + " INTEGER PRIMARY KEY, "
					+ TouryProviderMetaData.MarkersTableMetaData.TITLE + " TEXT, "
					+ TouryProviderMetaData.MarkersTableMetaData.DESCRIPTION + " TEXT, "
					+ TouryProviderMetaData.MarkersTableMetaData.RADIUS + " REAL, "
					+ TouryProviderMetaData.MarkersTableMetaData.ORDER + " INTEGER, "
					+ TouryProviderMetaData.MarkersTableMetaData.DIRECTION + " REAL, "
					+ TouryProviderMetaData.MarkersTableMetaData.TRIGGER_LONGITUDE + " REAL, "
					+ TouryProviderMetaData.MarkersTableMetaData.TRIGGER_LATITUDE + " REAL, "
					+ TouryProviderMetaData.MarkersTableMetaData.MARKER_LONGITUDE + " REAL, "
					+ TouryProviderMetaData.MarkersTableMetaData.MARKER_LATITUDE + " REAL, "
					+ TouryProviderMetaData.MarkersTableMetaData.TOUR_ID + " INTEGER, "
					+ TouryProviderMetaData.MarkersTableMetaData.UNSYNCED + " INTEGER, "
					+ " FOREIGN KEY (" + TouryProviderMetaData.MarkersTableMetaData.TOUR_ID + ") REFERENCES "
					+ TouryProviderMetaData.ToursTableMetaData.TABLE_NAME + "(" + TouryProviderMetaData.ToursTableMetaData._ID + ")"
					+ ");");
			
			db.execSQL("CREATE TABLE " + TouryProviderMetaData.ToursTableMetaData.TABLE_NAME + " ("
					+ TouryProviderMetaData.ToursTableMetaData._ID + " INTEGER PRIMARY KEY, "
					+ TouryProviderMetaData.ToursTableMetaData.UNSYNCED + " INTEGER, "
					+ TouryProviderMetaData.ToursTableMetaData.NAME + " TEXT);"
					);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TouryProviderMetaData.ToursTableMetaData.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TouryProviderMetaData.MarkersTableMetaData.TABLE_NAME);
			onCreate(db);
		}
	}
	
	private DatabaseHelper mOpenHelper;

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch(sUriMatcher.match(uri)){
		case TOUR:
			count = db.delete(ToursTableMetaData.TABLE_NAME, where, whereArgs);
			break;
		case TOUR_ID:
			String rowId = uri.getPathSegments().get(1);
			count = db.delete(ToursTableMetaData.TABLE_NAME, ToursTableMetaData._ID + "=" + rowId
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case MARKERS:
			String rowId2 = uri.getPathSegments().get(1);
			count = db.delete(MarkersTableMetaData.TABLE_NAME, MarkersTableMetaData.TOUR_ID + "=" + rowId2
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case MARKER_ID:
			String rowId3 = uri.getPathSegments().get(1);
			String rowId4 = uri.getPathSegments().get(3);
			count = db.delete(MarkersTableMetaData.TABLE_NAME, MarkersTableMetaData.TOUR_ID + "=" + rowId3 + " AND " + MarkersTableMetaData._ID + "=" + rowId4
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case TOUR:
			return TouryProviderMetaData.MarkersTableMetaData.CONTENT_TYPE;
		case TOUR_ID:
			return TouryProviderMetaData.MarkersTableMetaData.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		if(sUriMatcher.match(uri) != TOUR && sUriMatcher.match(uri) != MARKERS) throw new IllegalArgumentException("Unknown URI " + uri);
		
		ContentValues values;
		if(initialValues != null){
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId = 0;
		switch(sUriMatcher.match(uri)){
		case TOUR:
			rowId = db.insert(TouryProviderMetaData.ToursTableMetaData.TABLE_NAME, TouryProviderMetaData.ToursTableMetaData.NAME, values);
			if(rowId > 0){
				Uri insertedUri = ContentUris.withAppendedId(TouryProviderMetaData.ToursTableMetaData.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(insertedUri, null);
				return insertedUri;
			}
			break;
		case MARKERS:
			rowId = db.insert(MarkersTableMetaData.TABLE_NAME, MarkersTableMetaData.TITLE, values);
			if(rowId > 0){
				Uri insertedUri = Uri.withAppendedPath(TouryProviderMetaData.ToursTableMetaData.CONTENT_URI, "tour/" + uri.getPathSegments().get(1) + "/marker/" + rowId);
				getContext().getContentResolver().notifyChange(insertedUri, null);
				return insertedUri;
			}
			break;
		}

		
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
//		Log.d(TAG, uri.toString());
		switch(sUriMatcher.match(uri)){
		case TOUR:
			qb.setTables(ToursTableMetaData.TABLE_NAME);
			qb.setProjectionMap(sToursProjectionMap);
			break;
		case TOUR_ID:
			qb.setTables(ToursTableMetaData.TABLE_NAME);
			qb.setProjectionMap(sToursProjectionMap);
			qb.appendWhere(TouryProviderMetaData.MarkersTableMetaData._ID + "=" + uri.getPathSegments().get(1));
			break;
		case MARKERS:
            qb.setTables(MarkersTableMetaData.TABLE_NAME);
            qb.appendWhere(MarkersTableMetaData.TOUR_ID + "=" + uri.getPathSegments().get(1));
            qb.setProjectionMap(sMarkersProjectionMap);
            break;
        case MARKER_ID:
            qb.setTables(MarkersTableMetaData.TABLE_NAME);
            qb.appendWhere(MarkersTableMetaData.TOUR_ID + "=" + uri.getPathSegments().get(1));
            qb.appendWhere(MarkersTableMetaData._ID + uri.getPathSegments().get(3));
            break;
        case ALL_MARKERS:
            qb.setTables(MarkersTableMetaData.TABLE_NAME);
            qb.setProjectionMap(sMarkersProjectionMap);
            break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		String orderBy;
		if(TextUtils.isEmpty(sortOrder)){
			orderBy = TouryProviderMetaData.MarkersTableMetaData.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}
		
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		//int i = c.getCount();
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch(sUriMatcher.match(uri)){
		case TOUR:
			count = db.update(ToursTableMetaData.TABLE_NAME, values, where, whereArgs);
			break;
		case TOUR_ID:
			String rowId = uri.getPathSegments().get(1);
			count = db.update(ToursTableMetaData.TABLE_NAME, values, ToursTableMetaData._ID + "=" + rowId
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case MARKERS:
			
			count = db.update(MarkersTableMetaData.TABLE_NAME, values, where, whereArgs);
			break;
		case MARKER_ID:
			String rowId2 = uri.getPathSegments().get(1);
			String rowId3 = uri.getPathSegments().get(3);
			count = db.update(MarkersTableMetaData.TABLE_NAME, values, MarkersTableMetaData.TOUR_ID + "=" + rowId2 + " AND " + MarkersTableMetaData._ID + "=" + rowId3
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		case ALL_MARKERS:
			count = db.update(MarkersTableMetaData.TABLE_NAME, values, where, whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}
