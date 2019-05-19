package edu.duke.compsci290.friender;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    public String ID;
    public String email;
    public String Image;
    public String Audio;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String email, String Image, String Audio) {
        this.email = email;
        this.Audio = Audio;
        this.Image = Image;
    }

}