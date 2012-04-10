
package com.android.settings.cyanogenmod;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.CMDProcessor;
import com.android.settings.CMDProcessor.CommandResult;
import com.android.settings.Helpers;

public class DensityChanger extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "DensityChanger";

    ListPreference mStockDensity;
    Preference mReboot;
    Preference mClearMarketData;
    Preference mOpenMarket;
    ListPreference mCustomDensity;

    private static final int MSG_DATA_CLEARED = 500;

    private static final int DIALOG_DENSITY = 101;
    private static final int DIALOG_WARN_DENSITY = 102;

    int newDensityValue;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_DATA_CLEARED:
                    mClearMarketData.setSummary(R.string.clear_market_data_cleared);
                    break;
            }

        };
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lcd_density);

        String currentDensity = SystemProperties.get("ro.sf.lcd_density");
        PreferenceScreen prefs = getPreferenceScreen();

        mStockDensity = (ListPreference) findPreference("stock_density");
        mStockDensity.setOnPreferenceChangeListener(this);

        mReboot = findPreference("reboot");
        mClearMarketData = findPreference("clear_market_data");
        mOpenMarket = findPreference("open_market");

        mCustomDensity = (ListPreference) findPreference("lcd_density");
        mCustomDensity.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        mClearMarketData.setSummary("");
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mReboot) {
            PowerManager pm = (PowerManager) getActivity()
                    .getSystemService(Context.POWER_SERVICE);
            pm.reboot("Resetting density");
            return true;

        } else if (preference == mClearMarketData) {

            new ClearMarketDataTask().execute("");
            return true;

        } else if (preference == mOpenMarket) {
            Intent openMarket = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_APP_MARKET)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName activityName = openMarket.resolveActivity(getActivity()
                    .getPackageManager());
            if (activityName != null) {
                mContext.startActivity(openMarket);
            } else {
                preference.setSummary("Couldn't open Google Play! If you're sure it's installed, open it from the launcher.");
            }
            return true;

        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        LayoutInflater factory = LayoutInflater.from(mContext);

        switch (dialogId) {
            case DIALOG_DENSITY:
                final View textEntryView = factory.inflate(
                        R.layout.alert_dialog_lcd, null);
                return new AlertDialog.Builder(getActivity())
                        .setTitle("Set custom density")
                        .setView(textEntryView)
                        .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                EditText dpi = (EditText) textEntryView.findViewById(R.id.dpi_edit);
                                Editable text = dpi.getText();
                                Log.i(TAG, text.toString());

                                try {
                                    newDensityValue = Integer.parseInt(text.toString());
                                    showDialog(DIALOG_WARN_DENSITY);
                                } catch (Exception e) {
                                    mCustomDensity.setSummary("INVALID DENSITY!");
                                }

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();
                            }
                        }).create();
            case DIALOG_WARN_DENSITY:
                return new AlertDialog.Builder(getActivity())
                        .setTitle("WARNING!")
                        .setMessage(
                                "Changing your LCD density can cause unexpected app behavior. If you encounter market app incompatibility please return here and restart the process from step 1.")
                        .setCancelable(false)
                        .setNeutralButton("Got it!", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setLcdDensity(newDensityValue, false);
                                dialog.dismiss();
                                mCustomDensity.setSummary(newDensityValue + "");

                            }
                        })
                        .setPositiveButton("Reboot now", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setLcdDensity(newDensityValue, true);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .create();
        }
        return null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomDensity) {
            String strValue = (String) newValue;
            if (strValue.equals("custom")) {
                showDialog(DIALOG_DENSITY);
                return true;
            } else {
                newDensityValue = Integer.parseInt((String) newValue);
                showDialog(DIALOG_WARN_DENSITY);
                return true;
            }
        } else if (preference == mStockDensity) {
            newDensityValue = Integer.parseInt((String) newValue);
            setLcdDensity(newDensityValue, false);
            mStockDensity.setSummary("Density set to: " + newDensityValue);
            return true;
        }

        return false;
    }

    private void setLcdDensity(int newDensity, boolean reboot) {
        Helpers.getMount("rw");
        new CMDProcessor().su.runWaitFor("busybox sed -i 's|ro.sf.lcd_density=.*|"
                + "ro.sf.lcd_density" + "=" + newDensity + "|' " + "/system/build.prop");
        Helpers.getMount("ro");
        if (reboot) {
            PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
            pm.reboot("Resetting density");
        }
    }

    class ClearUserDataObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            mHandler.sendEmptyMessage(MSG_DATA_CLEARED);
        }
    }

    private class ClearMarketDataTask extends AsyncTask<String, Void, Boolean> {
        protected Boolean doInBackground(String... stuff) {
            String vending = "/data/data/com.android.vending/";
            CommandResult cr = new CMDProcessor().su.runWaitFor("ls " + vending);

            if (cr.stdout == null)
                return false;

            for (String dir : cr.stdout.split("\n")) {
                if (!dir.equals("lib")) {
                    String c = "rm -r " + vending + dir;
                    // Log.i(TAG, c);
                    if (!new CMDProcessor().su.runWaitFor(c).success())
                        return false;
                }
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            mClearMarketData.setSummary(result ? "Market data cleared."
                    : "Market data couldn't be cleared, please clear it yourself!");
        }
    }
}
