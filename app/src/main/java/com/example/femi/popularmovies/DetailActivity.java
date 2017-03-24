package com.example.femi.popularmovies;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.femi.popularmovies.data.MovieContract;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.Vector;

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

        private String title;
        private String overview;
        private String date;
        private String poster;
        private Double rating;
        private int id;
        View rootView;
        LinearLayout reviewLayout;
        LinearLayout trailerLayout;


        public DetailFragment(){

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            // The detail Activity called via intent.  Inspect the intent for forecast data.
            Intent intent = getActivity().getIntent();
            if (intent != null && intent.hasExtra("title")) {
                title = intent.getStringExtra("title");
                overview = intent.getStringExtra("overview");
                date = intent.getStringExtra("date");
                poster = intent.getStringExtra("poster");
                rating = intent.getExtras().getDouble("rating");
                id = intent.getExtras().getInt("id");

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

                FetchTrailer fetchTrailer = new FetchTrailer();
                fetchTrailer.execute(id);

            }

            Button favouriteButton = (Button) rootView.findViewById(R.id.favourite);
            favouriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ContentValues movieValue = new ContentValues();

                    movieValue.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, id);
                    movieValue.put(MovieContract.MovieEntry.COLUMN_TITLE, title);
                    movieValue.put(MovieContract.MovieEntry.COLUMN_DATE, date);
                    movieValue.put(MovieContract.MovieEntry.COLUMN_POSTER, poster);
                    movieValue.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, overview);
                    movieValue.put(MovieContract.MovieEntry.COLUMN_RATING, rating);

                    try {

                        getContext().getContentResolver().insert(MovieContract.MovieEntry.CONTENT_URI, movieValue);

                        Toast.makeText(getActivity(), "added to favourite", Toast.LENGTH_SHORT).show();
                    } catch (Exception e){
                        Toast.makeText(getActivity(), "already favourite", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            Button reviewButton = (Button) rootView.findViewById(R.id.review);
            reviewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FetchReview fetchReview = new FetchReview();
                    fetchReview.execute(id);
                }
            });

            return rootView;
        }

        public class FetchReview extends AsyncTask<Integer, Void, Movies[]> {

            private final String LOG_TAG = DetailFragment.class.getSimpleName();
            private static final String API_KEY = BuildConfig.API_KEY;

            private Movies[] getMovieDataFromJson(String movieJsonStr)
                    throws JSONException {

                final String M_LIST = "results";
                final String M_CONTENT = "content";
                final String M_URL = "url";

                JSONObject movieJson = new JSONObject(movieJsonStr);
                JSONArray movieArray = movieJson.getJSONArray(M_LIST);


                Movies[] resultMvs = new  Movies[movieArray.length()];

                for (int i = 0; i < movieArray.length(); i++){

                    String content;
                    String url;

                    JSONObject movie = movieArray.getJSONObject(i);
                    content = movie.getString(M_CONTENT);
                    url = movie.getString(M_URL);

                    resultMvs[i]= new Movies();
                    resultMvs[i].setReviewContent(content);
                    resultMvs[i].setReviewUrl(url);
                }

                return  resultMvs;



            }

            @Override
            protected Movies[] doInBackground(Integer... params) {
                if (params.length == 0){
                    return null;
                }

                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                String movieJsonStr = null;

                int id = params[0];

                try {
                    final String MOVIE_BASE_URL =
                            "https://api.themoviedb.org/3/movie/" + id + "/reviews?";
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
                    reviewLayout = (LinearLayout) rootView.findViewById(R.id.main_review);
                    for (final Movies review:result)
                    {
                        TextView reviewContent = new TextView(getActivity());
                        reviewContent.setText(review.getReviewContent());
                        reviewContent.setGravity(Gravity.CENTER);
                        reviewContent.setTextSize(15);
                        reviewContent.setTextColor(getResources().getColor(R.color.textColor));
                        reviewContent.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(review.getReviewUrl()));
                                startActivity(i);
                            }
                        });
                        TextView line = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.line, null);

                        reviewLayout.addView(reviewContent);
                        reviewLayout.addView(line);
                    }

                }
            }
        }

        public class FetchTrailer extends AsyncTask<Integer, Void, Movies[]> {

            private final String LOG_TAG = DetailFragment.class.getSimpleName();
            private static final String API_KEY = BuildConfig.API_KEY;

            private Movies[] getMovieDataFromJson(String movieJsonStr)
                    throws JSONException {

                final String M_LIST = "results";
                final String M_KEY = "key";

                JSONObject movieJson = new JSONObject(movieJsonStr);
                JSONArray movieArray = movieJson.getJSONArray(M_LIST);


                Movies[] resultMvs = new  Movies[movieArray.length()];
                for (int i = 0; i < movieArray.length(); i++){

                    String key;

                    JSONObject movie = movieArray.getJSONObject(i);
                    key = movie.getString(M_KEY);

                    resultMvs[i]= new Movies();
                    resultMvs[i].setYoutubeKey(key);
                }

                return  resultMvs;
            }

            @Override
            protected Movies[] doInBackground(Integer... params) {
                if (params.length == 0){
                    return null;
                }

                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                String movieJsonStr = null;

                int id = params[0];

                try {
                    final String MOVIE_BASE_URL =
                            "https://api.themoviedb.org/3/movie/" + id + "/videos?";
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
            protected void onPostExecute(final Movies[] result){

                if (result != null){
                    trailerLayout = (LinearLayout) rootView.findViewById(R.id.main_review);
                    for (int i=0;i<result.length; i++ )
                    {
                        TextView trailerText = new TextView(getActivity());
                        ImageView playImage = new ImageView(getActivity());
                        LinearLayout trailLay = new LinearLayout(getActivity());

                        trailLay.setLayoutParams(new ViewGroup
                                .LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                        trailLay.setOrientation(LinearLayout.HORIZONTAL);

                        playImage.setImageResource(R.drawable.play);
                        playImage.setMaxHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                        playImage.setMaxWidth(70);

                        String trail = "Trailer " + String.valueOf(i+1);
                        trailerText.setText(trail);
                        trailerText.setGravity(Gravity.CENTER);
                        trailerText.setTextSize(25);
                        trailerText.setTextColor(getResources().getColor(R.color.textColor));

                        trailLay.addView(playImage);
                        trailLay.addView(trailerText);

                        final Movies videoKey = result[i];
                        trailLay.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent appIntent = new Intent(Intent.ACTION_VIEW);
                                appIntent.setData(Uri.parse("vnd.youtube:" + videoKey.getYoutubeKey()));
                                Intent webIntent = new Intent(Intent.ACTION_VIEW);
                                webIntent.setData(Uri.parse("http://www.youtube.com/watch?v="  + videoKey.getYoutubeKey()));
                                try{
                                startActivity(appIntent);
                                } catch (ActivityNotFoundException ex){
                                    startActivity(webIntent);
                                }
                            }
                        });
                        TextView line = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.line, null);

                        trailerLayout.addView(trailLay);
                        trailerLayout.addView(line);
                    }

                }
            }
        }

    }
}