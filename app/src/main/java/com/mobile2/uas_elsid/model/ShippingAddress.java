package com.mobile2.uas_elsid.model;

public class ShippingAddress {
    private String province;
    private String city;
    private String address;
    private String postalCode;
    private String provinceId;
    private String cityId;

    public ShippingAddress(String province, String city, String address,
                           String postalCode, String provinceId, String cityId) {
        this.province = province;
        this.city = city;
        this.address = address;
        this.postalCode = postalCode;
        this.provinceId = provinceId;
        this.cityId = cityId;
    }

    // Getters
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getAddress() { return address; }
    public String getPostalCode() { return postalCode; }
    public String getProvinceId() { return provinceId; }
    public String getCityId() { return cityId; }

    // Setters
    public void setProvince(String province) { this.province = province; }
    public void setCity(String city) { this.city = city; }
    public void setAddress(String address) { this.address = address; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setProvinceId(String provinceId) { this.provinceId = provinceId; }
    public void setCityId(String cityId) { this.cityId = cityId; }
}