package in.dragons.galaxy;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;

import java.io.IOException;

import in.dragons.galaxy.task.playstore.PlayStoreTask;

public class AccountTypeDialogBuilder extends CredentialsDialogBuilder {

    public AccountTypeDialogBuilder(Context context) {
        super(context);
    }

    @Override
    public MaterialStyledDialog show() {
        MaterialStyledDialog.Builder builder = new MaterialStyledDialog.Builder(context);
        final MaterialStyledDialog materialStyledDialog = builder
                .setHeaderDrawable(R.drawable.header_00)
                .setTitle(R.string.dialog_account_type_title)
                .setDescription(R.string.dialog_account_type_description)
                .setPositiveText(R.string.login_google)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        showCredentialsDialog();
                    }
                })
                .setNegativeText(R.string.login_dummy)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        logInWithPredefinedAccount();
                    }
                })
                .withDialogAnimation(true)
                .setCancelable(false)
                .show();
        return materialStyledDialog;
    }

    public Dialog showCredentialsDialog() {
        UserProvidedAccountDialogBuilder builder = new UserProvidedAccountDialogBuilder(this.context);
        builder.setCaller(caller);
        return builder.show();
    }

    public void refreshToken() {
        RefreshTokenTask task = new RefreshTokenTask();
        task.setCaller(caller);
        task.setContext(context);
        task.execute();
    }

    public void logInWithPredefinedAccount() {
        LoginTask task = new LoginTask();
        task.setCaller(caller);
        task.setContext(context);
        task.prepareDialog(R.string.dialog_message_logging_in_predefined, R.string.dialog_title_logging_in);
        task.execute();
    }

    abstract private static class AppProvidedCredentialsTask extends CredentialsDialogBuilder.CheckCredentialsTask {

        abstract protected void payload() throws IOException;

        @Override
        protected CredentialsDialogBuilder getDialogBuilder() {
            return new AccountTypeDialogBuilder(context);
        }

        @Override
        protected Void doInBackground(String[] params) {
            try {
                payload();
            } catch (IOException e) {
                exception = e;
            }
            return null;
        }
    }

    private static class RefreshTokenTask extends AppProvidedCredentialsTask {

        @Override
        public void setCaller(PlayStoreTask caller) {
            super.setCaller(caller);
            setProgressIndicator(caller.getProgressIndicator());
        }

        @Override
        protected void payload() throws IOException {
            new PlayStoreApiAuthenticator(context).refreshToken();
        }
    }

    private static class LoginTask extends AppProvidedCredentialsTask {

        @Override
        protected void payload() throws IOException {
            new PlayStoreApiAuthenticator(context).login();
        }
    }
}
