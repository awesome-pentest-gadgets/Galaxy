package in.dragons.galaxy;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;

import java.util.regex.Pattern;

import in.dragons.galaxy.fragment.FilterMenu;
import in.dragons.galaxy.model.App;
import in.dragons.galaxy.task.playstore.DetailsTask;
import in.dragons.galaxy.task.playstore.SearchTask;

public class SearchActivity extends EndlessScrollActivity {

    public static final String PUB_PREFIX = "pub:";

    private String query;

    static protected boolean actionIs(Intent intent, String action) {
        return null != intent && null != intent.getAction() && intent.getAction().equals(action);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String newQuery = getQuery(intent);
        if (actionIs(intent, Intent.ACTION_VIEW) && looksLikeAPackageId(newQuery)) {
            Log.i(getClass().getSimpleName(), "Following search suggestion to app page: " + newQuery);
            startActivity(DetailsActivity.getDetailsIntent(this, newQuery));
            finish();
            return;
        }
        Log.i(getClass().getSimpleName(), "Searching: " + newQuery);
        if (null != newQuery && !newQuery.equals(this.query)) {
            clearApps();
            this.query = newQuery;
            setTitle(getTitleString());
            if (looksLikeAPackageId(query)) {
                Log.i(getClass().getSimpleName(), query + " looks like a package id");
                checkPackageId(query);
            } else {
                loadApps();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.filter_category).setVisible(true);
        return result;
    }

    @Override
    protected SearchTask getTask() {
        SearchTask task = new SearchTask(iterator);
        task.setQuery(query);
        task.setFilter(new FilterMenu(this).getFilterPreferences());
        return task;
    }

    private String getTitleString() {
        return query.startsWith(PUB_PREFIX)
                ? getString(R.string.apps_by, query.substring(PUB_PREFIX.length()))
                : getString(R.string.activity_title_search, query)
                ;
    }

    private String getQuery(Intent intent) {
        if (intent.getScheme() != null
                && (intent.getScheme().equals("market")
                || intent.getScheme().equals("http")
                || intent.getScheme().equals("https")
        )
                ) {
            return intent.getData().getQueryParameter("q");
        }
        if (actionIs(intent, Intent.ACTION_SEARCH)) {
            return intent.getStringExtra(SearchManager.QUERY);
        } else if (actionIs(intent, Intent.ACTION_VIEW)) {
            return intent.getDataString();
        }
        return null;
    }

    private boolean looksLikeAPackageId(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        String pattern = "([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)+[\\p{L}_$][\\p{L}\\p{N}_$]*";
        Pattern r = Pattern.compile(pattern);
        return r.matcher(query).matches();
    }

    private void checkPackageId(String packageId) {
        DetailsTask task = new CheckPackageIdTask(this);
        task.setContext(this);
        task.setPackageName(packageId);
        task.execute();
    }

    static private class CheckPackageIdTask extends DetailsTask {

        private SearchActivity activity;

        public CheckPackageIdTask(SearchActivity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPostExecute(App app) {
            super.onPostExecute(app);
            if (null != app && ContextUtil.isAlive(activity)) {
                DetailsActivity.app = app;
                showPackageIdDialog(app.getPackageName());
            } else {
                activity.finish();
            }
        }

        private MaterialStyledDialog showPackageIdDialog(final String packageId) {
            return new MaterialStyledDialog.Builder(activity)
                    .setHeaderDrawable(R.drawable.header_04)
                    .setTitle(R.string.dialog_title_package_id)
                    .setDescription(R.string.dialog_message_package_id)
                    .setPositiveText(android.R.string.yes)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            activity.startActivity(DetailsActivity.getDetailsIntent(activity, packageId));
                            activity.finish();
                        }
                    })
                    .setNegativeText(R.string.dialog_two_factor_cancel)
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            activity.loadApps();
                        }
                    })
                    .withDialogAnimation(true)
                    .setCancelable(true)
                    .show();
        }
    }
}
