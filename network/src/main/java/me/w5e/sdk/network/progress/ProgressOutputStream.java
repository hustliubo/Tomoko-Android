package me.w5e.sdk.network.progress;

import java.io.IOException;
import java.io.OutputStream;

import androidx.annotation.NonNull;

/**
 * Created by w5e on 2018/7/20.
 */
public class ProgressOutputStream extends OutputStream {
    private final OutputStream delegate;
    private final ProgressListener l;

    private long contentLength;//总长度
    private long writtenLength;//已经写入的长度

    public ProgressOutputStream(OutputStream delegate, long contentLength, ProgressListener l) {
        this(delegate, contentLength, 0, l);
    }

    public ProgressOutputStream(OutputStream delegate, long contentLength, long writtenLength,
                                ProgressListener l) {
        this.delegate = delegate;
        this.l = l;
        this.contentLength = contentLength;
        this.writtenLength = writtenLength;
    }

    @Override
    public void write(@NonNull byte[] b, int off, int len) throws IOException {
        if (delegate == null) return;
        delegate.write(b, off, len);
        if (contentLength < 0 || writtenLength > contentLength) {
            l.onProgress(writtenLength, contentLength, 100);
            return;
        }
        if (len < b.length) {
            writtenLength += len;
        } else {
            writtenLength += b.length;
        }
        updateProgress();
    }

    @Override
    public void write(int b) throws IOException {
        if (delegate == null) return;
        this.delegate.write(b);
        if (this.contentLength < 0) {
            this.l.onProgress(writtenLength, contentLength, 100);
            return;
        }
        this.writtenLength += 1;
        updateProgress();
    }

    @Override
    public void close() throws IOException {
        if (this.delegate != null) {
            this.delegate.close();
        }
    }

    @Override
    public void flush() throws IOException {
        if (this.delegate != null) {
            this.delegate.flush();
        }
    }

    private void updateProgress() {
        if (l != null) {
            int progress = (int) (writtenLength / (double) contentLength * 100);
            l.onProgress(writtenLength, contentLength, progress);
        }
    }
}