package edu.duke.compsci290.friender;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by zbd1023 on 4/28/18.
 */

public class Loc {
    private LatLng location;
    private String email;

    Loc(LatLng l, String e){
        location = l;
        email = e;
    }

    public LatLng getLocation() {
        return location;
    }

    public String getEmail() {
        return email;
    }
}
