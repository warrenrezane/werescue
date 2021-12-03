package com.rexor.werescue.Model;

public class users {

    private String firstname;
    private String lastname;
    private String email;
    private String password;
    private String id;
    private String circle_id;
    private String date;
    private String image_url;
    private String phonenumber;
    private double latitude, longitude;

    public  users(String id, String firstname, String lastname, String circle_id, String email, String password, String date, double latitude, double longitude, String image_url, String phonenumber)
    {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.circle_id = circle_id;
        this.email = email;
        this.password = password;
        this.date = date;
        this.latitude = latitude;
        this.longitude = longitude;
        this.image_url = image_url;
        this.phonenumber = phonenumber;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getcircle_id() {
        return circle_id;
    }

    public void setcircle_id(String circle_id) {
        this.circle_id = circle_id;
    }

    public String getid() {
        return id;
    }

    public void setid(String id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }
}
