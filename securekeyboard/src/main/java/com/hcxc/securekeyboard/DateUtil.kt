package com.hcxc.securekeyboard

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * author: wyb
 * date: 2018/2/7.
 * 日期格式为特殊格式,后台和前端不能直接以date变量去接取
 * 需要进行字符串转换后才能使用
 * dateFormat.timeZone = TimeZone.getTimeZone("Asia/Shanghai")->默认传入工具类时取的时区是手机时区，需要校准时加入该方法进行校准
 */
object DateUtil {
    //后台一般会返回的日期形式的字符串
    const val EN_M = "MM"
    const val EN_MD = "MM-dd"
    const val EN_HM = "HH:mm"
    const val EN_HMS = "HH:mm:ss"
    const val EN_YM = "yyyy-MM"
    const val EN_YMD = "yyyy-MM-dd"
    const val EN_YMDHM = "yyyy-MM-dd HH:mm"
    const val EN_YMDHMS = "yyyy-MM-dd HH:mm:ss"
    const val CN_M = "M月"
    const val CN_MD = "M月d日"
    const val CN_HM = "HH时mm分"
    const val CN_HMS = "HH时mm分ss秒"
    const val CN_YM = "yyyy年M月"
    const val CN_YMD = "yyyy年MM月dd日"
    const val CN_YMDHM = "yyyy年MM月dd日 HH时mm分"
    const val CN_YMDHMS = "yyyy年MM月dd日 HH时mm分ss秒"

    /**
     * 传入日期是否为手机当日
     *
     * @param inputDate 日期类
     * @return
     */
    @Synchronized
    @JvmStatic
    fun isToday(inputDate: Date): Boolean {
        var flag = false
        try {
            //获取当前系统时间
            val subDate = getDateTime(EN_YMD, System.currentTimeMillis())
            //定义每天的24h时间范围
            val beginTime = "$subDate 00:00:00"
            val endTime = "$subDate 23:59:59"
            //转换Date
            val dateFormat = getDateFormat(EN_YMDHMS)
            val parseBeginTime = dateFormat.parse(beginTime)
            val parseEndTime = dateFormat.parse(endTime)
            if (inputDate.after(parseBeginTime) && inputDate.before(parseEndTime)) flag = true
        } catch (ignored: ParseException) {
        }
        return flag
    }

    /**
     * 日期对比（统一年月日形式）
     *
     * @param fromSource 比较日期a
     * @param toSource   比较日期b
     * @return
     */
    @Synchronized
    @JvmStatic
    fun compareDate(fromSource: String, toSource: String, format: String = EN_YMD): Int {
        val dateFormat = getDateFormat(format)
        try {
            val comparedDate = dateFormat.parse(fromSource) ?: Date()
            val comparedDate2 = dateFormat.parse(toSource) ?: Date()
            return when {
                comparedDate.time > comparedDate2.time -> 1//日程时间大于系统时间
                comparedDate.time < comparedDate2.time -> -1//日程时间小于系统时间
                else -> 0
            }
        } catch (ignored: Exception) {
        }
        return 0
    }

    /**
     * 获取转换日期
     *
     * @param fromFormat 被转换的日期格式
     * @param toFormat   要转换的日期格式
     * @param source 被转换的日期
     * @return
     */
    @Synchronized
    @JvmStatic
    fun getDateFormat(fromFormat: String, toFormat: String, source: String): String {
        var result = ""
        try {
            result = getDateTime(toFormat, getDateFormat(fromFormat).parse(source) ?: Date())
        } catch (ignored: ParseException) {
        }
        return result
    }

    /**
     * 传入指定格式的日期字符串转成毫秒
     *
     * @param format 日期格式
     * @param source 日期
     * @return
     */
    @Synchronized
    @JvmStatic
    fun getDateTime(format: String, source: String) = getDateFormat(format).parse(source)?.time ?: 0


    /**
     * 传入指定日期格式和毫秒转换成日期字符串
     *
     * @param format 日期格式
     * @param timestamp 时间戳
     * @return
     */
    @Synchronized
    @JvmStatic
    fun getDateTime(format: String, timestamp: Long) = getDateFormat(format).format(Date(timestamp)) ?: ""

    /**
     * 传入指定日期格式和日期類转换成日期字符串
     *
     * @param format 日期格式
     * @param date 日期类
     * @return
     */
    @Synchronized
    @JvmStatic
    fun getDateTime(format: String, date: Date) = getDateFormat(format).format(date) ?: ""

    /**
     * 获取日期格式，时区为校准的中国时区
     * @param format 日期格式
     */
    private fun getDateFormat(format: String): SimpleDateFormat {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        return dateFormat
    }


    /**
     * 传入毫秒转换成00:00的格式
     *
     * @param timestamp 时间戳
     * @return
     */
    @Synchronized
    @JvmStatic
    fun getTime(timestamp: Long): String {
        if (timestamp <= 0) return "00:00"
        val second = (timestamp / 1000 / 60).toInt()
        val million = (timestamp / 1000 % 60).toInt()
        return "${if (second >= 10) second.toString() else "0$second"}:${if (million >= 10) million.toString() else "0$million"}"
    }

    /**
     * 获取日期的当月的第几周
     *
     * @param source 日期（yyyy-MM-dd）
     * @return
     */
    @Synchronized
    @JvmStatic
    fun getWeekOfMonth(source: String): Int {
        try {
            Calendar.getInstance().apply {
                time = getDateFormat(EN_YMD).parse(source) ?: Date()
                return get(Calendar.WEEK_OF_MONTH)
            }
        } catch (ignored: ParseException) {
        }
        return 0
    }

