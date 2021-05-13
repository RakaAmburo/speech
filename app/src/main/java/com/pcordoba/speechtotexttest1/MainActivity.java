package com.pcordoba.speechtotexttest1;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@RequiresApi(api = Build.VERSION_CODES.FROYO)
public class MainActivity extends AppCompatActivity implements
        RecognitionListener {

    private RestPoster restPoster = new RestPoster(this);
    private TextView capturedVoiceCmd;
    //private TextView appliedCmdResponse;
    private Button button;
    private ProgressBar progressBar;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private String lang;
    private String restUrl;
    private String restPort;
    final private String SPANISH = "es-AR";
    final private String ENGLISH = "en";
    private boolean isEndOfSpeech;

    public static ListView statusList;
    public static ArrayAdapter<String> statusListAdapter;


    private SharedPreferences SP;

    final private int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.handleSSLHandshake();
        setContentView(R.layout.activity_main);
        capturedVoiceCmd = (TextView) findViewById(R.id.resultMessage);
        //appliedCmdResponse = (TextView) findViewById(R.id.response);
        button = (Button) findViewById(R.id.mantener);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        List listItem =  new ArrayList();
        statusList=(ListView)findViewById(R.id.statusList);
        statusListAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_selectable_list_item, android.R.id.text1, listItem);
        statusList.setAdapter(statusListAdapter);


        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        setLanguage();

        setTitle("Control por voz 1.2");

        //createSpeech();

        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        askForPermission();
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setIndeterminate(true);
                        //if (speech != null) {
                        createSpeech();
                        //}
                        speech.startListening(recognizerIntent);
                        isEndOfSpeech = false;
                        break;
                    case MotionEvent.ACTION_UP:
                        capturedVoiceCmd.setText("");
                        //appliedCmdResponse.setText("");
                        statusListAdapter.clear();
                        /*try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        progressBar.setIndeterminate(false);
                        progressBar.setVisibility(View.INVISIBLE);
                        speech.stopListening();*/

                        break;
                }
                return false;
            }


            /*@SuppressLint("ClickableViewAccessibility")
            private void askForPermission() {

                String permission = "android.permission.RECORD_AUDIO";
                if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {

                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, 1);

                    } else {

                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, 1);
                    }
                } else {
                }
            }*/
        });
    }

    private void askForPermission() {

        String permission = "android.permission.RECORD_AUDIO";
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, 1);

            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, 1);
            }
        } else {
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getActiveNetworkInfo();

        if (mWifi != null && mWifi.isConnected()) {

            capturedVoiceCmd.setText("wifi connected");
            final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null) {
                String ssid = connectionInfo.getSSID();
                capturedVoiceCmd.setText(ssid);
            } else {
                capturedVoiceCmd.setText("wifi not connected");
            }

        } else {
            capturedVoiceCmd.setText("wifi not connected");
        }

        new Content().execute();


       /* askForPermission();
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        createSpeech();
        speech.startListening(recognizerIntent);
        isEndOfSpeech = false;
        capturedVoiceCmd.setText("");
        statusListAdapter.clear();*/

    }

    private long lastClickTime = 0;

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        restUrl = SP.getString("restUrl", "192.168.43.137");
        restPort = SP.getString("restPort", "8095/Guestbook/sendMessage");

        if (SystemClock.elapsedRealtime() - lastClickTime < 2000) {
            return;
        }

        lastClickTime = SystemClock.elapsedRealtime();

        restPoster.postVoiceResult(matches, restUrl, restPort, this);

        if (matches.size() >= 1) {
            //text = matches.get(0);
            //returnedText.setText(text);
        } else {
            capturedVoiceCmd.setText("Listening Service Error");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_item_new_thingy:
                Intent i = new Intent(this, CustomPreferenceActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void createSpeech() {
        if (speech != null) {
            speech.destroy();
        }
        setLanguage();
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        //recognizerIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (speech == null) {
            createSpeech();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speech != null) {
            speech.destroy();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        progressBar.setIndeterminate(false);
        progressBar.setMax(10);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
        isEndOfSpeech = true;
        progressBar.setIndeterminate(true);
        //worked
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.INVISIBLE);
        speech.stopListening();
    }

    private void setLanguage() {
        if ("1".equalsIgnoreCase(SP.getString("lang", "1"))) {
            lang = SPANISH;
        } else {
            lang = ENGLISH;
        }
    }

    @Override
    public void onError(int errorCode) {

        if (errorCode == SpeechRecognizer.ERROR_NO_MATCH) {
            createSpeech();
            speech.startListening(recognizerIntent);
            return;
        }


        if (speech != null) {
            speech.destroy();
        }
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.INVISIBLE);
        String errorMessage = getErrorText(errorCode);
        capturedVoiceCmd.setText(errorMessage);
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
    }

    @Override
    public void onPartialResults(Bundle arg0) {

    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        progressBar.setProgress((int) rmsdB);
    }

    /**
     * Enables https connections
     */
    @SuppressLint("TrulyRandom")
    public static void handleSSLHandshake() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception ignored) {
        }
    }

    private class Content extends AsyncTask<Void, Void, Void> {

        private String text = "empty";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                Document document = Jsoup.connect("https://slevin08kelevra.github.io/nodeTasks/").get();
                text = document.selectFirst("section").selectFirst("input").attr("value");
            } catch (Exception e) {
                text = "error";
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            capturedVoiceCmd.setText(text);
        }
    }
}