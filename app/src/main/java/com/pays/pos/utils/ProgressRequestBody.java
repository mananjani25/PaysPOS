package com.pays.pos.utils;

import android.os.Handler;
import android.os.Looper;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ProgressRequestBody extends RequestBody {
    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private File mFile;
    private String mPath;
    private UploadCallbacks mListener;
    private String content_type;

    public ProgressRequestBody(final File file, String content_type,
                               final UploadCallbacks listener) {
        this.content_type = content_type;
        mFile = file;
        mListener = listener;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        //return MediaType.parse(content_type + "/*");
        return MediaType.parse(content_type);
    }

    @Override
    public long contentLength() throws IOException {
        return mFile.length();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long fileLength = mFile.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        FileInputStream in = new FileInputStream(mFile);
        long uploaded = 0;

        try {
            int read;
            int num = 0;
            Handler handler = new Handler(Looper.getMainLooper());
            while ((read = in.read(buffer)) != -1) {

                int progress = (int) (100 * uploaded / fileLength);
                if( progress > num + 1 ){
                    // update progress on UI thread
                    handler.post(new ProgressUpdater(uploaded, fileLength));
                    num = progress;
                }

                uploaded += read;
                sink.write(buffer, 0, read);

                /*uploaded += read;
                sink.write(buffer, 0, read);

                // update progress on UI thread
                handler.post(new ProgressUpdater(uploaded, fileLength));*/
            }
        } finally {
            in.close();
        }
    }

    public interface UploadCallbacks {
        void onProgressUpdate(int percentage);

        void onError();

        void onFinish();
    }

    private class ProgressUpdater implements Runnable {
        private long mUploaded;
        private long mTotal;

        ProgressUpdater(long uploaded, long total) {
            mUploaded = uploaded;
            mTotal = total;
        }

        @Override
        public void run() {
            if (mListener!=null) {
                mListener.onProgressUpdate((int) (100 * mUploaded / mTotal));
            }
        }
    }
}
