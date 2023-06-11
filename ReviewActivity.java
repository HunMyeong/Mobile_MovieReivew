package com.example.apitest2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {
    private ReviewDatabaseHelper dbHelper;
    private SQLiteDatabase database;
    String apiKey = "";
    private EditText editTextName;
    private Spinner spinnerMovie;
    private EditText editTextReview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        setTitle("한줄평");

        editTextName = findViewById(R.id.editTextName);
        spinnerMovie = findViewById(R.id.spinnerMovie);
        editTextReview = findViewById(R.id.editTextReview);

        dbHelper = new ReviewDatabaseHelper(this);
        database = dbHelper.getWritableDatabase();

        // AsyncTask를 사용하여 네트워크 작업 수행
        new FetchMovieTitlesTask().execute();

        Button buttonSave = findViewById(R.id.buttonDone);
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveReview();
                updateReviewList(); // 리뷰 저장 후 목록 업데이트
            }
        });

        updateReviewList(); // 액티비티 시작 시 리뷰 목록 업데이트
    }

    private class FetchMovieTitlesTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> movieTitles = new ArrayList<>();

            Date date = new Date();//현재 날짜와 시간을 가진 객체
            date.setTime(date.getTime() - (1000 * 60 * 60 * 24)); //현재 날짜의 1일을 뺀 날짜

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); //SimpleDateFormat 자동으로 편리하게 포맷을 넣을 수 있다.
            String dateStr = sdf.format(date);

            String address = "http://www.kobis.or.kr/kobisopenapi/webservice/rest/boxoffice/searchDailyBoxOfficeList.xml"
                    + "?key=" + apiKey
                    + "&targetDt=" + dateStr
                    + "&itemPerPage=10";

            try {
                URL url = new URL(address);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    movieTitles = parseMovieNames(inputStream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return movieTitles;
        }

        @Override
        protected void onPostExecute(List<String> movieTitles) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(ReviewActivity.this,
                    android.R.layout.simple_spinner_item, movieTitles);
            spinnerMovie.setAdapter(adapter);
        }
    }

    private List<String> parseMovieNames(InputStream inputStream) throws XmlPullParserException {
        List<String> movieNames = new ArrayList<>();

        try {
            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = xmlFactoryObject.newPullParser();
            xmlPullParser.setInput(inputStream, null);

            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xmlPullParser.getName().equals("movieNm")) {
                    eventType = xmlPullParser.next();
                    String movieName = xmlPullParser.getText();
                    movieNames.add(movieName);
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return movieNames;
    }

    private void saveReview() {
        String movie = spinnerMovie.getSelectedItem().toString();
        String name = editTextName.getText().toString();
        String review = editTextReview.getText().toString();

        ContentValues values = new ContentValues();
        values.put("movie", movie);
        values.put("name", name);
        values.put("review", review);

        long newRowId = database.insert("reviews", null, values);
        if (newRowId != -1) {
            Toast.makeText(this, "한 줄 평이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "한 줄 평 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateReviewList() {
        List<String> reviewList = new ArrayList<>();
        Cursor cursor = database.query("reviews", null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            String movie = cursor.getString(cursor.getColumnIndex("movie"));
            String name = cursor.getString(cursor.getColumnIndex("name"));
            String review = cursor.getString(cursor.getColumnIndex("review"));
            reviewList.add(0, movie + " - " + name + ": " + review); // 위로 추가될 수 있도록 인덱스 0에 추가
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reviewList);
        ListView listView = findViewById(R.id.listViewReviews);
        listView.setAdapter(adapter);
    }

    public class ReviewDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "reviewDB";
        private static final int DATABASE_VERSION = 1;

        public ReviewDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTableSQL = "CREATE TABLE reviews (_id INTEGER PRIMARY KEY AUTOINCREMENT, movie TEXT, name TEXT, review TEXT)";
            db.execSQL(createTableSQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 테이블 업그레이드를 위한 로직 작성 (필요시 구현)
        }
    }
}
