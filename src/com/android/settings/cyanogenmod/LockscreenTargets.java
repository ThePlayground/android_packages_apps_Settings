/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.widget.multiwaveview.MultiWaveView;
import com.android.internal.widget.multiwaveview.TargetDrawable;
import com.android.settings.R;
import com.android.settings.Utils;

public class LockscreenTargets extends Fragment implements ShortcutPickHelper.OnPickListener, MultiWaveView.OnTriggerListener {

    private MultiWaveView mWaveView;
    private ShortcutPickHelper mPicker;
    private ArrayList<TargetInfo> mTargetStore = new ArrayList<TargetInfo>();
    private int mTargetOffset;
    private boolean isLandscape;

    class TargetInfo {
        String uri;
        Drawable icon;
        TargetInfo(String in, Drawable target) {
            uri = in;
            icon = target;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        container.setPadding(0, 0, 0, 0);
        View view = inflater.inflate(R.layout.lockscreen_targets, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder tString = new StringBuilder();
                final int maxTargets = Utils.isScreenLarge() ? MultiWaveView.MAX_TABLET_TARGETS : MultiWaveView.MAX_PHONE_TARGETS;
                for (int i = mTargetOffset + 1; i <= mTargetOffset + maxTargets; i++) {
                    String uri = mTargetStore.get(i).uri;
                    tString.append(uri);
                    tString.append("|");
                }
                tString.deleteCharAt(tString.length() - 1);
                Settings.System.putString(getActivity().getContentResolver(), Settings.System.LOCKSCREEN_TARGETS, tString.toString());
            }
        });
        getActivity().findViewById(R.id.reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializeView(MultiWaveView.DEFAULT_TARGETS);
            }
        });
        initializeView(Settings.System.getString(getActivity().getContentResolver(), Settings.System.LOCKSCREEN_TARGETS));
    }

    private void initializeView(String input) {
        if (input == null) {
            input = MultiWaveView.DEFAULT_TARGETS;
        }
        mTargetStore.clear();
        final int maxTargets = Utils.isScreenLarge() ? MultiWaveView.MAX_TABLET_TARGETS : MultiWaveView.MAX_PHONE_TARGETS;
        final int targetInset = Utils.isScreenLarge() ? MultiWaveView.TABLET_TARGET_INSET : MultiWaveView.PHONE_TARGET_INSET;
        final Resources res = getResources();
        final PackageManager packMan = getActivity().getPackageManager();
        final Drawable dd = getResources().getDrawable(com.android.internal.R.drawable.ic_lockscreen_lock_pressed);
        final InsetDrawable backing = new InsetDrawable(dd, 0, 0, 0, 0);
        final String[] targetStore = input.split("\\|");
        isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        mTargetOffset = isLandscape && !Utils.isScreenLarge() ? 2 : 0;
        if (isLandscape && !Utils.isScreenLarge()) {
            mTargetStore.add(new TargetInfo("", null));
            mTargetStore.add(new TargetInfo("", null));
        }
        mTargetStore.add(new TargetInfo("unlock", res.getDrawable(com.android.internal.R.drawable.ic_lockscreen_unlock)));
        for (int cc = 0; cc < 8 - mTargetOffset - 1; cc++) {
            String uri = null;
            Drawable draw = null;
            InsetDrawable[] inactivelayer = new InsetDrawable[2];
            inactivelayer[0] = backing;
            StateListDrawable states = new StateListDrawable();
            InsetDrawable[] activelayer = new InsetDrawable[2];
            activelayer[0] = new InsetDrawable(getResources().getDrawable(com.android.internal.R.drawable.ic_lockscreen_target_activated), 0, 0, 0, 0);
            if (cc < targetStore.length && cc < maxTargets) {
                uri = targetStore[cc];
                if (!uri.equals(MultiWaveView.EMPTY_TARGET.toLowerCase())) {
                    try {
                        // Sanity check
                        Intent in = Intent.parseUri(uri, 0);
                        ActivityInfo aInfo = in.resolveActivityInfo(packMan, PackageManager.GET_ACTIVITIES);
                        if (aInfo != null) {
                            draw = aInfo.loadIcon(packMan);
                        } else {
                            draw = getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                        }
                        inactivelayer[1] = new InsetDrawable(draw, targetInset, targetInset, targetInset, targetInset);
                        activelayer[1] = new InsetDrawable(draw, targetInset, targetInset, targetInset, targetInset);
                    } catch (Exception e) {
                    }
                }
            } else if (cc >= maxTargets) {
                mTargetStore.add(new TargetInfo("", null));
                continue;
            }
            if (activelayer[1] == null || inactivelayer[1] == null) {
                inactivelayer[1] = new InsetDrawable(getResources().getDrawable(android.R.drawable.ic_delete), targetInset, targetInset, targetInset, targetInset);
                activelayer[1] = new InsetDrawable(getResources().getDrawable(android.R.drawable.ic_delete), targetInset, targetInset, targetInset, targetInset);
            }
            LayerDrawable aa = new LayerDrawable(inactivelayer);
            aa.setId(0, 0);
            aa.setId(1, 1);
            LayerDrawable bb = new LayerDrawable(activelayer);
            bb.setId(0, 0);
            bb.setId(1, 1);
            states.addState(new int[] {android.R.attr.state_enabled, -android.R.attr.state_active, -android.R.attr.state_focused}, aa);
            states.addState(new int[] {android.R.attr.state_enabled, android.R.attr.state_active, -android.R.attr.state_focused}, bb);
            mTargetStore.add(new TargetInfo(uri,states));
        }
        mPicker = new ShortcutPickHelper(getActivity(), this);
        mWaveView = ((MultiWaveView) getActivity().findViewById(R.id.lock_target));
        mWaveView.setOnTriggerListener(this);
        ArrayList<TargetDrawable> tDraw = new ArrayList<TargetDrawable>();
        for (TargetInfo i : mTargetStore) {
            if (i != null) {
                tDraw.add(new TargetDrawable(res,i.icon));
            } else {
                tDraw.add(new TargetDrawable(res,null));
            }
        }
        mWaveView.setTargetResources(tDraw);
        mWaveView.setTag(null);
    }

    private void setTarget(int cTarget, String uri, Drawable pD) {
        ((LayerDrawable)((StateListDrawable)mTargetStore.get(cTarget).icon).getStateDrawable(0)).setDrawableByLayerId(1, pD);
        ((LayerDrawable)((StateListDrawable)mTargetStore.get(cTarget).icon).getStateDrawable(1)).setDrawableByLayerId(1, pD);
        mTargetStore.get(cTarget).uri = uri;
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        try {
            Intent i = Intent.parseUri(uri, 0);
            Integer cTarget = (Integer) mWaveView.getTag();
            PackageManager pm = getActivity().getPackageManager();
            ActivityInfo aInfo = i.resolveActivityInfo(pm, PackageManager.GET_ACTIVITIES);
            Drawable icon = null;
            if (aInfo != null) {
                icon = aInfo.loadIcon(pm);
            } else {
                icon = getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            }
            final int targetInset = Utils.isScreenLarge() ? MultiWaveView.TABLET_TARGET_INSET : MultiWaveView.PHONE_TARGET_INSET;
            InsetDrawable pD = new InsetDrawable(icon, targetInset, targetInset, targetInset, targetInset);
            setTarget(cTarget, uri, pD);
        } catch (Exception e) {
        }
        mWaveView.setTag(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) != null &&
                data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME).equals(MultiWaveView.EMPTY_TARGET)) {
            Integer cTarget = (Integer) mWaveView.getTag();
            InsetDrawable pD = new InsetDrawable(getResources().getDrawable(android.R.drawable.ic_delete), 60, 60, 60, 60);
            setTarget(cTarget, MultiWaveView.EMPTY_TARGET.toLowerCase(), pD);
            mWaveView.setTag(null);
        } else if (requestCode == Activity.RESULT_CANCELED || resultCode == Activity.RESULT_CANCELED) {
            mWaveView.setTag(null);
        } else {
            mPicker.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onGrabbed(View v, int handle) {
    }

    @Override
    public void onReleased(View v, int handle) {

    }

    @Override
    public void onTrigger(View v, int target) {
        if ((target != 0 && (Utils.isScreenLarge() || !isLandscape)) || (target != 2 && !Utils.isScreenLarge() && isLandscape)) {
            mWaveView.setTag(target);
            mPicker.pickShortcut(new String[] {MultiWaveView.EMPTY_TARGET}, new ShortcutIconResource[] {
                    ShortcutIconResource.fromContext(getActivity(), android.R.drawable.ic_delete) }, getId());
        }
    }

    @Override
    public void onGrabbedStateChange(View v, int handle) {
    }
}