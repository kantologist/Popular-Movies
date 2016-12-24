package com.example.femi.popularmovies;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Movie;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;


/**
 * A simple {@link Fragment} subclass.
 */
public class MovieFragment extends Fragment {

    private MovieAdapter mMovieAdapter;
    private  GridView gridView;
    private ArrayList<Movies> moviesList;




    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        if (savedInstanceState == null || !savedInstanceState.containsKey("movies")){
            moviesList = new ArrayList<Movies>(Arrays.asList(new Movies()));

        } else {
            moviesList = savedInstanceState.getParcelableArrayList("movies");

        }

    }



    public MovieFragment() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(Bundle outState){

        outState.putParcelableArrayList("movies", moviesList);
        super.onSaveInstanceState(outState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment




        mMovieAdapter = new MovieAdapter(
                getActivity(), new ArrayList<Movies>(moviesList));

        View rootView = inflater.inflate(R.layout.fragment_movie, container, false);

        gridView = (GridView) rootView.findViewById(R.id.movie_view);

        gridView.setAdapter(mMovieAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Movies movie = mMovieAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra("title", movie.getTitle())
                        .putExtra("poster", movie.getPoster())
                        .putExtra("date", movie.getDate())
                        .putExtra("overview", movie.getOverview())
                        .putExtra("rating", movie.getRating());
                startActivity(intent);

            }
        });

        return rootView;

    }


    private void updateMovies(){
        FetchMovies fetchMovies = new FetchMovies();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sort_by = prefs.getString(getString(R.string.pref_sorting_key),
                getString(R.string.pref_sorting_popular));
        fetchMovies.execute(sort_by);

    }

    @Override
    public void onStart(){
        super.onStart();
        updateMovies();
    }





    public class FetchMovies extends AsyncTask<String, Void, Movies[]>{

        private final String LOG_TAG = MovieFragment.class.getSimpleName();
        private static final String API_KEY = BuildConfig.API_KEY;
        private final int numOfMovies=20;




        private Movies[] getMovieDataFromJson(String movieJsonStr)
         throws JSONException{

            final String M_LIST = "results";
            final String M_POSTER = "poster_path";
            final String M_OVERVIEW = "overview";
            final String M_RATING = "vote_average";
            final String M_DATE = "release_date";
            final String M_TITLE = "title";


            JSONObject movieJson = new JSONObject(movieJsonStr);
            JSONArray movieArray = movieJson.getJSONArray(M_LIST);


            Movies[] resultMvs = new  Movies[numOfMovies];
            for (int i = 0; i < movieArray.length(); i++){


                String poster;
                String overview;
                Double rating;
                String date;
                String title;


                JSONObject movie = movieArray.getJSONObject(i);
                poster = movie.getString(M_POSTER);
                overview = movie.getString(M_OVERVIEW);
                rating = movie.getDouble(M_RATING);
                date = movie.getString(M_DATE);
                title = movie.getString(M_TITLE);

                resultMvs[i]= new Movies();
                resultMvs[i].setPoster(poster);
                resultMvs[i].setOverview(overview);
                resultMvs[i].setRating(rating);
                resultMvs[i].setDate(date);
                resultMvs[i].setTitle(title);

            }

            return  resultMvs;



        }

        @Override
        protected Movies[] doInBackground(String... params) {
            if (params.length == 0){
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String movieJsonStr = null;

            String sort_by = params[0];

            try {
                final String MOVIE_BASE_URL =
                        "https://api.themoviedb.org/3/movie/" + sort_by + "?";
                final String API_PARAM = "api_key";

                Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                        .appendQueryParameter(API_PARAM, API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "MOVIE URI " + builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null){
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0 ){
                    return null;
                }

                movieJsonStr = buffer.toString();


            } catch (IOException e) {

                Log.e(LOG_TAG, "ERROR ",  e);

                return null;

            } finally {


                if (urlConnection != null ){
                    urlConnection.disconnect();
                }

                if (reader != null){
                    try{
                        reader.close();
                    } catch (final IOException e){
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getMovieDataFromJson(movieJsonStr);
            } catch (JSONException e){
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }



            return null;

        }


        @Override
        protected void onPostExecute(Movies[] result){

            if (result != null){
                moviesList = new ArrayList<Movies>(Arrays.asList(result));
                mMovieAdapter = new MovieAdapter(getActivity(), moviesList);
                gridView.setAdapter(mMovieAdapter);
            }
        }
    }
}
