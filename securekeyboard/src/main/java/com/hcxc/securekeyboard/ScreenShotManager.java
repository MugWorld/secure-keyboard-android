package com.hcxc.securekeyboard;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;


public class ScreenShotManager {
    private static final String TAG = "ScreenShotUtil";
    private Context mContext;
    private ArrayList<String> shotTemp= new ArrayList();
    public ScreenShotManager() {
    }

    /**
     * 读取媒体数据库时需要读取的列
     */
    private static final String[] MEDIA_PROJECTIONS = {
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
    };
    private static final String[] MEDIA_PROJECTIONS_API_16 = {
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.ImageColumns.DATE_ADDED,
            MediaStore.Images.ImageColumns.DATE_MODIFIED
    };

    /**
     * 内部存储器内容观察者
     */
    private ContentObserver mInternalObserver;

    /**
     * 外部存储器内容观察者
     */
    private ContentObserver mExternalObserver;

    private HandlerThread mHandlerThread;
    private Handler mHandler;


    private ScreenShotManager mScreenShotUtil;

    private OnScreenShotListener mListener;

    public void init(Context context) {
        mContext = context;
        mHandlerThread = new HandlerThread("Screenshot_Observer");
        mHandlerThread.start();
        mHandler = new Handler(Looper.getMainLooper());

        // 初始化
        mInternalObserver = new MediaContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, mHandler);
        mExternalObserver = new MediaContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mHandler);

        // 添加监听
        mContext.getContentResolver().registerContentObserver(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                Build.VERSION.SDK_INT > Build.VERSION_CODES.P,
                mInternalObserver
        );
        mContext.getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                Build.VERSION.SDK_INT > Build.VERSION_CODES.P,
                mExternalObserver
        );
    }

    public interface OnScreenShotListener {
        void onShot(String imagePath);
    }

    /**
     * 设置截屏监听器
     */
    public void setListener(OnScreenShotListener listener) {
        mListener = listener;
    }


    private void onDestroy() {
        // 注销监听
        mContext.getContentResolver().unregisterContentObserver(mInternalObserver);
        mContext.getContentResolver().unregisterContentObserver(mExternalObserver);
    }

    private static final String[] KEYWORDS = {
            "screenshot", "screen_shot", "screen-shot", "screen shot",
            "screencapture", "screen_capture", "screen-capture", "screen capture",
            "screencap", "screen_cap", "screen-cap", "screen cap","screenshots"

    };

    private void handleMediaContentChange(Uri contentUri) {
        Cursor cursor = null;
        try {
            // 数据改变时查询数据库中最后加入的一条数据
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                cursor = mContext.getContentResolver().query(
                        contentUri,
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN ? MEDIA_PROJECTIONS : MEDIA_PROJECTIONS_API_16,
                        null,
                        null,
                        MediaStore.Images.ImageColumns.DATE_ADDED + " desc limit 1"
//                    MediaStore.Images.ImageColumns.DATE_ADDED + " desc"
                );
            } else {
                Bundle queryArgs = new Bundle();
                queryArgs.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, new String[]{MediaStore.Files.FileColumns.DATE_ADDED});
                queryArgs.putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
                queryArgs.putInt(ContentResolver.QUERY_ARG_LIMIT, 1);
                cursor = mContext.getContentResolver().query(
                        contentUri,
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN ? MEDIA_PROJECTIONS : MEDIA_PROJECTIONS_API_16,
                        queryArgs,
                        null
                );
            }

            if (cursor == null) {
                return;
            }
            if (!cursor.moveToFirst()) {
                return;
            }

            // 获取各列的索引
            int dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            int dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN);
            int dateAddedIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_ADDED);
            int dateModifiedIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED);
            // 获取行数据
            String data = cursor.getString(dataIndex);

            long dateTaken = cursor.getLong(dateTakenIndex);
            String dateAdded =  DateUtil.getDateTime("yyyy-MM-dd HH:mm:ss", cursor.getLong(dateAddedIndex) * 1000);
            String dateModified =  DateUtil.getDateTime("yyyy-MM-dd HH:mm:ss", cursor.getLong(dateModifiedIndex) * 1000);
            String dateTaken1 = DateUtil.getDateTime("yyyy-MM-dd HH:mm:ss", dateTaken);
//            Log.e("julis_data","文件名:"+data+" 添加时间："+" "+cursor.getLong(dateAddedIndex)+" 修改时间:"+cursor.getLong(dateModifiedIndex)+" dateTaken时间:"+dateTaken);
//            Log.e("julis_data","文件名:"+data+" 添加时间："+" "+dateAdded+" 修改时间:"+dateModified+" dateTaken时间:"+dateTaken1);
            // 处理获取到的第一行数据
            handleMediaRowData(data, dateTaken);
