package com.example.femi.popularmovies;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by femi on 12/23/16.
 */

public class Movies implements Parcelable{
    private String mposter ;
    private String moverview ;
    private Double mrating ;
    private String mdate ;
    private String mtitle ;

    public Movies(){
        mposter = "";
        moverview = "";
        mrating = 0.0;
        mdate = "";
        mtitle = "";
    }


    public void setPoster(String poster){
        mposter = poster;
    }
    public String getPoster(){
        return mposter;
    }

    public void setOverview(String overview){
        moverview = overview;
    }
    public String getOverview(){
        return moverview;
    }
    public void setRating(Double rating){
        mrating = rating;
    }

    public Double getRating() {
        return mrating;
    }

    public void setDate(String date){
        mdate = date;
    }

    public String getDate() {
        return mdate;
    }

    public void setTitle(String title){
        mtitle = title;
    }

    public String getTitle() {
        return mtitle;
    }


    private Movies(Parcel in){
        mtitle = in.readString();
        moverview = in.readString();
        mposter = in.readString();
        mrating = in.readDouble();
        mdate = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mtitle);
        dest.writeString(moverview);
        dest.writeString(mposter);
        dest.writeDouble(mrating);
        dest.writeString(mdate);
    }

    private void readFromParcel(Parcel in ) {

        mtitle = in.readString();
        mdate = in.readString();
        moverview = in.readString();
        mposter = in.readString();
        mrating = in.readDouble();
    }

    public final Parcelable.Creator<Movies> CREATOR = new Parcelable.Creator<Movies>(){

        @Override
        public Movies createFromParcel(Parcel source) {
            return new Movies(source);
        }

        @Override
        public Movies[] newArray(int size) {
            return new Movies[size];
        }
    };
}

