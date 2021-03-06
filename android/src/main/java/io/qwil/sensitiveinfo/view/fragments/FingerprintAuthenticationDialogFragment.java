/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Modifications copyright (C) 2018 Sowa Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package io.qwil.sensitiveinfo.view.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;

import io.qwil.sensitiveinfo.R;
import io.qwil.sensitiveinfo.util.AppConstants;


/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
public class FingerprintAuthenticationDialogFragment extends DialogFragment
        implements FingerprintUiHelper.Callback {

    private Button mCancelButton;
    private View mFingerprintContent;

    private HashMap mStrings;
    private FingerprintUiHelper.Callback mCallback;
    private FingerprintManager.CryptoObject mCryptoObject;
    private FingerprintUiHelper mFingerprintUiHelper;
    private Activity mActivity;

    private SharedPreferences mSharedPreferences;

    public static FingerprintAuthenticationDialogFragment newInstance(HashMap strings) {
        FingerprintAuthenticationDialogFragment f = new FingerprintAuthenticationDialogFragment();

        // Supply an argument.
        Bundle args = new Bundle();
        args.putSerializable("strings", strings);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
        mStrings = (HashMap) getArguments().getSerializable("strings");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setCanceledOnTouchOutside(false);
        getDialog().setTitle(mStrings.containsKey("header") ? mStrings.get("header").toString() : getString(R.string.header));

        View v = inflater.inflate(R.layout.fingerprint_dialog_container, container, false);

        ((TextView) v.findViewById(R.id.fingerprint_description)).setText(
                mStrings.containsKey("description") ? mStrings.get("description").toString() : getString(R.string.fingerprint_description));
        ((TextView) v.findViewById(R.id.fingerprint_status)).setText(
                mStrings.containsKey("hint") ? mStrings.get("hint").toString() : getString(R.string.fingerprint_hint));

        mCancelButton = (Button) v.findViewById(R.id.cancel_button);
        mCancelButton.setText(
                mStrings.containsKey("cancel") ? mStrings.get("cancel").toString() : getString(R.string.cancel));
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onError(
                        AppConstants.E_AUTHENTICATION_CANCELLED,
                        mStrings.containsKey("cancelled") ? mStrings.get("cancelled").toString() : "Authentication was cancelled");
                dismiss();
            }
        });

        mFingerprintContent = v.findViewById(R.id.fingerprint_container);
        mFingerprintContent.setVisibility(View.VISIBLE);

        mFingerprintUiHelper = new FingerprintUiHelper(
                mActivity.getSystemService(FingerprintManager.class),
                (ImageView) v.findViewById(R.id.fingerprint_icon),
                (TextView) v.findViewById(R.id.fingerprint_status),
                mCancelButton,
                mStrings,
                this);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mFingerprintUiHelper.startListening(mCryptoObject);
    }

    @Override
    public void onPause() {
        super.onPause();
        mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        mCallback.onError(
                AppConstants.E_AUTHENTICATION_CANCELLED,
                mStrings.containsKey("cancelled") ? mStrings.get("cancelled").toString() : "Authentication was cancelled");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mActivity = getActivity();
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        mCryptoObject = cryptoObject;
    }

    /**
     * Sets the callback object to notify the caller about authentication success/error.
     */
    public void setCallback(FingerprintUiHelper.Callback callback) {
        mCallback = callback;
    }

    @Override
    public void onAuthenticated(FingerprintManager.AuthenticationResult result) {
        // Callback from FingerprintUiHelper. Let the activity know that authentication was
        // successful.
        mCallback.onAuthenticated(result);
        dismiss();
    }

    @Override
    public void onError(String errorCode, CharSequence errString) {
        mCallback.onError(errorCode, errString);
        dismiss();
    }
}