package me.relex.beformat;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import io.reactivex.CompletableTransformer;
import io.reactivex.FlowableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.Closeable;
import java.io.File;
import java.util.Random;
import okio.BufferedSink;
import okio.Okio;

public class IOUtil {

    /**
     * 1M
     */
    public static final int IO_BUFFER_SIZE = 1024 * 1024;

    private static boolean deleteDirectory(File fileDir) {
        if (fileDir == null || !fileDir.exists()) {
            return false;
        }
        boolean success = false;
        try {
            if (fileDir.isDirectory()) {
                String[] files = fileDir.list();
                for (String child : files) {
                    success = deleteDirectory(new File(fileDir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            success = fileDir.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    private static boolean deleteFile(File file) {
        try {
            if (file.exists()) {
                return file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void closeSilently(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }

    private static void cleanExternalStorage() {
        File storageDir = Environment.getExternalStorageDirectory();
        File[] files = storageDir.listFiles();
        if (files == null || files.length <= 0) {
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                deleteFile(file);
            } else if (file.isDirectory()) {
                deleteDirectory(file);
            }
        }
    }

    public static long getStorageTotalSize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.getBlockSizeLong();
        } else {
            blockSize = stat.getBlockSize();
        }
        long totalBlocks;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            totalBlocks = stat.getBlockCountLong();
        } else {
            totalBlocks = stat.getBlockCount();
        }
        return blockSize * totalBlocks;
    }

    public static long getStorageAvailableSize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.getBlockSizeLong();
        } else {
            blockSize = stat.getBlockSize();
        }
        long availableBlocks;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            availableBlocks = stat.getAvailableBlocksLong();
        } else {
            availableBlocks = stat.getAvailableBlocks();
        }
        return blockSize * availableBlocks;
    }

    public static long writeRandomDataFile(File file, Random random, byte[] bufferByte) {
        BufferedSink buffer = null;
        long size = 0;
        try {
            buffer = Okio.buffer(Okio.sink(file));
            // 10M ~ 100M
            int randomCount = random.nextInt(90) + 10;
            for (int i = 0; i < randomCount; i++) {
                random.nextBytes(bufferByte);
                buffer.write(bufferByte);
                size += IO_BUFFER_SIZE;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeSilently(buffer);
        }
        return size;
    }

    public static io.reactivex.Completable cleanStorage() {
        return io.reactivex.Completable.defer(() -> {
            cleanExternalStorage();
            return io.reactivex.Completable.complete();
        });
    }

    public static class Flowable {
        public static <T> FlowableTransformer<T, T> ioToMain() {
            return upstream -> upstream.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

    public static class Completable {
        public static CompletableTransformer ioToMain() {
            return upstream -> upstream.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }
}
