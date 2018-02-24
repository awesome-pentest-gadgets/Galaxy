package in.dragons.galaxy.fragment.preference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.github.yeriomin.playstoreapi.PropertiesDeviceInfoProvider;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import in.dragons.galaxy.ContextUtil;
import in.dragons.galaxy.DeviceInfoActivity;
import in.dragons.galaxy.GalaxyActivity;
import in.dragons.galaxy.OnListPreferenceChangeListener;
import in.dragons.galaxy.PlayStoreApiAuthenticator;
import in.dragons.galaxy.PreferenceActivity;
import in.dragons.galaxy.R;
import in.dragons.galaxy.SpoofDeviceManager;
import in.dragons.galaxy.Util;

public class Device extends List {

    private static final String PREFERENCE_DEVICE_DEFINITION_REQUESTED = "PREFERENCE_DEVICE_DEFINITION_REQUESTED";

    public Device(PreferenceActivity activity) {
        super(activity);
    }

    @Override
    public void draw() {
        super.draw();
        listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ContextUtil.toast(
                        activity.getApplicationContext(),
                        R.string.pref_device_to_pretend_to_be_notice,
                        PreferenceManager.getDefaultSharedPreferences(activity).getString(PreferenceActivity.PREFERENCE_DOWNLOAD_DIRECTORY, "")
                );
                ((AlertDialog) listPreference.getDialog()).getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                        if (position > 0) {
                            Intent i = new Intent(activity, DeviceInfoActivity.class);
                            i.putExtra(DeviceInfoActivity.INTENT_DEVICE_NAME, (String) keyValueMap.keySet().toArray()[position]);
                            activity.startActivity(i);
                        }
                        return false;
                    }
                });
                return false;
            }
        });
    }

    @Override
    protected OnListPreferenceChangeListener getOnListPreferenceChangeListener() {
        OnListPreferenceChangeListener listener = new OnListPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!TextUtils.isEmpty((String) newValue) && !isDeviceDefinitionValid((String) newValue)) {
                    ContextUtil.toast(activity.getApplicationContext(), R.string.error_invalid_device_definition);
                    return false;
                }
                showLogOutDialog();
                return super.onPreferenceChange(preference, newValue);
            }
        };
        listener.setDefaultLabel(activity.getString(R.string.pref_device_to_pretend_to_be_default));
        return listener;
    }

    @Override
    protected Map<String, String> getKeyValueMap() {
        Map<String, String> devices = new SpoofDeviceManager(activity).getDevices();
        devices = Util.sort(devices);
        Util.addToStart(
                (LinkedHashMap<String, String>) devices,
                "",
                activity.getString(R.string.pref_device_to_pretend_to_be_default)
        );
        return devices;
    }

    private boolean isDeviceDefinitionValid(String spoofDevice) {
        PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
        deviceInfoProvider.setProperties(new SpoofDeviceManager(activity).getProperties(spoofDevice));
        deviceInfoProvider.setLocaleString(Locale.getDefault().toString());
        return deviceInfoProvider.isValid();
    }

    private boolean showRequestDialog(boolean logOut) {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(PREFERENCE_DEVICE_DEFINITION_REQUESTED, true)
                .apply();
        ;
        return true;
    }

    private AlertDialog showLogOutDialog() {
        new MaterialStyledDialog.Builder(activity)
                .setHeaderDrawable(R.drawable.header_01)
                .setTitle(R.string.dialog_title_logout)
                .setDescription(R.string.pref_device_to_pretend_to_be_toast)
                .setPositiveText(android.R.string.yes)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        new RequestOnClickListener(activity, true);
                    }
                })
                .setNegativeText(R.string.dialog_two_factor_cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        new RequestOnClickListener(activity, false);
                    }
                })
                .setCancelable(true)
                .withDialogAnimation(true)
                .show();
        return null;
    }

    private void finishAll() {
        new PlayStoreApiAuthenticator(activity.getApplicationContext()).logout();
        GalaxyActivity.cascadeFinish();
        activity.finish();
    }

    class RequestOnClickListener implements DialogInterface.OnClickListener {

        private boolean logOut;
        private boolean askedAlready;

        public RequestOnClickListener(Activity activity, boolean logOut) {
            askedAlready = PreferenceActivity.getBoolean(activity, PREFERENCE_DEVICE_DEFINITION_REQUESTED);
            this.logOut = logOut;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
            if (askedAlready) {
                if (logOut) {
                    finishAll();
                }
            } else {
                showRequestDialog(logOut);
            }
        }
    }

    class FinishingOnClickListener implements DialogInterface.OnClickListener {

        private boolean logOut;

        public FinishingOnClickListener(boolean logOut) {
            this.logOut = logOut;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
            if (logOut) {
                finishAll();
            }
        }
    }
}
