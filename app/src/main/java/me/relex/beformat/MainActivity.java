package me.relex.beformat;

import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.io.File;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private View mButton;
    private View mProgressLayout;
    private TextView mProgressText;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = findViewById(R.id.button);
        mProgressLayout = findViewById(R.id.progress_layout);
        mProgressText = findViewById(R.id.progress_text);
        mButton.setOnClickListener(v -> {
            if (!PermissionChecker.hasPermissions(this, PermissionChecker.NECESSARY_PERMISSIONS)) {
                Snackbar.make(mButton, R.string.denied_permission_message, Snackbar.LENGTH_LONG)
                        .show();
                return;
            }
            showProgressView();
            cleanStorage();
        });

        checkPermissions();
    }

    private void showProgressView() {
        mButton.setVisibility(View.GONE);
        mProgressLayout.setVisibility(View.VISIBLE);
    }

    private void cleanStorage() {
        // 一般耗时 5s 以内
        mProgressText.setText(R.string.clean_storage);
        Disposable disposable = IOUtil.cleanStorage()
                .compose(IOUtil.Completable.ioToMain())
                .subscribe(this::writeData);
        mCompositeDisposable.add(disposable);
    }

    private void writeData() {

        Flowable<String> flowable = Flowable.create(emitter -> {
            long availableSize = IOUtil.getStorageAvailableSize();
            String availableText = Formatter.formatFileSize(getApplicationContext(), availableSize);
            // 预留空间，500M
            long spaceSize = IOUtil.IO_BUFFER_SIZE * 200L;
            long totalWriteSize = 0;
            Random random = new Random();
            byte[] bufferByte = new byte[IOUtil.IO_BUFFER_SIZE];
            File tempFile;
            File dataDir = new File(Environment.getExternalStorageDirectory(),
                    getApplication().getString(R.string.app_name));
            dataDir.mkdirs();
            while (totalWriteSize + spaceSize < availableSize) {
                if (emitter.isCancelled()) {
                    break;
                }
                tempFile = new File(dataDir, SystemClock.uptimeMillis() + ".tmp");
                totalWriteSize += IOUtil.writeRandomDataFile(tempFile, random, bufferByte);
                emitter.onNext(getApplicationContext().getString(R.string.process_progress_format,
                        availableText,
                        Formatter.formatFileSize(getApplicationContext(), totalWriteSize)));
            }
            if (!emitter.isCancelled()) {
                emitter.onComplete();
            }
        }, BackpressureStrategy.LATEST);

        Disposable disposable = flowable.compose(IOUtil.Flowable.ioToMain()).subscribe(value -> {
            mProgressText.setText(value);
        }, throwable -> {
            mProgressText.setText(R.string.process_error);
            if (BuildConfig.DEBUG) {
                throwable.printStackTrace();
            }
        }, () -> {
            mCompositeDisposable.clear();
            mProgressText.setText(R.string.process_finish);
        });
        mCompositeDisposable.add(disposable);
    }

    public void checkPermissions() {
        if (!PermissionChecker.hasPermissions(this, PermissionChecker.NECESSARY_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PermissionChecker.NECESSARY_PERMISSIONS,
                    PermissionChecker.PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionChecker.PERMISSIONS_REQUEST_CODE
                && PermissionChecker.checkDeniedPermission(permissions, grantResults).size() > 0) {
            Snackbar.make(mButton, R.string.denied_permission_message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override public void onBackPressed() {
        if (mCompositeDisposable.size() > 0) {
            new AlertDialog.Builder(this).setMessage(R.string.exit_alert_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.exit_confirm, (dialog, which) -> {
                        mCompositeDisposable.clear();
                        onBackPressed();
                    })
                    .setNegativeButton(R.string.exit_cancel, null)
                    .create()
                    .show();
            return;
        }
        super.onBackPressed();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }
}
