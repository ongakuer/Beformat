package me.relex.beformat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionChecker {
    public static final int PERMISSIONS_REQUEST_CODE = 100;

    public static final String[] NECESSARY_PERMISSIONS = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    public static boolean hasPermissions(Context context, String... perms) {
        for (String perm : perms) {
            boolean hasPerm = (ContextCompat.checkSelfPermission(context, perm)
                    == PackageManager.PERMISSION_GRANTED);
            if (!hasPerm) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    public static List<String> checkDeniedPermission(String[] permissions, int[] grantResults) {
        ArrayList<String> denied = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            String perm = permissions[i];
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                denied.add(perm);
            }
        }
        return denied;
    }
}
