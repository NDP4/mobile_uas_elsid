package com.mobile2.uas_elsid.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULLNAME = "fullname";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_CITY = "city";
    private static final String KEY_PROVINCE = "province";
    private static final String KEY_POSTAL_CODE = "postal_code";
    private static final String KEY_AVATAR = "avatar";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;


    // Getter methods
    public String getUserId() {
        String userId = pref.getString(KEY_USER_ID, "");
        // Debug log
        System.out.println("Debug - Getting UserId from Session: " + userId);
        return userId;
    }

    public String getFullname() {
        return pref.getString(KEY_FULLNAME, "");
    }
    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }
    public String getPhone() {
        return pref.getString(KEY_PHONE, "");
    }

    public String getAddress() {
        return pref.getString(KEY_ADDRESS, "");
    }

    public String getCity() {
        return pref.getString(KEY_CITY, "");
    }

    public String getProvince() {
        return pref.getString(KEY_PROVINCE, "");
    }

    public String getPostalCode() {
        return pref.getString(KEY_POSTAL_CODE, "");
    }
    public String getAvatarPath() {
        return pref.getString(KEY_AVATAR, "");
    }
    public void saveAvatarPath(String avatar) {
        if (avatar != null) {
            System.out.println("Saving avatar path: " + avatar); // Debug log
            editor.putString(KEY_AVATAR, avatar);
            editor.commit(); // Use commit() instead of apply() for immediate effect
        }
    }

    public void updateProfile(String fullname, String phone, String address,
                              String city, String province, String postalCode) {
        editor.putString(KEY_FULLNAME, fullname);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_ADDRESS, address);
        editor.putString(KEY_CITY, city);
        editor.putString(KEY_PROVINCE, province);
        editor.putString(KEY_POSTAL_CODE, postalCode);
        editor.commit();
//        editor.apply(); // menggunakan apply() untuk penyimpanan asynchronous
    }

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String userId, String fullname, String email, String phone, String address) {
        // Validate input
        if (userId == null || userId.isEmpty()) {
            System.out.println("Error - Attempting to create session with empty userId");
            return;
        }

        // Clear existing session first
        editor.clear();

        // Save new session data
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_FULLNAME, fullname);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_ADDRESS, address);

        // Use commit() untuk memastikan data tersimpan segera
        boolean success = editor.commit();

        // Verify saved data
        System.out.println("Debug - Session Created - Success: " + success);
        System.out.println("Debug - Session Created - UserId: " + pref.getString(KEY_USER_ID, ""));
        System.out.println("Debug - Session Created - IsLoggedIn: " + pref.getBoolean(KEY_IS_LOGGED_IN, false));
    }

    public boolean isLoggedIn() {
        boolean isLoggedIn = pref.getBoolean(KEY_IS_LOGGED_IN, false);
        // Debug log
        System.out.println("Debug - Checking IsLoggedIn: " + isLoggedIn);
        return isLoggedIn;
    }


    public void logout() {
        editor.clear();
//        editor.commit();
        editor.apply(); // menggunakan apply() untuk penyimpanan asynchronous
    }
}