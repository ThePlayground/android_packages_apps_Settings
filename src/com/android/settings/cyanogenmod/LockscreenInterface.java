
package com.android.settings.cyanogenmod;

import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.CMDProcessor;
import com.android.settings.Helpers;

public class LockscreenInterface extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    
    private static final String TAG = "Lockscreens";
    
    public static final int REQUEST_PICK_WALLPAPER = 199;
    public static final int SELECT_WALLPAPER = 3;
    
    private static final String WALLPAPER_NAME = "lockscreen_wallpaper.jpg";

    CheckBoxPreference mLockscreenLandscape;
    
    Preference mLockscreenWallpaper;
    
    ArrayList<String> keys = new ArrayList<String>();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        keys.add(Settings.System.LOCKSCREEN_LANDSCAPE);
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.lockscreen_interface_settings);

        mLockscreenWallpaper = findPreference("wallpaper");

        for (String key : keys) {
            try {
                ((CheckBoxPreference) findPreference(key)).setChecked(Settings.System.getInt(getActivity().getContentResolver(), key) == 1);
            } catch (SettingNotFoundException e) {
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if (!isSDPresent) {
            mLockscreenWallpaper.setEnabled(false);
            mLockscreenWallpaper.setSummary("Sdcard Unavailable");

        }
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mLockscreenWallpaper) {

            int width = getActivity().getWallpaperDesiredMinimumWidth();
            int height = getActivity().getWallpaperDesiredMinimumHeight();
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            float spotlightX = (float) display.getWidth() / width;
            float spotlightY = (float) display.getHeight() / height;

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("aspectX", width);
            intent.putExtra("aspectY", height);
            intent.putExtra("outputX", width);
            intent.putExtra("outputY", height);
            intent.putExtra("scale", true);
            // intent.putExtra("return-data", false);
            intent.putExtra("spotlightX", spotlightX);
            intent.putExtra("spotlightY", spotlightY);
            startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
            return true;

        } else if (keys.contains(preference.getKey())) {
            Log.e("RC_Lockscreens", "key: " + preference.getKey());
            return Settings.System.putInt(getActivity().getContentResolver(), preference.getKey(), ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.lockscreens, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.remove_wallpaper:
                Helpers.getMount("rw");
                File f = new File(mContext.getFilesDir(), WALLPAPER_NAME);
                Log.e(TAG, mContext.deleteFile(WALLPAPER_NAME) + "");
                Log.e(TAG, mContext.deleteFile(WALLPAPER_NAME) + "");
                Helpers.getMount("ro");
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
    
    private Uri getLockscreenExternalUri() {
        File dir = mContext.getExternalCacheDir();
        File wallpaper = new File(dir, WALLPAPER_NAME);

        return Uri.fromFile(wallpaper);
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Stub for future preference options
        return false;
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_WALLPAPER) {

                Helpers.getMount("rw");
                FileOutputStream wallpaperStream = null;
                try {
                    wallpaperStream = mContext.openFileOutput(WALLPAPER_NAME, Context.MODE_PRIVATE);
                } catch (FileNotFoundException e) {
                    return; // NOOOOO
                }

                Uri selectedImageUri = data.getData();
                Log.d(TAG, "Selected image uri: " + selectedImageUri);
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(selectedImageUri.getPath()));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, wallpaperStream);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Error opening file!", e);
                    Toast.makeText(getActivity(), "Error opening file!", Toast.LENGTH_LONG).show();
                    return;
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Out of memory!", e);
                    Toast.makeText(getActivity(), "Out of memory! Try a smaller image.", Toast.LENGTH_LONG).show();
                    return;
                }
                Helpers.getMount("ro");

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

}