    /**
     * 获取日期是第几周
     *
     * @param source 日期（yyyy-MM-dd）
     * @return
     */
    @Synchronized
    @JvmStatic
    fun getWeekOfDate(source: String): Int {
        try {
            Calendar.getInstance().apply {
                time = getDateFormat(EN_YMD).parse(source) ?: Date()
                var weekIndex = get(Calendar.DAY_OF_WEEK) - 1
                if (weekIndex < 0) weekIndex = 0
                return weekIndex
            }
        } catch (ignored: ParseException) {
        }
        return 0
    }

    /**
     * 返回中文形式的星期
     *
     * @param source 日期（yyyy-MM-dd）
     * @return
     */
    @Synchronized
    @JvmStatic
    fun getDateWeek(source: String): String {
        return when (getWeekOfDate(source)) {
            0 -> "星期天"
            1 -> "星期一"
            2 -> "星期二"
            3 -> "星期三"
            4 -> "星期四"
            5 -> "星期五"
            6 -> "星期六"
            else -> ""
        }
    }

    /**
     * 处理时间
     *
     * @param timestamp 时间戳->秒
     */
    @Synchronized
    @JvmStatic
    fun getSecondFormat(timestamp: Long): String {
        val result: String?
        val hour: Long
        val second: Long
        var minute: Long
        if (timestamp <= 0) return "00:00:00" else {
            minute = timestamp / 60
            hour = minute / 60
            if (hour > 99) return "99:59:59"
            minute %= 60
            second = timestamp - hour * 3600 - minute * 60
            result = "${unitFormat(hour)}:${unitFormat(minute)}:${unitFormat(second)}"
        }
        return result
    }

    private fun unitFormat(time: Long) = if (time in 0..9) "0$time" else "" + time

    @Synchronized
    @JvmStatic
    fun getSecondCNFormat(timestamp: Long): String {
        val hour = timestamp / 3600//小时
        val minute = (timestamp - hour * 3600) / 60//分钟
        val second = (timestamp - hour * 3600).mod(60)//秒
        return if (0L == hour)"${minute}分${second}秒" else "${hour}时${minute}分${second}秒"
    }

//    private fun fetchGalleryImages(
//        context: Context,
//        orderBy: String,
//        orderAscending: Boolean,
//        limit: Int = 20,
//        offset: Int = 0
//    ): List<Any> {
//        val galleryImageUrls = mutableListOf<Any>()
//        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
//        val projection = arrayOf(
//            MediaStore.Files.FileColumns._ID,
//            MediaStore.Files.FileColumns.DATA,
//            MediaStore.Files.FileColumns.DATE_ADDED,
//            MediaStore.Files.FileColumns.MEDIA_TYPE,
//            MediaStore.Files.FileColumns.MIME_TYPE,
//            MediaStore.Files.FileColumns.TITLE,
//            MediaStore.Video.Media.DURATION
//        )
//        val whereCondition = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
//        val selectionArgs = arrayOf(
//            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
//            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
//        )
//        createCursor(
//            contentResolver = context.contentResolver,
//            collection = collection,
//            projection = projection,
//            whereCondition = whereCondition,
//            selectionArgs = selectionArgs,
//            orderBy = orderBy,
//            orderAscending = orderAscending,
//            limit = limit,
//            offset = offset
//        )?.use { cursor ->
//            while (cursor.moveToNext()) {
//                galleryImageUrls.add(
////                    MediaBrowser.MediaItem(
////                        cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)),
////                        ContentUris.withAppendedId(
////                            collection,
////                            cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
////                        ),
////                        cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)),
////                        cursor.getStringOrNull(cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)),
////                        cursor.getLongOrNull(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
////                    )
//                )
//            }
//        }
//        return galleryImageUrls
//    }
//
//    private fun createCursor(
//        contentResolver: ContentResolver,
//        collection: Uri,
//        projection: Array<String>,
//        whereCondition: String,
//        selectionArgs: Array<String>,
//        orderBy: String,
//        orderAscending: Boolean,
//        limit: Int = 20,
//        offset: Int = 0
//    ): Cursor? = when {
//        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
//            val selection = createSelectionBundle(whereCondition, selectionArgs, orderBy, orderAscending, limit, offset)
//            contentResolver.query(collection, projection, selection, null)
//        }
//        else -> {
//            val orderDirection = if (orderAscending) "ASC" else "DESC"
//            var order = when (orderBy) {
//                "ALPHABET" -> "${MediaStore.Audio.Media.TITLE}, ${MediaStore.Audio.Media.ARTIST} $orderDirection"
//                else -> "${MediaStore.Audio.Media.DATE_ADDED} $orderDirection"
//            }
//            order += " LIMIT $limit OFFSET $offset"
//            contentResolver.query(collection, projection, whereCondition, selectionArgs, order)
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun createSelectionBundle(
//        whereCondition: String,
//        selectionArgs: Array<String>,
//        orderBy: String,
//        orderAscending: Boolean,
//        limit: Int = 20,
//        offset: Int = 0
//    ): Bundle = Bundle().apply {
//        // Limit & Offset
//        putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
//        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
//        // Sort function
//        when (orderBy) {
//            "ALPHABET" -> putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Files.FileColumns.TITLE))
//            else -> putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Files.FileColumns.DATE_ADDED))
//        }
//        // Sorting direction
//        val orderDirection =
//            if (orderAscending) ContentResolver.QUERY_SORT_DIRECTION_ASCENDING else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
//        putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, orderDirection)
//        // Selection
//        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, whereCondition)
//        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
//    }

}