package com.lt.lrmd.lhfa;

import android.location.Location;
import java.util.Observable;

public class ObservedLocation extends Observable {

    public void LocationChanged(Location location)
    {
        setChanged();
        notifyObservers(location);
    }
}
