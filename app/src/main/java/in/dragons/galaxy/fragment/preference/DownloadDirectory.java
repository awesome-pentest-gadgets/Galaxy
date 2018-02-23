package in.dragons.galaxy.fragment.preference;

import android.Manifest;
import android.content.pm.PackageManager;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;

import java.io.File;
import java.io.IOException;

import in.dragons.galaxy.ContextUtil;
import in.dragons.galaxy.GalaxyPermissionManager;
import in.dragons.galaxy.Paths;
import in.dragons.galaxy.PreferenceActivity;
import in.dragons.galaxy.R;

public class DownloadDirectory extends Abstract {

    private EditTextPreference preference;

    public DownloadDirectory setPreference(EditTextPreference preference) {
        this.preference = preference;
        return this;
    }

    @Override
    public void draw() {
        preference.setSummary(Paths.getDownloadPath(activity).getAbsolutePath());
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                GalaxyPermissionManager permissionManager = new GalaxyPermissionManager(activity);
                if (!permissionManager.checkPermission()) {
                    permissionManager.requestPermission();
                }
                return true;
            }
        });
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                String newValue = (String) o;
                boolean result = checkNewValue(newValue);
                if (!result) {
                    if (ContextUtil.isAlive(activity) && !((EditTextPreference) preference).getText().equals(Paths.FALLBACK_DIRECTORY)) {
                        getFallbackDialog().show();
                    } else {
                        ContextUtil.toast(activity, R.string.error_downloads_directory_not_writable);
                    }
                } else {
                    try {
                        preference.setSummary(new File(Paths.getStorageRoot(activity), newValue).getCanonicalPath());
                    } catch (IOException e) {
                        Log.i(getClass().getName(), "checkNewValue returned true, but drawing the path \"" + newValue + "\" in the summary failed... strange");
                        return false;
                    }
                }
                return result;
            }

            private boolean checkNewValue(String newValue) {
                try {
                    File storageRoot = Paths.getStorageRoot(activity);
                    File newDir = new File(storageRoot, newValue).getCanonicalFile();
                    if (!newDir.getCanonicalPath().startsWith(storageRoot.getCanonicalPath())) {
                        return false;
                    }
                    if (newDir.exists()) {
                        return newDir.canWrite();
                    }
                    if (activity.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        return newDir.mkdirs();
                    }
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }

            private MaterialStyledDialog getFallbackDialog() {
                return new MaterialStyledDialog.Builder(activity)
                        .setHeaderDrawable(R.drawable.header_03)
                        .setTitle(R.string.dialog_title_logout)
                        .setDescription(activity.getString(R.string.error_downloads_directory_not_writable)
                                + "\n\n" + activity.getString(R.string.pref_message_fallback, Paths.FALLBACK_DIRECTORY))
                        .setPositiveText(android.R.string.yes)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                preference.setText(Paths.FALLBACK_DIRECTORY);
                                preference.getOnPreferenceChangeListener().onPreferenceChange(preference, Paths.FALLBACK_DIRECTORY);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeText(android.R.string.cancel)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(true)
                        .withDialogAnimation(true)
                        .show();
            }
        });
    }

    public DownloadDirectory(PreferenceActivity activity) {
        super(activity);
    }
}
