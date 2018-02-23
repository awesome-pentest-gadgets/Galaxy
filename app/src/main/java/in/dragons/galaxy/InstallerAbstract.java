package in.dragons.galaxy;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;

import java.io.File;

import in.dragons.galaxy.model.App;
import in.dragons.galaxy.notification.IgnoreUpdatesService;
import in.dragons.galaxy.notification.NotificationBuilder;
import in.dragons.galaxy.notification.NotificationManagerWrapper;

public abstract class InstallerAbstract {

    protected Context context;
    protected boolean background;

    static public Intent getOpenApkIntent(Context context, File file) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    abstract protected void install(App app);

    public InstallerAbstract(Context context) {
        Log.i(getClass().getSimpleName(), "Installer chosen");
        this.context = context;
        background = !(context instanceof Activity);
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public void verifyAndInstall(App app) {
        if (verify(app)) {
            Log.i(getClass().getSimpleName(), "Installing " + app.getPackageName());
            install(app);
        } else {
            sendBroadcast(app.getPackageName(), false);
        }
    }

    protected boolean verify(App app) {
        File apkPath = Paths.getApkPath(context, app.getPackageName(), app.getVersionCode());
        if (!apkPath.exists()) {
            Log.w(getClass().getSimpleName(), apkPath.getAbsolutePath() + " does not exist");
            return false;
        }
        if (!new ApkSignatureVerifier(context).match(app.getPackageName(), apkPath)) {
            Log.i(getClass().getSimpleName(), "Signature mismatch for " + app.getPackageName());
            ((GalaxyApplication) context.getApplicationContext()).removePendingUpdate(app.getPackageName());
            if (ContextUtil.isAlive(context)) {
                getSignatureMismatchDialog(app).show();
            } else {
                notifySignatureMismatch(app);
            }
            return false;
        }
        return true;
    }

    protected void notifyAndToast(int notificationStringId, int toastStringId, App app) {
        showNotification(notificationStringId, app);
        if (!background) {
            ContextUtil.toast(context, toastStringId, app.getDisplayName());
        }
    }

    protected void sendBroadcast(String packageName, boolean success) {
        Intent intent = new Intent(
                success
                        ? DetailsInstallReceiver.ACTION_PACKAGE_REPLACED_NON_SYSTEM
                        : DetailsInstallReceiver.ACTION_PACKAGE_INSTALLATION_FAILED
        );
        intent.setData(new Uri.Builder().scheme("package").opaquePart(packageName).build());
        context.sendBroadcast(intent);
    }

    private MaterialStyledDialog getSignatureMismatchDialog(final App app) {
        MaterialStyledDialog.Builder builder = new MaterialStyledDialog.Builder(context);
        builder
                .setDescription(R.string.details_signature_mismatch)
                .setPositiveText(android.R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        return;
                    }
                });
        if (new BlackWhiteListManager(context).isUpdatable(app.getPackageName())) {
            builder.setNeutralText(R.string.action_ignore)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            context.startService(getIgnoreIntent(app));
                        }
                    });
        }
        builder.withDialogAnimation(true);
        return builder.show();
    }

    private void notifySignatureMismatch(App app) {
        notifyAndToast(
                R.string.notification_download_complete_signature_mismatch,
                R.string.notification_download_complete_signature_mismatch_toast,
                app
        );
    }

    private void showNotification(int notificationStringId, App app) {
        File file = Paths.getApkPath(context, app.getPackageName(), app.getVersionCode());
        Intent openApkIntent = getOpenApkIntent(context, file);
        NotificationBuilder builder = NotificationManagerWrapper.getBuilder(context)
                .setIntent(openApkIntent)
                .setTitle(app.getDisplayName())
                .setMessage(context.getString(notificationStringId));
        if (new BlackWhiteListManager(context).isUpdatable(app.getPackageName())) {
            builder.addAction(
                    R.drawable.ic_cancel,
                    R.string.action_ignore,
                    PendingIntent.getService(context, 0, getIgnoreIntent(app), PendingIntent.FLAG_UPDATE_CURRENT)
            );
        }
        new NotificationManagerWrapper(context).show(app.getDisplayName(), builder.build());
    }

    private Intent getIgnoreIntent(App app) {
        Intent intentIgnore = new Intent(context, IgnoreUpdatesService.class);
        intentIgnore.putExtra(IgnoreUpdatesService.PACKAGE_NAME, app.getPackageName());
        intentIgnore.putExtra(IgnoreUpdatesService.VERSION_CODE, app.getVersionCode());
        return intentIgnore;
    }
}
