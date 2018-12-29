package me.w5e.sdk.network.request;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.w5e.sdk.network.HttpClient;
import me.w5e.sdk.network.progress.ProgressListener;
import me.w5e.sdk.network.progress.ProgressOutputStream;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by w5e on 2018/7/31.
 */
public class DownloadFileProcessor extends HttpClient.RequestProcessor<File> {
    private String mRemotePath;
    private File mLocalFile;
    private ProgressListener mListener;
    private boolean append;
    private long writtenLength;
    private Source source;
    private BufferedSink sink;
    public FileLengthListener fileLengthListener;

    /**
     * @param httpClient     端
     * @param remotePath     远程下载路径
     * @param localFile      本地文件或文件夹
     * @param renameIfExists 是否在存在同名文件是保存重命名后的文件
     * @param l              进度监听器
     */
    public DownloadFileProcessor(@NonNull HttpClient httpClient,
                                 @NonNull String remotePath, @NonNull File localFile,
                                 boolean renameIfExists,
                                 ProgressListener l) {
        super(httpClient);
        mRemotePath = remotePath;
        if (localFile.isDirectory()) {
            String filename = new File(remotePath).getName();
            mLocalFile = new File(localFile, filename);
        } else {
            mLocalFile = localFile;
        }
        if (renameIfExists) {
            mLocalFile = renameFile(mLocalFile);
        }
        mListener = l;
    }

    public File getLocalFile() {
        return mLocalFile;
    }

    @NonNull
    @Override
    protected String getUrl() {
        return mRemotePath;
    }

    @NonNull
    @Override
    protected String getMethod() {
        return HttpClient.METHOD_GET;
    }

    @Nullable
    @Override
    protected HashMap<String, String> getHeaders() {
        if (mLocalFile.exists() && mLocalFile.canWrite()) {
            writtenLength = mLocalFile.length();
        } else {
            writtenLength = 0;
        }
        append = writtenLength > 0;
        if (!append) return null;
        HashMap<String, String> headersMap = new HashMap<>();
        headersMap.put("Range", "bytes=" + writtenLength + "-");
        return headersMap;
    }

    @Nullable
    @Override
    protected HashMap<String, String> getParams() {
        return null;
    }

    @Nullable
    @Override
    protected RequestBody getRequestBody() {
        return null;
    }

    @NonNull
    @Override
    protected final HttpClient.Result<File> parseResult(Response response) {
        boolean success = response.code() == HttpClient.SC_OK
                || response.code() == HttpClient.SC_PARTIAL_CONTENT;
        if (success) {
            long fileLength = 0;
            try {
                String contentLength = response.header("Content-Length");
                if (contentLength != null) fileLength = Long.parseLong(contentLength);
                if (append) fileLength += writtenLength;
                if (fileLengthListener != null) fileLengthListener.onGetFileLength(fileLength);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(HttpClient.TAG, "Get ContentLength Failed!");
            }

            try {
                File parent = mLocalFile.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    Log.e(HttpClient.TAG, "Download File: Make dirs failed!");
                }
                ProgressOutputStream os = new ProgressOutputStream(
                        new FileOutputStream(mLocalFile, append), fileLength, writtenLength, mListener);
                sink = Okio.buffer(Okio.sink(os));
                if (response.body() != null) {
                    source = response.body().source();
                    while (!isCanceled() && source.read(sink.buffer(), 8192) != -1) {
                        sink.flush();
                    }
                    if (mLocalFile.exists() && mLocalFile.length() == fileLength) {
                        if (HttpClient.DEBUG)
                            Log.d(HttpClient.TAG, "Download Successfully");
                        return new HttpClient.Result<>(mLocalFile);
                    }
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException)) {
                    e.printStackTrace();
                }
                if (!isCanceled()) {
                    String errorMsg = e.getLocalizedMessage();
                    Log.e(HttpClient.TAG, "Download Failed: " + errorMsg);
                    return new HttpClient.Result<>(e);
                } else {
                    Log.d(HttpClient.TAG, "Download Canceled");
                }
            } finally {
                try {
                    if (!isCanceled())
                        Util.closeQuietly(source);
                    Util.closeQuietly(sink);
                } catch (Exception ignored) {
                }
            }
        } else if (response.body() != null) {
            try {
                if (HttpClient.DEBUG)
                    Log.e(HttpClient.TAG, "Download Failed: " + response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new HttpClient.Result<>(0, response.message());
        }
        return new HttpClient.Result<>(false);
    }

    private File renameFile(@NonNull File localFile) {
        File dir = localFile.getParentFile();
        String filename = localFile.getName();
        if (localFile.exists()) {
            String format;
            if (!filename.contains(".")) {
                format = filename + " (%d)";
            } else {
                format = filename.substring(0, filename.lastIndexOf('.')) + " (%d)"
                        + filename.substring(filename.lastIndexOf('.'));
            }
            int index = 0;
            while (true) {
                localFile = new File(dir, String.format(format, index));
                if (!localFile.exists())
                    return localFile;
                index++;
            }
        }
        return localFile;
    }

    public interface FileLengthListener {
        void onGetFileLength(long fileLength);
    }
}
