package com.music.monir;

public class user {
    private String email;
    private String date;
    private String status;

    public user(){

    }
    public user(String email, String date, String status){
        this.email = email;
        this.date = date;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String getEmail() {
        return email;
    }

    public String getDate() {
        return date;
    }
}
