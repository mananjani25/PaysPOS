package com.pays.pos.utils

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.DatabaseUtils
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import com.pays.pos.data.remote.Constants
import java.io.*
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by Sumeet on 10/21/2016.
 */
object FileUtils {
    /**
     * TAG for log messages.
     */
    private val AUTHORITY: String? = "com.app.iPos.localstorage.documents"
    private const val TAG = "FileUtils"
    private const val DEBUG = false // Set to true to enable logging
    const val MIME_TYPE_AUDIO = "audio/*"
    const val MIME_TYPE_TEXT = "text/*"
    const val MIME_TYPE_IMAGE = "image/*"
    const val MIME_TYPE_VIDEO = "video/*"
    const val MIME_TYPE_APP = "application/*"
    const val HIDDEN_PREFIX = "."



    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     *
     * @param uri
     * @return Extension including the dot("."); "" if there is no extension;
     * null if uri was null.
     */
    fun getExtension(uri: String?): String? {
        if (uri == null) {
            return null
        }
        val dot = uri.lastIndexOf(".")
        return if (dot >= 0) {
            uri.substring(dot)
        } else {
            // No extension.
            ""
        }
    }

    /**
     * @return Whether the URI is a local one.
     */
    fun isLocal(url: String?): Boolean {
        return try {
            if (url != null && !url.startsWith("http://") && !url.startsWith("https://")) {
                true
            } else File(url).exists()
        } catch (e: Exception) {
            false
        }
        //        return false;
    }

    /**
     * @return True if Uri is a MediaStore Uri.
     * @author paulburke
     */
    fun isMediaUri(uri: Uri): Boolean {
        return "media".equals(uri.authority, ignoreCase = true)
    }

    /**
     * Convert File into Uri.
     *
     * @param file
     * @return uri
     */
    fun getUri(file: File?): Uri? {
        return if (file != null) {
            Uri.fromFile(file)
        } else null
    }

    /**
     * Returns the path only (without file name).
     *
     * @param file
     * @return
     */
    fun getPathWithoutFilename(file: File?): File? {
        return if (file != null) {
            if (file.isDirectory) {
                // no file to be split off. Return everything
                file
            } else {
                val filename = file.name
                val filepath = file.absolutePath

                // Construct path without file name.
                var pathwithoutname = filepath.substring(
                    0, filepath.length - filename.length
                )
                if (pathwithoutname.endsWith("/")) {
                    pathwithoutname = pathwithoutname.substring(0, pathwithoutname.length - 1)
                }
                File(pathwithoutname)
            }
        } else null
    }

