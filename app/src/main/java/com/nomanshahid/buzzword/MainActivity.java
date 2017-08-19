package com.nomanshahid.buzzword;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {

    private Context context = this;
    public static final String TAG = MainActivity.class.getSimpleName();
    private EditText userNumber;
    private ProgressBar loadingData;
    private ImageView appTitle;
    private TextView searchingData;
    private ConstraintLayout constraintLayout;
    private ArrayList<String> dictionaryList = new ArrayList<>();
    private ArrayList<String> potentialWords = new ArrayList<>();
    private ArrayList<String> realWordsList = new ArrayList<>();
    private ArrayList<Word> realWords = new ArrayList<>();
    private HashMap<Character, char[]> mKeypad = new HashMap<Character, char[]>() {
        {
            put('2', new char[] {'a', 'b', 'c'});
            put('3', new char[] {'d', 'e', 'f'});
            put('4', new char[] {'g', 'h', 'i'});
            put('5', new char[] {'j', 'k', 'l'});
            put('6', new char[] {'m', 'n', 'o'});
            put('7', new char[] {'p', 'q', 'r', 's'});
            put('8', new char[] {'t', 'u', 'v'});
            put('9', new char[] {'w', 'x', 'y', 'z'});
        }
    };

    public String prettyPhoneNumber (String stringPhoneNumber) {
        String formattedNumber = "(";
        for (int i = 0; i < stringPhoneNumber.length(); ++i) {
            if (i == 3) formattedNumber += ") ";
            if (i == 6) formattedNumber += "-";
            formattedNumber += stringPhoneNumber.charAt(i);
        }
        return formattedNumber;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userNumber = (EditText) findViewById(R.id.txtNumber);
        loadingData = (ProgressBar) findViewById(R.id.loadingDataBar);
        appTitle = (ImageView) findViewById(R.id.appTitle);
        searchingData = (TextView) findViewById(R.id.searchingData);
        constraintLayout = (ConstraintLayout) findViewById(R.id.constraintLayout);
        loadingData.setVisibility(View.INVISIBLE);
        searchingData.setVisibility(View.INVISIBLE);
        // populate dictionary list when app starts
        InputStream is = getApplicationContext().getResources().openRawResource(R.raw.words);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String str;
        try {
            while ((str = reader.readLine()) != null) {
                dictionaryList.add(str);
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception Caught: ", e);
        }
        userNumber.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String stringPhoneNumber =  userNumber.getText().toString();
                    if (stringPhoneNumber.length() < 10) {
                        Toast.makeText(getApplicationContext(),
                                "Please enter a 10 digit phone number", Toast.LENGTH_LONG).show();
                    }
                    else {
                        if (!isNetworkAvailable()) {
                            Toast.makeText(getApplicationContext(), R.string.network_unavailable_message, Toast.LENGTH_LONG).show();
                        } else {
                            Utils.hideKeyboard(MainActivity.this);
                            RetrieveWordDataTask retrieveWordDataTask = new RetrieveWordDataTask
                                    (context, MainActivity.this, stringPhoneNumber);
                            retrieveWordDataTask.execute();
                        }
                    }
                    return true;
                }
                return false;
            }
        });


       /* button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String stringPhoneNumber =  userNumber.getText().toString();
                if (stringPhoneNumber.length() < 10) {
                    Toast.makeText(getApplicationContext(),
                            "Please enter a 10 digit phone number", Toast.LENGTH_LONG).show();
                }
                else {
                    RetrieveWordDataTask retrieveWordDataTask = new RetrieveWordDataTask(stringPhoneNumber);
                    retrieveWordDataTask.execute();
                    button.setEnabled(false);
                    Log.v(TAG, "Setting display for tempText ... ");
                    //tempText.setText(prettyPhoneNumber(stringPhoneNumber));
                }
            }
        });*/
    }

    public void populatePotentialWords (String stringPhoneNumber) { // Find all potential combinations
        char num1 = stringPhoneNumber.charAt(stringPhoneNumber.length() - 4);
        char num2 = stringPhoneNumber.charAt(stringPhoneNumber.length() - 3);
        char num3 = stringPhoneNumber.charAt(stringPhoneNumber.length() - 2);
        char num4 = stringPhoneNumber.charAt(stringPhoneNumber.length() - 1);
        for (char letter1 : mKeypad.get(num1)) {
            for (char letter2 : mKeypad.get(num2)) {
                for (char letter3 : mKeypad.get(num3)) {
                    for (char letter4 : mKeypad.get(num4)) {
                        potentialWords.add("" + letter1 + letter2 + letter3 + letter4);
                    }
                }
            }
        }
    }

    public void filterWords() { // Filters potential words to actual words in local dictionary
        for (String dictWord : dictionaryList) {
            for (String word : potentialWords) {
                if (word.equals(dictWord)) {
                    realWordsList.add(word);
                }
            }
        }
    }

    public void populateRealWords () { // populates Word array
        for (String word : realWordsList) {
            getWordData(word);
        }
    }

    private void getWordData(String wordToSearch) {
        String dictionaryURL = "http://api.pearson.com/v2/dictionaries/ldoce5/entries?headword=" + wordToSearch;
        if (isNetworkAvailable()) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(dictionaryURL)
                    .build();

            Call call = client.newCall(request);
            try {
                Response response = call.execute();
                if(response.isSuccessful()) {
                    String jsonData = response.body().string();
                    if (hasResult(jsonData)) {
                        realWords.add(getCurrentDetails(jsonData));
                        Log.v(TAG, "Added to real words array ... size = " + realWords.size());
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Exception caught: ", e);
            }
            /*call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String jsonData = response.body().string();
                        if (response.isSuccessful()) {
                            if (hasResult(jsonData)) {
                                realWords.add(getCurrentDetails(jsonData));
                                Log.v(TAG, "Added to real words array ... size = " + realWords.size());
                            }
                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });*/
        }
        else {
            Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasResult(String jsonData) throws JSONException { // Double check, to see if Pearson API has word data
        JSONObject wordObj = new JSONObject(jsonData);
        int resultsLength = wordObj.getJSONArray("results").length();
        return resultsLength != 0;
    }

    private Word getCurrentDetails(String jsonData) throws JSONException { // Return Word object containing details
        JSONObject wordObj = new JSONObject(jsonData);
        JSONArray results = wordObj.getJSONArray("results");
        JSONObject firstResult = results.getJSONObject(0);
        String word = firstResult.getString("headword");
        String definition = firstResult.getJSONArray("senses").getJSONObject(0)
                .getJSONArray("definition").getString(0);
        Log.v(TAG, "From JSON: " + word + "->" + definition);
        Word newWord = new Word();
        newWord.setWord(word.substring(0,1).toUpperCase() + word.substring(1));
        newWord.setDefinition(definition.substring(0, 1).toUpperCase() + definition.substring(1));

        return newWord;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        Boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return isAvailable;
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }

    public static class Utils{
        public static void hideKeyboard(@NonNull Activity activity) {
            // Check if no view has focus:
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    class RetrieveWordDataTask extends AsyncTask<Void, Void, Void> {

        private String phoneNumber;

        public RetrieveWordDataTask(Context context, MainActivity mainActivity, String stringPhoneNumber) {
            phoneNumber = stringPhoneNumber;
        }

        @Override
        protected Void doInBackground(Void... params) {
            populatePotentialWords(phoneNumber);
            filterWords();
            populateRealWords();
            return null;
        }

        @Override
        protected void onPreExecute() {
            userNumber.setVisibility(View.INVISIBLE);
            loadingData.setVisibility(View.VISIBLE);
            searchingData.setText("Analyzing " + prettyPhoneNumber(phoneNumber) + " . . .");
            searchingData.setVisibility(View.VISIBLE);
            //constraintLayout.setBackgroundColor(Color.parseColor("#4CAF50"));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast.makeText(getApplicationContext(), "DONE!!!", Toast.LENGTH_LONG).show();
            context.startActivity(new Intent(context, SliderActivity.class));
            //tempText.setText(potentialWords.size() + " " + realWords.size());
        }
    }

}
