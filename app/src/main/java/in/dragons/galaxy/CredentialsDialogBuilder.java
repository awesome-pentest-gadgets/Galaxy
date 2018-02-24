package in.dragons.galaxy;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.github.yeriomin.playstoreapi.AuthException;
import com.github.yeriomin.playstoreapi.GooglePlayException;
import com.github.yeriomin.playstoreapi.TokenDispenserException;

import java.io.IOException;

import in.dragons.galaxy.task.playstore.CloneableTask;
import in.dragons.galaxy.task.playstore.PlayStoreTask;

abstract public class CredentialsDialogBuilder {

    protected Context context;
    protected PlayStoreTask caller;

    public CredentialsDialogBuilder(Context context) {
        this.context = context;
    }

    public void setCaller(PlayStoreTask caller) {
        this.caller = caller;
    }

    abstract public Dialog show();

    abstract protected static class CheckCredentialsTask extends PlayStoreTask<Void> {

        protected PlayStoreTask caller;

        public void setCaller(PlayStoreTask caller) {
            this.caller = caller;
        }

        static private final String APP_PASSWORDS_URL = "https://security.google.com/settings/security/apppasswords";

        abstract protected CredentialsDialogBuilder getDialogBuilder();

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (success()) {
                new FirstLaunchChecker(context).setLoggedIn();
                android.os.Process.killProcess(android.os.Process.myPid());
                if (caller instanceof CloneableTask) {
                    Log.i(getClass().getSimpleName(), caller.getClass().getSimpleName() + " is cloneable. Retrying.");
                    ((PlayStoreTask) ((CloneableTask) caller).clone()).execute();
                }
            }
        }

        @Override
        protected void processException(Throwable e) {
            super.processException(e);
            if ((e instanceof GooglePlayException && ((GooglePlayException) e).getCode() == 500)
                    || (e instanceof AuthException && !TextUtils.isEmpty(((AuthException) e).getTwoFactorUrl()))
                    ) {
                return;
            }
            CredentialsDialogBuilder builder = getDialogBuilder();
            if (null != caller) {
                builder.setCaller(caller);
            }
            if (ContextUtil.isAlive(context)) {
                builder.show();
            }
        }

        @Override
        protected void processIOException(IOException e) {
            super.processIOException(e);
            if (e instanceof TokenDispenserException) {
                ContextUtil.toast(context, R.string.error_token_dispenser_problem);
            } else if (e instanceof GooglePlayException && ((GooglePlayException) e).getCode() == 500) {
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PreferenceActivity.PREFERENCE_BACKGROUND_UPDATE_INTERVAL, "-1").commit();
                ContextUtil.toast(context, R.string.error_invalid_device_definition);
                context.startActivity(new Intent(context, PreferenceActivity.class));
            }
        }

        @Override
        protected void processAuthException(AuthException e) {
            if (e instanceof CredentialsEmptyException) {
                Log.w(getClass().getSimpleName(), "Credentials empty");
            } else if (null != e.getTwoFactorUrl()) {
                getTwoFactorAuthDialog().show();
            } else {
                ContextUtil.toast(context, R.string.error_incorrect_password);
            }
        }

        private MaterialStyledDialog getTwoFactorAuthDialog() {
            MaterialStyledDialog.Builder builder = new MaterialStyledDialog.Builder(context);
            return builder
                    .setDescription(R.string.dialog_message_two_factor)
                    .setTitle(R.string.dialog_title_two_factor)
                    .setPositiveText(R.string.dialog_two_factor_create_password)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(APP_PASSWORDS_URL));
                            if (i.resolveActivity(context.getPackageManager()) != null) {
                                context.startActivity(i);
                            } else {
                                Log.e(getClass().getSimpleName(), "No application available to handle http links... very strange");
                            }
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    })
                    .setNegativeText(R.string.dialog_two_factor_cancel)
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    })
                    .withDialogAnimation(true)
                    .show();
        }
    }
}