//            while (cursor.moveToNext()) {
//                // 获取各列的索引
//                int dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
//                int dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN);
//                int dateAddedIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_ADDED);
//                int dateModifiedIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED);
//
//                // 获取行数据
//                String data = cursor.getString(dataIndex);
//                String dateTaken = DateUtil.getDateTime("yyyy-MM-dd HH:mm:ss", cursor.getLong(dateTakenIndex));
//                String dateAdded = "";
//                if (dateAddedIndex != -1){
//                    dateAdded =  DateUtil.getDateTime("yyyy-MM-dd HH:mm:ss", cursor.getLong(dateAddedIndex));
//                }
//                String dateModified = "";
//                if (dateModifiedIndex != -1){
//                    dateModified =  DateUtil.getDateTime("yyyy-MM-dd HH:mm:ss", cursor.getLong(dateModifiedIndex));
//                }
//
//
//                Log.e("julis_data","文件名:"+data+" 添加时间："+" "+dateAdded+" 修改时间:"+dateModified+" dateTaken时间:"+dateTaken);
//
//            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    /**
     * 判断时间是否合格，图片插入时间小于1秒才有效
     */
    private boolean isTimeValid(long dateAdded) {
        return Math.abs(System.currentTimeMillis() - dateAdded) <= 1;
    }


    /**
     * 处理监听到的资源
     *
     * @param data      /storage/emulated/0/Pictures/Screenshots/Screenshot_20200705-234705.jpg
     * @param dateTaken 1593964025960  截图的时间戳
     */
    private void handleMediaRowData(String data, long dateTaken) {
        // 发现个别手机会自己修改截图文件夹的文件，截屏功能会误以为是用户在截屏操作，进行捕获，所以加了一个时间判断
//        if (!isTimeValid(dateTaken)) {
//            Log.e(TAG, "图片插入时间大于1秒，不是截屏");
//            return;
//        }


//        Long theDT = System.currentTimeMillis();
//        Long nextAM = Long.valueOf(3000);  //Use a 1 second minimum delay to avoid repeated calls
//        SharedPreferences preferences = PreferenceManager
//                .getDefaultSharedPreferences(mContext);
//        Long lastAM = preferences.getLong("lastAM", 0);
//        if ((lastAM + nextAM) < theDT){
//            SharedPreferences.Editor editor = preferences.edit();
//            editor.putLong("lastAM", theDT); // value to store
//            editor.commit();
//
//            // DO WHAT YOU NEED TO DO HERE
//            if (checkScreenShot(data, dateTaken)) {
//                Log.d(TAG, data + " " + dateTaken);
//                mListener.onShot(data);
//            } else {
//                Log.d(TAG, "Not screenshot event");
//            }
//        }
        //这里我做了一个简单的判断，如何DATE_ADDED和当前时间相差两秒以内，那么从数据库查出的这条数据我视为有效
        if (checkScreenShot(data, dateTaken)) {
//            Log.d(TAG, data + " " + dateTaken);
//            Log.e(TAG,   " shotTemp size" + shotTemp.size() + " " + shotTemp);
            if (!shotTemp.contains(data)){
                shotTemp.add(data);
//                Log.e(TAG,   " shotTemp size" + shotTemp.size() + " " + Thread.currentThread() + " " + shotTemp);
                mListener.onShot(data);
            }
        } else {
            Log.d(TAG, "Not screenshot event");
        }

    }

    /**
     * 判断是否是截屏
     */
    private boolean checkScreenShot(String data, long dateTaken) {

        data = data.toLowerCase();
        // 判断图片路径是否含有指定的关键字之一, 如果有, 则认为当前截屏了
        for (String keyWork : KEYWORDS) {
            if (data.contains(keyWork)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 媒体内容观察者(观察媒体数据库的改变)
     */
    private class MediaContentObserver extends ContentObserver {

        private Uri mContentUri;

        public MediaContentObserver(Uri contentUri, Handler handler) {
            super(handler);
            mContentUri = contentUri;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
//            Log.e(TAG,  " -=-=" + mContentUri.toString());
//            Log.e(TAG, " self change1 " + selfChange + " Uri " + mContentUri.toString());
            handleMediaContentChange(mContentUri);
        }

        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            super.onChange(selfChange, uri);
//            Log.e(TAG, " self change " + selfChange + " Uri " + uri);
        }
    }
}
