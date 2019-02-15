/*
 * Copyright (c) 2019 CELLA
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.w5e.sdk.network.progress;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by w5e on 2018/7/20.
 */

public class ProgressOutputStream extends OutputStream {
    private final OutputStream mDelegate;
    private final ProgressListener mListener;

    private long mContentLength;//总长度
    private long mWrittenLength;//已经写入的长度

    private static final int PROGRESS_MAX = 100;

    public ProgressOutputStream(OutputStream delegate, long contentLength, ProgressListener l) {
        this(delegate, contentLength, 0, l);
    }

    public ProgressOutputStream(OutputStream delegate, long contentLength, long writtenLength,
                                ProgressListener l) {
        mDelegate = delegate;
        mListener = l;
        mContentLength = contentLength;
        mWrittenLength = writtenLength;
    }

    @Override
    public void write(@NonNull byte[] b, int off, int len) throws IOException {
        if (mDelegate == null) {
            return;
        }
        mDelegate.write(b, off, len);
        if (mContentLength < 0 || mWrittenLength > mContentLength) {

            mListener.onProgress(mWrittenLength, mContentLength, PROGRESS_MAX);
            return;
        }
        if (len < b.length) {
            mWrittenLength += len;
        } else {
            mWrittenLength += b.length;
        }
        updateProgress();
    }

    @Override
    public void write(int b) throws IOException {
        if (mDelegate == null) {
            return;
        }
        this.mDelegate.write(b);
        if (this.mContentLength < 0) {
            this.mListener.onProgress(mWrittenLength, mContentLength, PROGRESS_MAX);
            return;
        }
        this.mWrittenLength += 1;
        updateProgress();
    }

    @Override
    public void close() throws IOException {
        if (this.mDelegate != null) {
            this.mDelegate.close();
        }
    }

    @Override
    public void flush() throws IOException {
        if (this.mDelegate != null) {
            this.mDelegate.flush();
        }
    }

    private void updateProgress() {
        if (mListener != null) {
            int progress = (int) (mWrittenLength / (double) mContentLength * PROGRESS_MAX);
            mListener.onProgress(mWrittenLength, mContentLength, progress);
        }
    }
}
