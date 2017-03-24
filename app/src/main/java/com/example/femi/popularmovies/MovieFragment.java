package com.example.femi.popularmovies;


import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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

import com.example.femi.popularmovies.data.MovieContract;

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
    private GridView gridView;
    private ArrayList<Movies> moviesList;
    private final String LOG_TAG = MovieFragment.class.getSimpleName();
    String sort_by;

    private static final String[] MOVIE_PROJECTION = new String[]{
            MovieContract.MovieEntry.COLUMN_MOVIE_ID,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_OVERVIEW,
            MovieContract.MovieEntry.COLUMN_DATE,
            MovieContract.MovieEntry.COLUMN_POSTER,
            MovieContract.MovieEntry.COLUMN_RATING
    };

    private static final int INDEX_MOVIE_ID = 0;
    private static final int INDEX_MOVIE_TITLE = 1;
    private static final int INDEX_MOVIE_OVERVIEW = 2;
    private static final int INDEX_MOVIE_DATE = 3;
    private static final int INDEX_MOVIE_POSTER = 4;
    private static final int INDEX_MOVIE_RATING = 5;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (savedInstanceState == null || !savedInstanceState.containsKey("movies")) {
            moviesList = new ArrayList<Movies>(Arrays.asList(new Movies()));

        } else {
            moviesList = savedInstanceState.getParcelableArrayList("movies");

        }

    }


    public MovieFragment() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putParcelableArrayList("movies", moviesList);
        outState.putString("preference", sort_by);
        super.onSaveInstanceState(outState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        if (savedInstanceState == null || !savedInstanceState.containsKey("movies")
                || !savedInstanceState.containsKey("preference")) {
            moviesList = new ArrayList<Movies>(Arrays.asList(new Movies()));
            Log.v(LOG_TAG, "fetched movies " + moviesList.get(0).getPoster());


        } else {
            moviesList = savedInstanceState.getParcelableArrayList("movies");
            Log.v(LOG_TAG, "got movies " + moviesList.get(0).getPoster());
        }

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
                        .putExtra("rating", movie.getRating())
                        .putExtra("id", movie.getId());
                startActivity(intent);

            }
        });

        return rootView;

    }


    private void updateMovies() {
        FetchMovies fetchMovies = new FetchMovies();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sort_by = prefs.getString(getString(R.string.pref_sorting_key),
                getString(R.string.pref_sorting_popular));


        if (sort_by.equals(getString(R.string.pref_sorting_favourite))) {
            Cursor cursor = getContext().getContentResolver().query(
                    MovieContract.MovieEntry.CONTENT_URI,
                    MOVIE_PROJECTION,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.getCount() > 0) {

                Movies[] resultMvs = new Movies[cursor.getCount()];
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    int movie_id = cursor.getInt(INDEX_MOVIE_ID);
                    String title = cursor.getString(INDEX_MOVIE_TITLE);
                    String overview = cursor.getString(INDEX_MOVIE_OVERVIEW);
                    String date = cursor.getString(INDEX_MOVIE_DATE);
                    String poster = cursor.getString(INDEX_MOVIE_POSTER);
                    Double rating = cursor.getDouble(INDEX_MOVIE_RATING);

                    resultMvs[i] = new Movies();
                    resultMvs[i].setPoster(poster);
                    resultMvs[i].setOverview(overview);
                    resultMvs[i].setRating(rating);
                    resultMvs[i].setDate(date);
                    resultMvs[i].setTitle(title);
                    resultMvs[i].setId(movie_id);
                    cursor.moveToNext();
                }

                moviesList = new ArrayList<Movies>(Arrays.asList(resultMvs));
                mMovieAdapter = new MovieAdapter(getActivity(), moviesList);
                gridView.setAdapter(mMovieAdapter);

                cursor.close();
            } else {

            }
        } else {
            fetchMovies.execute(sort_by);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String current_sort_by = prefs.getString(getString(R.string.pref_sorting_key),
                getString(R.string.pref_sorting_popular));
        if (moviesList.size() == 1 && !current_sort_by.equals(sort_by)) {
            updateMovies();
        }
    }


    public class FetchMovies extends AsyncTask<String, Void, Movies[]> {

        private final String LOG_TAG = MovieFragment.class.getSimpleName();
        private static final String API_KEY = BuildConfig.API_KEY;
        private final int numOfMovies = 20;


        private Movies[] getMovieDataFromJson(String movieJsonStr)
                throws JSONException {

            final String M_LIST = "results";
            final String M_POSTER = "poster_path";
            final String M_OVERVIEW = "overview";
            final String M_RATING = "vote_average";
            final String M_DATE = "release_date";
            final String M_TITLE = "title";
            final String M_ID = "id";


            JSONObject movieJson = new JSONObject(movieJsonStr);
            JSONArray movieArray = movieJson.getJSONArray(M_LIST);


            Movies[] resultMvs = new Movies[numOfMovies];

            for (int i = 0; i < movieArray.length(); i++) {


                String poster;
                String overview;
                Double rating;
                String date;
                String title;
                int id;


                JSONObject movie = movieArray.getJSONObject(i);
                poster = movie.getString(M_POSTER);
                overview = movie.getString(M_OVERVIEW);
                rating = movie.getDouble(M_RATING);
                date = movie.getString(M_DATE);
                title = movie.getString(M_TITLE);
                id = movie.getInt(M_ID);

                resultMvs[i] = new Movies();
                resultMvs[i].setPoster(poster);
                resultMvs[i].setOverview(overview);
                resultMvs[i].setRating(rating);
                resultMvs[i].setDate(date);
                resultMvs[i].setTitle(title);
                resultMvs[i].setId(id);

            }

            return resultMvs;


        }

        @Override
        protected Movies[] doInBackground(String... params) {
            if (params.length == 0) {
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
                if (inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }

                movieJsonStr = buffer.toString();


            } catch (IOException e) {

                Log.e(LOG_TAG, "ERROR ", e);

                return null;

            } finally {


                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getMovieDataFromJson(movieJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }


            return null;

        }


        @Override
        protected void onPostExecute(Movies[] result) {

            if (result != null) {
                moviesList = new ArrayList<Movies>(Arrays.asList(result));
                mMovieAdapter = new MovieAdapter(getActivity(), moviesList);
                gridView.setAdapter(mMovieAdapter);
            }
        }
    }
}
