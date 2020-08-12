package com.element.plugin;

import org.apache.cordova.*;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONException;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.element.camera.ElementFaceAuthActivity;
import com.element.camera.ElementFaceEnrollActivity;
import com.element.camera.ElementFaceSDK;
import com.element.camera.ElementUserUtils;
//import com.element.camera.ProviderUtil;
import com.element.camera.UserInfo;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.element.plugin.ElementCordovaFaceSDK.ApiResult.RESULT_FAILURE;
import static com.element.plugin.ElementCordovaFaceSDK.ApiResult.RESULT_SUCCESS;

public class ElementCordovaFaceSDK extends CordovaPlugin {
    public static class ApiResult {
        public static String RESULT_SUCCESS = "SUCCESS";
        public static String RESULT_FAILURE = "FAILURE";

        String result;
        String resultMessage;

        public ApiResult(String result, String resultMessage) {
            this.result = result;
            this.resultMessage = resultMessage;
        }
    }

    private static final String LOG_TAG = "CordovaElementFaceSdk";
    private static final String CAMERA = Manifest.permission.CAMERA;
    private static final String ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private static final int ENROLL_REQ_CODE = 12800;
    private static final int AUTH_REQ_CODE = 12801;

    private CallbackContext latestCallback;
    private boolean permissionsAccepted;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        ElementFaceSDK.initSDK(cordova.getActivity().getApplication());
        requestPermission();
    }

    private void requestPermission() {
        if (cordova.hasPermission(CAMERA) &&
                cordova.hasPermission(ACCESS_FINE_LOCATION) &&
                cordova.hasPermission(ACCESS_COARSE_LOCATION)) {
            permissionsAccepted = true;
        } else {
            String[] permissions = {CAMERA, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION};
            cordova.requestPermissions(this, 0, permissions);
        }
    }

    private void holdCallbackForActivityResult() {
        // Send no result, to execute the callbacks later
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        latestCallback.sendPluginResult(pluginResult);
    }

    private void postPluginSuccessResult(String message) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, getJsonResultResponse(RESULT_SUCCESS, message));
        latestCallback.sendPluginResult(pluginResult);
    }

    private void postPluginFailureResult(String message) {
        // Send a positive result to the callbackContext
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, getJsonResultResponse(RESULT_FAILURE, message));
        latestCallback.sendPluginResult(pluginResult);
    }

    // called from element.js
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.i(LOG_TAG, action);
        latestCallback = callbackContext;

        // These function do not require permissions
        if (action.equals("list")) {
            handleListAction();
            return true;
        }

        if (!permissionsAccepted) {
            handlePermissionAction();
            return true;
        }

        if (action.equals("enroll")) {
            String userId = args.getString(0);
            String firstName = args.getString(1);
            String lastName = args.getString(2);
            handleEnrollAction(userId, firstName, lastName);
            return true;
        }

        if (action.equals("auth")) {
            String userId = args.getString(0);
            handleAuthAction(userId);
            return true;
        }

        return false;
    }

    private void handleListAction() {
        // This result is only one that isn't ApiResult. It's a Json List<UserInfo>
        String list = getJsonList();
        Log.i(LOG_TAG, "handleListAction -> " + list);
        postPluginSuccessResult(list);
    }

    private void handlePermissionAction() {
        requestPermission();
        postPluginFailureResult("Must accept permissions first.");
    }

    private void handleEnrollAction(String userId, String firstName, String lastName) {
        Log.i(LOG_TAG, "handleEnrollAction");

        Activity activity = cordova.getActivity();
//        UserInfo userInfo = UserInfo.enrollNewUser(activity, cordova.getContext().getPackageName(),
//                firstName, lastName, null);
        UserInfo userInfo = UserInfo.builder().setId(userId).setName(firstName).setName2(lastName).enroll(activity);

        Log.i(LOG_TAG, "handleEnrollAction -> " + userInfo.userId);
        holdCallbackForActivityResult();
        Intent intent = new Intent(activity, ElementFaceEnrollActivity.class);
        intent.putExtra(ElementFaceEnrollActivity.EXTRA_ELEMENT_USER_ID, userInfo.userId);
        cordova.startActivityForResult(this, intent, ENROLL_REQ_CODE);
    }

    private void handleAuthAction(String userId) {
        Log.i(LOG_TAG, "handleAuthAction");
        holdCallbackForActivityResult();

        Intent intent = new Intent(cordova.getActivity(), ElementFaceAuthActivity.class);
        intent.putExtra(ElementFaceAuthActivity.EXTRA_ELEMENT_USER_ID, userId);
        intent.putExtra(ElementFaceAuthActivity.EXTRA_LIVENESS_DETECTION, "GAZE");
        intent.putExtra(ElementFaceAuthActivity.EXTRA_UI_DELEGATE, "selfieDot");
        cordova.startActivityForResult(this, intent, AUTH_REQ_CODE);
    }

    private String getJsonList() {
        //List<UserInfo> list = ProviderUtil.getUsers(cordova.getContext(), cordova.getContext().getPackageName(), null);
        List<UserInfo> list = ElementUserUtils.getUsers(cordova.getContext());
        Type listType = new TypeToken<List<UserInfo>>() {
        }.getType();
        return new Gson().toJson(list, listType);
    }

    private String getJsonResultResponse(String result, String message) {
        return new Gson().toJson(new ApiResult(result, message));
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Log.d(LOG_TAG, "Permission denied.");
                Toast.makeText(cordova.getContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                return;
            }
        }
        permissionsAccepted = true;
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        latestCallback = callbackContext;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(LOG_TAG, "onActivityResult -> " + requestCode + " ( " + resultCode + ")");

        if (requestCode == ENROLL_REQ_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(LOG_TAG, "onActivityResult -> Enroll SUCCESS");
                postPluginSuccessResult("Enroll Complete");
            } else {
                Log.i(LOG_TAG, "onActivityResult -> Enroll FAILED");
                postPluginFailureResult("Enroll Cancelled");
            }
        } else if (requestCode == AUTH_REQ_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String userId = data.getStringExtra(ElementFaceAuthActivity.EXTRA_ELEMENT_USER_ID);
                //UserInfo userInfo = ProviderUtil.getUser(cordova.getContext(), cordova.getContext().getPackageName(), userId);
                UserInfo userInfo = ElementUserUtils.getUser(cordova.getContext(), userId);
                String results = data.getStringExtra(ElementFaceAuthActivity.EXTRA_RESULTS);

                if (ElementFaceAuthActivity.USER_VERIFIED.equals(results)) {
                    Log.i(LOG_TAG, "onActivityResult -> Auth VERIFIED");
                    postPluginSuccessResult("Verified " + userInfo.name + " " + userInfo.name2);
                } else if (ElementFaceAuthActivity.USER_FAKE.equals(results)) {
                    Log.i(LOG_TAG, "onActivityResult -> Auth FAKE");
                    postPluginFailureResult("Try Again! (fake)");
                } else {
                    Log.i(LOG_TAG, "onActivityResult -> Auth TRY AGAIN");
                    postPluginFailureResult("Try Again.");
                }
            } else {
                Log.i(LOG_TAG, "onActivityResult -> Auth FAILED");
                postPluginFailureResult("Auth Failed!");
            }
        }
    }
}