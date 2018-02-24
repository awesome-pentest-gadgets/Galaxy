package in.dragons.galaxy;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.ImageViewCompat;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Properties;
import java.util.TimeZone;

public class AccountsActivity extends GalaxyActivity {

    AccountTypeDialogBuilder accountTypeDialogBuilder = new AccountTypeDialogBuilder(this);

    SharedPreferences sharedPreferences;
    String deviceName;
    ImageView spoofed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setTheme(sharedPreferences.getBoolean("THEME", true) ? R.style.AppTheme : R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accounts_layout);
        onCreateDrawer(savedInstanceState);

        deviceName = sharedPreferences.getString(PreferenceActivity.PREFERENCE_DEVICE_TO_PRETEND_TO_BE, "");

        spoofed = (ImageView) findViewById(R.id.spoofed_indicator);

        if (isSpoofed())
            drawSpoofedDevice();
        else
            drawDevice();

        notifyConnected(this);

        if (isValidEmail(Email) && isConnected()) {
            new GoogleAccountInfo(Email) {
                @Override
                public void onPostExecute(String result) {
                    parseRAW(result);
                }
            }.execute();
            drawGoogle();
        } else if (isDummyEmail())
            drawDummy();
        setFab();
    }

    public boolean isSpoofed() {
        return (deviceName.contains("device-"));
    }

    public void drawDevice() {
        ImageViewCompat.setImageTintList(spoofed, ColorStateList.valueOf((getResources().getColor(R.color.colorGreen))));
        ((TextView) findViewById(R.id.device_model)).setText(Build.MODEL + " (" + Build.DEVICE + ")");
        ((TextView) findViewById(R.id.device_manufacturer)).setText("By : " + Build.MANUFACTURER);
        ((TextView) findViewById(R.id.device_architect)).setText("Board : " + Build.BOARD);
        ((TextView) findViewById(R.id.device_timezone)).setText("Timezone : " + (CharSequence) TimeZone.getDefault().getDisplayName());
        Display mDisplay = (this).getWindowManager().getDefaultDisplay();
        ((TextView) findViewById(R.id.device_resolution)).setText(mDisplay.getWidth() + " x " + mDisplay.getHeight());
        ((TextView) findViewById(R.id.device_architect)).setText("Board : " + Build.BOARD);
        ((TextView) findViewById(R.id.device_api)).setText("API Level " + Build.VERSION.SDK);
        ((TextView) findViewById(R.id.device_cpu)).setText(Build.CPU_ABI);
    }

    public void drawSpoofedDevice() {
        ImageViewCompat.setImageTintList(spoofed, ColorStateList.valueOf((getResources().getColor(R.color.colorRed))));
        Properties properties = new SpoofDeviceManager(this).getProperties(deviceName);
        String Model = properties.getProperty("UserReadableName");
        ((TextView) findViewById(R.id.device_model)).setText(Model.substring(0, Model.indexOf('(')) + " (" + properties.getProperty("Build.DEVICE") + ")");
        ((TextView) findViewById(R.id.device_manufacturer)).setText("By : " + properties.getProperty("Build.MANUFACTURER"));
        ((TextView) findViewById(R.id.device_architect)).setText("Board : " + properties.getProperty("Build.HARDWARE"));
        ((TextView) findViewById(R.id.device_timezone)).setText("Timezone : " + properties.getProperty("TimeZone"));
        ((TextView) findViewById(R.id.device_resolution)).setText(properties.getProperty("Screen.Width") + " x " + properties.getProperty("Screen.Height"));
        ((TextView) findViewById(R.id.device_api)).setText("API Level " + properties.getProperty("Build.VERSION.SDK_INT"));
        String Platforms = properties.getProperty("Platforms");
        ((TextView) findViewById(R.id.device_cpu)).setText(Platforms.substring(0, Platforms.indexOf(',')));
    }

    public void drawDummy() {
        ((LinearLayout) findViewById(R.id.dummy_container)).setVisibility(View.VISIBLE);
        ((LinearLayout) findViewById(R.id.dummy_action)).setVisibility(View.VISIBLE);
        ((LinearLayout) findViewById(R.id.no_dummy)).setVisibility(View.GONE);
        TextView dummyEmail = (TextView) findViewById(R.id.dummy_email);
        dummyEmail.setText(Email);
        TextView gsfIdView = (TextView) findViewById(R.id.dummy_gsf);
        gsfIdView.setText("GSF ID : " + sharedPreferences.getString(PlayStoreApiAuthenticator.PREFERENCE_GSF_ID, ""));

        Button logout = (Button) findViewById(R.id.account_logout);
        logout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showLogOutDialog();
            }
        });
        Button switched = (Button) findViewById(R.id.account_switch);
        switched.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                accountTypeDialogBuilder.logInWithPredefinedAccount();
            }
        });
    }

    public void drawGoogle() {
        if (Email != "") {
            ((LinearLayout) findViewById(R.id.google_container)).setVisibility(View.VISIBLE);
            ((LinearLayout) findViewById(R.id.google_action)).setVisibility(View.VISIBLE);
            ((LinearLayout) findViewById(R.id.no_google)).setVisibility(View.GONE);
            TextView googleEmail = (TextView) findViewById(R.id.google_email);
            googleEmail.setText(Email);
            TextView gsfIdView = (TextView) findViewById(R.id.google_gsf);
            gsfIdView.setText("GSF ID : " + sharedPreferences.getString(PlayStoreApiAuthenticator.PREFERENCE_GSF_ID, ""));

            Button button = (Button) findViewById(R.id.google_logout);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showLogOutDialog();
                }
            });
        }
    }


    public void setFab() {
        FloatingActionButton dummyfab = (FloatingActionButton) findViewById(R.id.dummy_login);
        dummyfab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                accountTypeDialogBuilder.logInWithPredefinedAccount();
            }
        });

        FloatingActionButton googlefab = (FloatingActionButton) findViewById(R.id.google_login);
        googlefab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                accountTypeDialogBuilder.showCredentialsDialog();
            }
        });
    }

    public void parseRAW(String rawData) {
        TextView googleName = (TextView) findViewById(R.id.google_name);
        googleName.setText(rawData.substring(rawData.indexOf("<name>") + 6, rawData.indexOf("</name>")));
        Picasso.with(this)
                .load(rawData.substring(rawData.indexOf("<gphoto:thumbnail>") + 18, rawData.lastIndexOf("</gphoto:thumbnail>")))
                .placeholder(R.drawable.ic_user_placeholder)
                .transform(new CircleTransform())
                .into(((ImageView) findViewById(R.id.google_avatar)));
    }
}
