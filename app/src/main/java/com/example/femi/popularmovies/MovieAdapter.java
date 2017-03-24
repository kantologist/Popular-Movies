package com.example.femi.popularmovies;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by femi on 12/23/16.
 */

public class MovieAdapter extends ArrayAdapter<Movies> {

    public MovieAdapter(Context context, List<Movies> Movie) {
        super(context, 0, Movie);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Movies movie = getItem(position);


        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item_movie, parent, false);
        }

        ImageView icon = (ImageView) convertView.findViewById(R.id.movie_data);
        String base_url = "http://image.tmdb.org/t/p/w780/";
        Picasso.with(getContext()).load(base_url + movie.getPoster()).into(icon);
        return  convertView;
    }
    }
