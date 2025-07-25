package com.mobile2.uas_elsid.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IS_GUEST = "isGuest";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULLNAME = "fullname";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_CITY = "city";
    private static final String KEY_PROVINCE = "province";
    private static final String KEY_POSTAL_CODE = "postal_code";
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_PROVINCE_ID = "province_id";
    private static final String KEY_CITY_ID = "city_id";


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
    public void setGuestMode(boolean isGuest) {
        editor.putBoolean(KEY_IS_GUEST, isGuest);
        editor.commit();
    }

    public boolean isGuestMode() {
        return pref.getBoolean(KEY_IS_GUEST, false);
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

    public void createLoginSession(String userId, String fullname, String email, String phone, String address, String avatar) {
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
        editor.putString(KEY_AVATAR, avatar);

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
        // Store temporary shipping data
        String tempProvinceId = getProvinceId();
        String tempCityId = getCityId();
        String tempProvince = getProvince();
        String tempCity = getCity();
        String tempPostalCode = getPostalCode();
        String tempAddress = getAddress();

        // Clear all session data
        editor.clear();
        
        // Restore shipping data
        editor.putString(KEY_PROVINCE_ID, tempProvinceId);
        editor.putString(KEY_CITY_ID, tempCityId);
        editor.putString(KEY_PROVINCE, tempProvince);
        editor.putString(KEY_CITY, tempCity);
        editor.putString(KEY_POSTAL_CODE, tempPostalCode);
        editor.putString(KEY_ADDRESS, tempAddress);
        
        // Set guest mode to false and commit changes
        editor.putBoolean(KEY_IS_GUEST, false);
        editor.commit();
    }

    public void updateProfile(String fullname, String phone, String address,
                              String city, String province, String postalCode,
                              String provinceId, String cityId) {
        editor.putString(KEY_FULLNAME, fullname);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_ADDRESS, address);
        editor.putString(KEY_CITY, city);
        editor.putString(KEY_PROVINCE, province);
        editor.putString(KEY_POSTAL_CODE, postalCode);
        editor.putString(KEY_PROVINCE_ID, provinceId);
        editor.putString(KEY_CITY_ID, cityId);
        editor.commit();
    }

    public String getProvinceId() {
        return pref.getString(KEY_PROVINCE_ID, "");
    }

    public String getCityId() {
        return pref.getString(KEY_CITY_ID, "");
    }

    public void setProvince(String province) {
        editor.putString(KEY_PROVINCE, province);
        editor.commit();
    }

    public void setCity(String city) {
        editor.putString(KEY_CITY, city);
        editor.commit();
    }

    public void setCityId(String cityId) {
        editor.putString(KEY_CITY_ID, cityId);
        editor.commit();
    }

    public void setAddress(String address) {
        editor.putString(KEY_ADDRESS, address);
        editor.commit();
    }

    public void setPostalCode(String postalCode) {
        editor.putString(KEY_POSTAL_CODE, postalCode);
        editor.commit();
    }
}
