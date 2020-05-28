package com.element.plugin;

import org.apache.cordova.*;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONException;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.element.camera.ElementFaceAuthActivity;
import com.element.camera.ElementFaceEnrollActivity;
import com.element.camera.ElementFaceSDK;
import com.element.camera.ElementUserUtils;
import com.element.camera.UserInfo;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ElementCordovaFaceSDK extends CordovaPlugin {
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


	// called from element.js
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		Log.e("CordovaElementSdk", action);
		latestCallback = callbackContext;

		// These function do not require permissions
		if (action.equals("list")) {
			handleListAction();
			return true;
		}
		if (!permissionsAccepted) {
			handlePermissionAction();
			return false;
		}

		if (action.equals("create")) {
			String userId = args.getString(0);
			String firstName = args.getString(1);
			String lastName = args.getString(2);
			handleCreateAction(userId, firstName, lastName);
			return true;
		}

		if (action.equals("train")) {
			String userId = args.getString(0);
			handleEnrollAction(userId);
			return true;
		}

		if (action.equals("authentication")) {
			String userId = args.getString(0);
			handleAuthAction(userId);
			return true;
		}

		return false;
	}

	private void handleListAction() {
		String list = getJsonList();
		latestCallback.success(list);
	}

	private void handlePermissionAction() {
		requestPermission();
		latestCallback.error("Must accept permissions first.");
	}

	private void handleCreateAction(String userId, String firstName, String lastName) {
		Activity activity = cordova.getActivity();
		if (TextUtils.isEmpty(userId) || "null".equals(userId.toLowerCase())) {
			userId = UUID.randomUUID().toString();
		}

		UserInfo userInfo = UserInfo.builder().setId(userId).setName(firstName).setName2(lastName).enroll(activity);
		handleEnrollAction(userInfo.userId);
	}

	private void handleEnrollAction(String userId) {
		Intent intent = new Intent(cordova.getActivity(), ElementFaceEnrollActivity.class);
		intent.putExtra(ElementFaceEnrollActivity.EXTRA_ELEMENT_USER_ID, userId);
		cordova.startActivityForResult(this, intent, ENROLL_REQ_CODE);
	}

	private void handleAuthAction(String userId) {
		Intent intent = new Intent(cordova.getActivity(), ElementFaceAuthActivity.class);
		intent.putExtra(ElementFaceAuthActivity.EXTRA_ELEMENT_USER_ID, userId);
		cordova.startActivityForResult(this, intent, AUTH_REQ_CODE);
	}

	private String getJsonList() {
		List<UserInfo> list = ElementUserUtils.getUsers(cordova.getContext());
		Type listType = new TypeToken<List<UserInfo>>() {}.getType();
		return new Gson().toJson(list, listType);
	}

	public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
		for(int r : grantResults) {
			if(r == PackageManager.PERMISSION_DENIED) {
				Toast.makeText(cordova.getContext(), "Permission Denied", Toast.LENGTH_LONG).show();
				return;
			}
		}
		permissionsAccepted = true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.e("CordovaElementSdk", "Result -> " + requestCode + " ( " + resultCode + ")");
		if (requestCode == ENROLL_REQ_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				latestCallback.success("Enroll Complete");
			} else {
				latestCallback.error("Enroll Cancelled");
			}
		} else if (requestCode == AUTH_REQ_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				String userId = data.getStringExtra(ElementFaceAuthActivity.EXTRA_ELEMENT_USER_ID);
				UserInfo userInfo = ElementUserUtils.getUser(cordova.getContext(), userId);
				String results = data.getStringExtra(ElementFaceAuthActivity.EXTRA_RESULTS);

				if (ElementFaceAuthActivity.USER_VERIFIED.equals(results)) {
					latestCallback.success("Verified " + userInfo.name + " " + userInfo.name2);
				} else if (ElementFaceAuthActivity.USER_FAKE.equals(results)) {
					latestCallback.error("Try Again! (fake)");
				} else {
					latestCallback.error("Try Again.");
				}
			} else {
				latestCallback.error("Auth Failed!");
			}
		}
	}
}