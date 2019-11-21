package com.lt.lrmd.lhfa;

public class Placemark {

    String name;
    String description;
    double lat;
    double lng;

    public  Placemark()
    {
    }

    public Placemark(String name, String description, double lat, double lng)
    {
        this.name = name;
        this.description = description;
        this.lat = lat;
        this.lng = lng;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }
    public String getName()
    {
        return this.name;
    }
    public String getDescription()
    {
        return this.description;
    }
    public void setLongitude(double lng) { this.lng = lng; }
    public void setLatitude(double lat) { this.lat = lat; }
    public double getLongitude() { return lng; }
    public double getLatitude() { return lat; }
}


