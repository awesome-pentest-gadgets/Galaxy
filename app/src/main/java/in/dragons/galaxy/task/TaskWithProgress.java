package in.dragons.galaxy.task;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;

import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;

import in.dragons.galaxy.ContextUtil;
import in.dragons.galaxy.R;

abstract public class TaskWithProgress<T> extends AsyncTask<String, Void, T> {

    protected Context context;
    protected MaterialStyledDialog progressDialog;
    protected View progressIndicator;

    public View getProgressIndicator() {
        return progressIndicator;
    }

    public void setProgressIndicator(View progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void prepareDialog(int messageId, int titleId) {
        MaterialStyledDialog.Builder builder=new MaterialStyledDialog.Builder(context)
                .setHeaderDrawable(R.drawable.header_06)
                .setTitle(context.getString(titleId))
                .setDescription(context.getString(messageId))
                .setNeutralText("Wait a moment please..")
                .withDialogAnimation(true)
                .setCancelable(false);
        this.progressDialog = builder.show();
    }

    @Override
    protected void onPreExecute() {
        if (null != this.progressDialog && ContextUtil.isAlive(context)) {
            this.progressDialog.show();
        }
        if (null != progressIndicator) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPostExecute(T result) {
        if (null != this.progressDialog && ContextUtil.isAlive(context) && progressDialog.isShowing()) {
            this.progressDialog.dismiss();
        }
        if (null != progressIndicator) {
            progressIndicator.setVisibility(View.GONE);
        }
    }
}
