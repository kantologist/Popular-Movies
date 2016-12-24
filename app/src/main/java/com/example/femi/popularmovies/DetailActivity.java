package com.example.femi.popularmovies;

import android.content.Intent;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new DetailFragment())
                    .commit();
        };


    }


    public static class DetailFragment extends Fragment {

        public DetailFragment(){

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            // The detail Activity called via intent.  Inspect the intent for forecast data.
            Intent intent = getActivity().getIntent();
            if (intent != null && intent.hasExtra("title")) {
                String title = intent.getStringExtra("title");
                String overview = intent.getStringExtra("overview");
                String date = intent.getStringExtra("date");
                String poster = intent.getStringExtra("poster");
                Double rating = intent.getExtras().getDouble("rating");

                ((TextView) rootView.findViewById(R.id.title))
                        .setText(title);
                ((TextView) rootView.findViewById(R.id.overview))
                        .setText(overview);

                ((TextView) rootView.findViewById(R.id.date))
                        .setText(date);
                String rate = rating.toString() + "/10";
                ((TextView) rootView.findViewById(R.id.rating))
                        .setText(rate);
                ImageView icon = (ImageView) rootView.findViewById(R.id.poster);
                String base_url = "http://image.tmdb.org/t/p/w342/";
                Picasso.with(getContext()).load(base_url + poster).into(icon);

            }

            return rootView;
        }

    }
}