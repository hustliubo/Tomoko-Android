package me.w5e.sdk.network.progress;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.w5e.sdk.network.HttpClient;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * 可以监听文件上传进度的请求体
 */
public class FileRequestBody extends RequestBody {

    protected File file;
    private ProgressListener listener;
    private MediaType contentType;

    public FileRequestBody(@NonNull File file, ProgressListener progressListener) {
        this(null, file, progressListener);
    }

    public FileRequestBody(@Nullable MediaType contentType, @NonNull File file,
                           ProgressListener progressListener) {
        this.file = file;
        if (contentType != null)
            this.contentType = contentType;
        else
            this.contentType = HttpClient.CONTENT_TYPE_DEFAULT;
        this.listener = progressListener;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(file);
            double totalLength = contentLength();
            if (HttpClient.DEBUG) Log.d(HttpClient.TAG, "Write Body: " + totalLength);
            long wroteLength = 0;
            long readLen;
            while ((readLen = source.read(sink.buffer(), 8192)) != -1) {//okio.Segment.SIZE
                wroteLength += readLen;
                sink.flush();
                if (listener != null)
                    listener.onProgress(wroteLength, (long) totalLength, (int) (wroteLength / totalLength * 100));
            }
        } finally {
            Util.closeQuietly(source);
        }
    }
}