    /**
     * @return The MIME type for the given file.
     */
    fun getMimeType(file: File): String? {
        val extension = getExtension(file.name)
        return if (extension!!.length > 0) MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.substring(1)) else "application/octet-stream"
    }

    /**
     * @return The MIME type for the give Uri.
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        val file = File(getPath(context, uri))
        return getMimeType(file)
    }

    /**
     * @return The MIME type for the file url.
     */
    fun getContentType(fileString: String?): String {
        var type: String? = ""
        var contentType: String? = ""
        fileString?.let{
            contentType = getFileExtensionFromUrl(fileString)
            if (TextUtils.isEmpty(contentType)) {
                val i = fileString.lastIndexOf('.')
                if (i > 0) {
                    contentType = fileString.substring(i + 1)
                }
            }
            if (contentType != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(contentType)
            }
        }
        LogUtil.logE("!_@_", "content type:  $type")
        return type?:""
    }

    fun getFileExtensionFromUrl(url: String): String? {
        var url = url
        if (!TextUtils.isEmpty(url)) {
            val fragment = url.lastIndexOf('#')
            if (fragment > 0) {
                url = url.substring(0, fragment)
            }
            val query = url.lastIndexOf('?')
            if (query > 0) {
                url = url.substring(0, query)
            }
            val filenamePos = url.lastIndexOf('/')
            val filename = if (0 <= filenamePos) url.substring(filenamePos + 1) else url

            // if the filename contains special characters, we don't
            // consider it valid for our matching purposes:
            if (!filename.isEmpty() /*&&
                    Pattern.matches("[a-zA-Z_ \\[\\]\\(\\)\\'0-9\\.\\-\\(\\)\\%]+", filename)*/) {
                val dotPos = filename.lastIndexOf('.')
                if (0 <= dotPos) {
                    return filename.substring(dotPos + 1)
                }
            }
        }
        return ""
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is [LocalStorageProvider].
     * @author paulburke
     */


    fun isLocalStorageDocument(uri: Uri): Boolean {
        return AUTHORITY == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     * @author paulburke
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author paulburke
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author paulburke
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    fun getDataColumn(
        context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
            column
        )
        try {
            cursor = context.contentResolver.query(
                uri!!, projection, selection, selectionArgs, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                if (DEBUG) DatabaseUtils.dumpCursor(cursor)
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br></br>
     * <br></br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     * //     * @see #isLocal(String)
     * @see .getFile
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun getPath(context: Context, uri: Uri): String? {
        if (DEBUG) Log.d(
            "$TAG File -",
            "Authority: " + uri.authority + ", Fragment: " + uri.fragment + ", Port: " + uri.port + ", Query: " + uri.query + ", Scheme: " + uri.scheme + ", Host: " + uri.host + ", Segments: " + uri.pathSegments.toString()
        )
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // LocalStorageProvider
            if (isLocalStorageDocument(uri)) {
                // The path is the id
                return DocumentsContract.getDocumentId(uri)
            } else if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
                )
                return getDataColumn(
                    context, contentUri, null, null
                )
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1]
                )
                return getDataColumn(
                    context, contentUri, selection, selectionArgs
                )
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                context, uri, null, null
            )
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * Convert Uri into File, if possible.
     *
     * @return file A local file that the Uri was pointing to, or null if the
     * Uri is unsupported or pointed to a remote resource.
     * @author paulburke
     * @see .getPath
     */
    fun getFile(context: Context, uri: Uri?): File? {
        if (uri != null) {
            val path = getPath(context, uri)
            if (path != null && isLocal(path)) {
                return File(path)
            }
        }
        return null
    }

    /**
     * Get the file size in a human-readable string.
     *
     * @param size
     * @return
     * @author paulburke
     */
    fun getReadableFileSize(size: Int): String {
        val BYTES_IN_KILOBYTES = 1024
        val dec = DecimalFormat("###.#")
        val KILOBYTES = " KB"
        val MEGABYTES = " MB"
        val GIGABYTES = " GB"
        var fileSize = 0f
        var suffix = KILOBYTES
        if (size > BYTES_IN_KILOBYTES) {
            fileSize = size / BYTES_IN_KILOBYTES.toFloat()
            if (fileSize > BYTES_IN_KILOBYTES) {
                fileSize = fileSize / BYTES_IN_KILOBYTES
                if (fileSize > BYTES_IN_KILOBYTES) {
                    fileSize = fileSize / BYTES_IN_KILOBYTES
                    suffix = GIGABYTES
                } else {
                    suffix = MEGABYTES
                }
            }
        }
        return dec.format(fileSize.toDouble()) + suffix
    }

    /**
     * Attempt to retrieve the thumbnail of given File from the MediaStore. This
     * should not be called on the UI thread.
     *
     * @param context
     * @param file
     * @return
     * @author paulburke
     */
    fun getThumbnail(context: Context, file: File): Bitmap? {
        return getThumbnail(
            context, getUri(file), getMimeType(file)
        )
    }

    /**
     * Attempt to retrieve the thumbnail of given Uri from the MediaStore. This
     * should not be called on the UI thread.
     *
     * @param context
     * @param uri
     * @return
     * @author paulburke
     */
    fun getThumbnail(context: Context, uri: Uri): Bitmap? {
        return getThumbnail(
            context, uri, getMimeType(context, uri)
        )
    }

    /**
     * Attempt to retrieve the thumbnail of given Uri from the MediaStore. This
     * should not be called on the UI thread.
     *
     * @param context
     * @param uri
     * @param mimeType
     * @return
     * @author paulburke
     */
    fun getThumbnail(
        context: Context, uri: Uri?, mimeType: String?
    ): Bitmap? {
        if (DEBUG) Log.d(
            TAG, "Attempting to get thumbnail"
        )

//        if (!isMediaUri(uri)) {
//            LogM.e(TAG, "You can only retrieve thumbnails for images and videos.");
//            return null;
//        }
        var bm: Bitmap? = null
        if (uri != null) {
            val resolver = context.contentResolver
            var cursor: Cursor? = null
            try {
                cursor = resolver.query(uri, null, null, null, null)
                if (cursor!!.moveToFirst()) {
                    val id = cursor.getInt(0)
                    if (DEBUG) Log.d(
                        TAG, "Got thumb ID: $id"
                    )
                    if (mimeType!!.contains("video")) {
                        bm = MediaStore.Video.Thumbnails.getThumbnail(
                            resolver, id.toLong(), MediaStore.Video.Thumbnails.MINI_KIND, null
                        )
                    } else if (mimeType.contains(MIME_TYPE_IMAGE)) {
                        bm = MediaStore.Images.Thumbnails.getThumbnail(
                            resolver, id.toLong(), MediaStore.Images.Thumbnails.MINI_KIND, null
                        )
                    }
                }
            } catch (e: Exception) {
                if (DEBUG) e.message?.let {
                    LogUtil.logE(
                        "getThumbnail", it
                    )
                }
            } finally {
                cursor?.close()
            }
        }
        return bm
    }

    /**
     * File and folder comparator. TODO Expose sorting option method
     *
     * @author paulburke
     */
    var sComparator =
        Comparator<File> { f1, f2 -> // Sort alphabetically by lower case, which is much cleaner
            f1.name.toLowerCase().compareTo(
                f2.name.toLowerCase()
            )
        }

    /**
     * File (not directories) filter.
     *
     * @author paulburke
     */
    var sFileFilter = FileFilter { file ->
        val fileName = file.name
        // Return files only (not directories) and skip hidden files
        file.isFile && !fileName.startsWith(HIDDEN_PREFIX)
    }

    /**
     * Folder (directories) filter.
     *
     * @author paulburke
     */
    var sDirFilter = FileFilter { file ->
        val fileName = file.name
        // Return directories only and skip hidden directories
        file.isDirectory && !fileName.startsWith(HIDDEN_PREFIX)
    }

    /**
     * Get the Intent for selecting content to be used in an Intent Chooser.
     *
     * @return The intent for opening a file with Intent.createChooser()
     * @author paulburke
     */
    fun createGetContentIntent(): Intent {
        // Implicitly allow the user to select a particular kind of data
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        // The MIME data type filter
        intent.type = "*/*"
        // Only return URIs that can be opened with ContentResolver
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        return intent
    }

    fun copyFileanotherFolderFolder(
        sourceFilePath: String?, destiFolderPath: String
    ): String {
        //Log.i(Prefs.TAG, "Coping file: " + filePath + " to: " + folderPath);
        try {
            val `is` = FileInputStream(sourceFilePath)
            var o: BufferedOutputStream? = null
            val destFile = File(destiFolderPath)
            try {
                val buff = ByteArray(10000)
                var read = -1
                o = BufferedOutputStream(FileOutputStream(destFile), 10000)
                while (`is`.read(buff).also { read = it } > -1) {
                    o.write(buff, 0, read)
                }
            } finally {
                `is`.close()
                o?.close()
            }
        } catch (e: FileNotFoundException) {
            e.message?.let { Log.w("TAG", it) }
        } catch (e: IOException) {
            e.message?.let { Log.w("TAG", it) }
        }
        return destiFolderPath
    }

    @TargetApi(19)
    fun handleImageOnKitkat(
        data: Intent?, activity: Context
    ): String? {
        var imagePath: String? = null
        val uri = data!!.data
        //DocumentsContract defines the contract between a documents provider and the platform.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (DocumentsContract.isDocumentUri(activity, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)

                if (docId.startsWith("raw:")) {
                    return docId.replaceFirst("raw:", "");
                }

                if ("com.android.providers.media.documents" == uri?.authority) {
                    val id = docId.split(":")[1]
                    val selsetion = MediaStore.Images.Media._ID + "=" + id
                    imagePath = getImagePath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selsetion, activity
                    )
                } else if ("com.android.providers.downloads.documents" == uri?.authority) {
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse(
                            "content://downloads/public_downloads"
                        ), java.lang.Long.valueOf(docId)
                    )
                    imagePath = getImagePath(contentUri, null, activity)
                }
            } else if ("content".equals(uri?.scheme, ignoreCase = true)) {
                imagePath = getImagePath(uri, null, activity)
            } else if ("file".equals(uri?.scheme, ignoreCase = true)) {
                imagePath = uri?.path
            }
        } else {
            imagePath = getImagePath(uri, null, activity)
        }
        return imagePath
    }

    @SuppressLint("Range")
    fun getImagePath(
        uri: Uri?, selection: String?, activity: Context
    ): String {
        var path: String? = null
        val cursor = activity.contentResolver.query(uri!!, null, selection, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path!!
    }

    fun convertDate(mSelectedDate: String): String? {

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")


        val myDate = Date()

        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone("UTC")
        calendar.time = myDate
        val time = calendar.time

        val outputFormat = SimpleDateFormat("dd-MMM-yyyy, hh:mm a")
        val date = inputFormat.parse(mSelectedDate)
        val formattedDate = outputFormat.format(date)


        return formattedDate // prints 10-04-2018


        /*val sdf = SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.US)
        var convertedDate: Date? = null
        var formattedDate: String? = null
        try {
            convertedDate = sdf.parse(mSelectedDate)
            formattedDate = SimpleDateFormat("dd-MM-yyyy").format(convertedDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return formattedDate*/
    }

    fun getUTCTimeToLocal(timeStr: String?, utcFormat: String?): String? {

//        String dateStr = "Jul 16, 2013 12:08:59 AM";
        var utcFormat = utcFormat
        return try {
            if (TextUtils.isEmpty(utcFormat)) {
                utcFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            }
            val df = SimpleDateFormat(utcFormat, Locale.ENGLISH)
            df.timeZone = TimeZone.getTimeZone("UTC")
            var date: Date? = null
            try {
                date = df.parse(timeStr)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            df.timeZone = TimeZone.getDefault()
            convertDateTime(utcFormat, "hh:mm a", df.format(date))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun getUTCTimeToLocalYYYY(timeStr: String?, utcFormat: String?): String? {

//        String dateStr = "Jul 16, 2013 12:08:59 AM";
        var utcFormat = utcFormat
        return try {
            if (TextUtils.isEmpty(utcFormat)) {
                utcFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            }
            val df = SimpleDateFormat(utcFormat, Locale.ENGLISH)
            df.timeZone = TimeZone.getTimeZone("UTC")
            var date: Date? = null
            try {
                date = df.parse(timeStr)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            df.timeZone = TimeZone.getDefault()
            convertDateTime(utcFormat, "MM/dd/yyyy", df.format(date))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun getUTCTimeToLocalDate(timeStr: String?, utcFormat: String?): String? {

//        String dateStr = "Jul 16, 2013 12:08:59 AM";
        var utcFormat = utcFormat
        return try {
            if (TextUtils.isEmpty(utcFormat)) {
                utcFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            }
            val df = SimpleDateFormat(utcFormat, Locale.ENGLISH)
            df.timeZone = TimeZone.getTimeZone("UTC")
            var date: Date? = null
            try {
                date = df.parse(timeStr)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            df.timeZone = TimeZone.getDefault()
            convertDateTime(utcFormat, "MM/dd/yyyy, hh:mm a", df.format(date))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun convertDateTime(
        fromFormat: String?, toFormat: String?, date: String?
    ): String? {
        var date = date
        if (TextUtils.isEmpty(date)) return ""
        try {
            var spf = SimpleDateFormat(fromFormat)
            val newDate = spf.parse(date)
            spf = SimpleDateFormat(toFormat)
            date = spf.format(newDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return date
    }

    fun getUTCTimeToLocalHHMM(timeStr: String?, utcFormat: String?): String? {

//        String dateStr = "Jul 16, 2013 12:08:59 AM";
        var utcFormat = utcFormat
        return try {
            if (TextUtils.isEmpty(utcFormat)) {
                utcFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            }
            val df = SimpleDateFormat(utcFormat, Locale.ENGLISH)
            df.timeZone = TimeZone.getTimeZone("US")
            var date: Date? = null
            try {
                date = df.parse(timeStr)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            df.timeZone = TimeZone.getDefault()
            convertDateTime(utcFormat, "HH:mm", df.format(date))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ""
        }
    }


    fun convertDateDDMMYYYYNew(mSelectedDate: String?): String? {
        val sdf = SimpleDateFormat("EEE, MMM dd yyyy")
        var convertedDate: Date? = null
        var formattedDate: String? = null
        try {
            convertedDate = sdf.parse(mSelectedDate)
            formattedDate = SimpleDateFormat("yyyy-MM-dd").format(convertedDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return formattedDate
    }

    fun convertDateDDMMYYYYHome(mSelectedDate: String?): String? {
        val sdf = SimpleDateFormat("EEE, MMM dd yyyy")
        var convertedDate: Date? = null
        var formattedDate: String? = null
        try {
            convertedDate = sdf.parse(mSelectedDate)
            formattedDate = SimpleDateFormat("dd-MM-yyyy").format(convertedDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return formattedDate
    }

    fun convertCurrentDate(mSelectedDate: Date): String? {
        var formattedDate: String? = null
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            formattedDate = sdf.format(mSelectedDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return formattedDate
    }


    fun convertCurrentTime(mSelectedDate: String): String {
        try {
            val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm")
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate
        } catch (e: Exception) {
//Thu Jul 16 05:23:26 EDT 2020
            val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
            val outputFormat = SimpleDateFormat("HH:mm")
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate

        }


    }


    fun convertCurrentDate(mSelectedDate: String): String {
        try {
            val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE, MMM dd")
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate
        } catch (e: Exception) {
//Thu Jul 16 05:23:26 EDT 2020
            val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
            val outputFormat = SimpleDateFormat("EEE, MMM dd")
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate

        }


    }

    /*fun convertTimeOtherDays(mSelectedDate: String?): String? {
        try {
            val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("hh:mm a")
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            return formattedDate
        }catch (e: Exception) {
            val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
            val outputFormat = SimpleDateFormat("hh:mm a")
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate

        }
    }*/


    fun convertTime(mSelectedDate: String, utcFormat: String?): String? {

        var utcFormat = utcFormat
        return try {
            if (TextUtils.isEmpty(utcFormat)) {
                utcFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            }
            val df = SimpleDateFormat(utcFormat, Locale.ENGLISH)
            df.timeZone = TimeZone.getTimeZone("US")
            var date: Date? = null
            try {
                date = df.parse(mSelectedDate)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            df.timeZone = TimeZone.getDefault()

            var dateString = convertDateTime(utcFormat, "hh:mm a", df.format(date))

//            var dateString = df.format(date)
//            if (TextUtils.isEmpty(dateString)) return ""
//
//            var spf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
//            val newDate = spf.parse(dateString)
//            spf = SimpleDateFormat("hh:mm a")
//            dateString = spf.format(newDate)
            return dateString
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ""
        }


        // prints 10-04-2018


        /*EEE,MMM dd yyyy hh:mm - hh:mm:aa*/

        /*val sdf = SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.US)
        var convertedDate: Date? = null
        var formattedDate: String? = null
        try {
            convertedDate = sdf.parse(mSelectedDate)
            formattedDate = SimpleDateFormat("dd-MM-yyyy").format(convertedDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return formattedDate*/
    }


    fun convertDateToCheck(mSelectedDate: String): String? {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        val outputFormat = SimpleDateFormat("MM/dd/yyyy hh:mm: a")
        val date = inputFormat.parse(mSelectedDate)
        val formattedDate = outputFormat.format(date)
        //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
        return formattedDate
    }


    fun showDate(mSelectedDate: String): String? {
        val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
        val outputFormat = SimpleDateFormat("MM/dd/yyyy hh:mm a")
        val date = inputFormat.parse(mSelectedDate)
        val formattedDate = outputFormat.format(date)
        //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
        return formattedDate
    }

    fun sendDateToServer(mSelectedDate: String): String {
        val inputFormat = SimpleDateFormat("MM/dd/yyyy hh:mm a")
        val outputFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a")
        val date = inputFormat.parse(mSelectedDate)
        val formattedDate = outputFormat.format(date)
        //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
        return formattedDate
    }


    fun convertTimeToCheck(mSelectedDate: String?): String? {
        val inputFormat = SimpleDateFormat("hh:mm a")
        val outputFormat = SimpleDateFormat("HH:mm")
        val date = inputFormat.parse(mSelectedDate)
        val formattedDate = outputFormat.format(date)
        //  val formattedTimeFinal = outputFormat.parse(formattedDate)
        return formattedDate
    }


    fun convertDay(mSelectedDate: String?): String? {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        var convertedDate: Date? = null
        var formattedDate: String? = null
        try {
            convertedDate = sdf.parse(mSelectedDate)
            formattedDate = SimpleDateFormat("dd,MMM yyyy ").format(convertedDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return formattedDate
    }

    fun getFormatedDateTime(
        dateStr: String?, strReadFormat: String?, strWriteFormat: String?
    ): String? {
        var formattedDate = dateStr
        val readFormat: DateFormat = SimpleDateFormat(strReadFormat, Locale.getDefault())
        val writeFormat: DateFormat = SimpleDateFormat(strWriteFormat, Locale.getDefault())
        var date: Date? = null
        try {
            date = readFormat.parse(dateStr)
        } catch (e: ParseException) {
        }
        if (date != null) {
            formattedDate = writeFormat.format(date)
        }
        return formattedDate
    }

    fun getTimeSlots(calender: Calendar, addHours: Int): ArrayList<String>? {
        val localTimeModelList: ArrayList<String>? = ArrayList()

        if (addHours == 1) {
            calender.add(Calendar.MINUTE, 30)
        }
        val currentTime = calender.time.toString()

        //dateConvert = convertCurrentDate(currentTime)
        //  fromTimeConvert = convertAMPMFormat(currentTime)
        // toTimeConvert = convertAMPMFormat(currentTime)

        //val timeValue = "2015-10-28T18:37:04.899+05:30"
        val sdf = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
        try {
            val startCalendar = Calendar.getInstance()
            startCalendar.time = sdf.parse(currentTime)
            if (addHours == 1) {
                if (startCalendar[Calendar.MINUTE] < 30) {
                    startCalendar[Calendar.MINUTE] = 30
                } else {
                    startCalendar.add(Calendar.MINUTE, 30) // overstep hour and clear minutes
                    startCalendar.clear(Calendar.MINUTE)
                }
            }
            val endCalendar = Calendar.getInstance()
            endCalendar.time = startCalendar.time

            // if you want dates for whole next day, uncomment next line
            //endCalendar.add(Calendar.DAY_OF_YEAR, 1);
            endCalendar.add(Calendar.HOUR_OF_DAY, 24 - startCalendar[Calendar.HOUR_OF_DAY])
            endCalendar.clear(Calendar.MINUTE)
            endCalendar.clear(Calendar.SECOND)
            endCalendar.clear(Calendar.MILLISECOND)
            val slotTime = SimpleDateFormat("hh:mm a", Locale.US)
            // val slotDate = SimpleDateFormat(", dd/MM/yy"),
            while (endCalendar.after(startCalendar)) {
                val slotStartTime = slotTime.format(startCalendar.time)
                //   val slotStartDate = slotDate.format(startCalendar.time)
                startCalendar.add(Calendar.MINUTE, 30)
                val slotEndTime = slotTime.format(startCalendar.time)
                startCalendar.add(Calendar.MINUTE, -15)
                val finalSlotTime = "$slotStartTime - $slotEndTime"
                Log.d("DATE", "$slotStartTime - $slotEndTime")

                // localTimeModel = LocalTimeModel(finalSlotTime)
                localTimeModelList?.add(finalSlotTime)

                if (slotEndTime == "12:00 AM") {
                    break
                }

            }
        } catch (e: ParseException) {
            // date in wrong format
        }
        return localTimeModelList
    }

    @Throws(IOException::class)
    fun createImageOrVideoFile(context: Context, mediaTypeImage: Int): File? {
        var storageDir: File? = context.filesDir
        val dirCreated: Boolean
        if (storageDir == null) {
            val externalStorage: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (externalStorage == null) {
                storageDir = File(context.cacheDir, Environment.DIRECTORY_PICTURES)
                dirCreated = storageDir.exists() || storageDir.mkdirs()
            } else {
                dirCreated = true
            }
        } else {
            storageDir = File(context.filesDir, Environment.DIRECTORY_PICTURES)
            dirCreated = storageDir.exists() || storageDir.mkdirs()
        }
        return if (dirCreated) {
            val timeStamp = System.currentTimeMillis().toString()
            val file: File? = if (mediaTypeImage == Constants.MEDIA_TYPE_IMAGE) {
                val imageFileName: String = Constants.FILE_NAME_IMG.toString() + timeStamp
                File.createTempFile(
                    imageFileName,  //prefix
                    "." + Constants.EXTENSION_CAMERA_IMAGE_TEMP_IMG,  //suffix
                    storageDir //directory
                )
            } else {
                val videoFileName: String = Constants.FILE_NAME_VIDEO.toString() + timeStamp
                File.createTempFile(
                    videoFileName,  //prefix
                    "." + Constants.EXTENSION_CAMERA_VIDEO_TEMP_IMG,  //suffix
                    storageDir //directory
                )
            }
            file
        } else {
            null
        }
    }

}