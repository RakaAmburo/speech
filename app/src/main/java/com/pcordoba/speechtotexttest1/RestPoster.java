package com.pcordoba.speechtotexttest1;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class RestPoster {

    Context context;

    public RestPoster(Context context) {
        this.context = context;
    }

    private String responseText;

    public void postVoiceResult(ArrayList<String> matches, String restUrl, String restPort, Context context) {
        JSONObject matchesJson = jsonParser(matches);

        RequestQueue queue = Volley.newRequestQueue(context, new HurlStack(null, newSslSocketFactory()));
        String url = "https://" + restUrl + ":" + restPort;


        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, matchesJson,
                new Response.Listener<JSONObject>() {
            
                    @Override
                    public void onResponse(JSONObject response) {
                        updateReturnedText(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                updateReturnedText(error.toString());
            }

        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("authorization", RestPoster.generateToken());
                return headers;
            }
        };

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                40000,
                2,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(jsonRequest);

    }

    private JSONObject jsonParser(ArrayList<String> matches) {
        JSONObject jObject = new JSONObject();
        JSONArray matchesArray = new JSONArray();
        String message = new String();

        /*if (!matches.isEmpty()){
            message = matches.get(0).toString();
        }

        matches.remove(0);*/

        for (String result : matches) {
            matchesArray.put(result.toString());
        }
        try {
            //jObject.put("message", message);
            jObject.put("possibleMessages", matchesArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jObject;
    }

    private static char[] KEYSTORE_PASSWORD = "1234qwer".toCharArray();
    private SSLSocketFactory newSslSocketFactory() {
        try {
            // Get an instance of the Bouncy Castle KeyStore format
            KeyStore trusted = KeyStore.getInstance("BKS");
            // Get the raw resource, which contains the keystore with
            // your trusted certificates (root and any intermediate certs)
            InputStream in = context.getApplicationContext().getResources().openRawResource(R.raw.android);
            try {
                // Initialize the keystore with the provided trusted certificates
                // Provide the password of the keystore
                trusted.load(in, KEYSTORE_PASSWORD);
            } finally {
                in.close();
            }

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

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(trusted);

            String kmAlg = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmAlg);
            kmf.init(trusted, KEYSTORE_PASSWORD);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());

            SSLSocketFactory sf = context.getSocketFactory();
            return sf;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void updateReturnedText(String response) {

        TextView capturedVoiceCmd = (TextView) ((Activity) context).findViewById(R.id.resultMessage);
        capturedVoiceCmd.setText(response);
    }

    private void updateReturnedText(JSONObject response) {

        JSONArray stringResponse = null;
        String executedCmd;
        try {
            stringResponse = response.getJSONArray("status");
            executedCmd = response.get("appliedCmd").toString();
        } catch (JSONException e) {
            //stringResponse = e.toString();
            executedCmd = "Cant extract data from json response";
        }

        ArrayList<String> list = new ArrayList<String>();
        if (stringResponse != null) {
            int len = stringResponse.length();
            for (int i = 0; i < len; i++) {
                try {
                    list.add(stringResponse.get(i).toString());
                } catch (JSONException e) {
                    executedCmd = "Cant extract data from status";
                }
            }
        }

        MainActivity.statusListAdapter.addAll(list);
        TextView capturedVoiceCmd = (TextView) ((Activity) context).findViewById(R.id.resultMessage);
        capturedVoiceCmd.setText(executedCmd);
    }

    private static final int[][] strategy = new int[][]{
            {9, 9, 2, 4, 2, 3}, {1, 6, 3, 9, 5, 7}, {8, 7, 8, 3, 5, 9}, {9, 9, 9, 9, 9, 6}, {4, 6, 1, 1, 2, 5},
            {2, 5, 7, 5, 3, 9}, {5, 5, 5, 8, 9, 6}, {6, 7, 2, 9, 8, 9}, {9, 2, 7, 6, 4, 3}, {9, 3, 9, 2, 3, 4},
            {4, 1, 2, 3, 3, 4}, {4, 4, 7, 5, 9, 8}, {1, 6, 6, 6, 8, 2}, {5, 9, 5, 7, 9, 4}, {1, 5, 9, 8, 5, 8},
            {8, 5, 4, 2, 8, 2}, {9, 7, 7, 1, 9, 8}, {4, 7, 7, 4, 6, 5}, {4, 7, 8, 8, 6, 1}, {6, 3, 1, 4, 9, 6},
            {9, 9, 1, 8, 9, 6}, {2, 5, 2, 4, 3, 6}, {8, 8, 2, 1, 2, 7}, {6, 8, 1, 5, 4, 8}, {2, 7, 3, 4, 1, 6},
            {4, 3, 4, 3, 6, 4}, {3, 9, 3, 9, 3, 5}, {7, 5, 2, 6, 4, 4}, {9, 5, 7, 5, 2, 8}, {9, 3, 9, 8, 6, 3},
            {6, 4, 5, 7, 6, 8}, {4, 2, 9, 4, 8, 5}, {4, 8, 8, 3, 6, 9}, {4, 7, 3, 1, 9, 8}, {5, 3, 8, 1, 9, 7},
            {7, 2, 4, 5, 8, 8}, {3, 3, 7, 8, 4, 9}, {5, 5, 1, 4, 1, 4}, {1, 7, 8, 1, 7, 8}, {6, 1, 2, 6, 7, 6},
            {1, 3, 6, 3, 9, 7}, {3, 6, 2, 6, 4, 2}, {4, 6, 2, 8, 5, 1}, {9, 7, 2, 2, 8, 6}, {7, 1, 7, 9, 1, 8},
            {6, 3, 1, 5, 8, 8}, {9, 3, 7, 4, 1, 9}, {1, 1, 8, 2, 3, 7}, {1, 7, 2, 7, 5, 8}, {6, 1, 4, 3, 8, 5},
            {6, 3, 9, 4, 6, 5}, {7, 5, 7, 8, 9, 2}, {1, 7, 9, 9, 9, 8}, {2, 1, 1, 2, 4, 2}, {6, 7, 6, 1, 9, 2},
            {8, 3, 8, 4, 6, 3}, {6, 7, 4, 2, 5, 7}, {9, 6, 9, 6, 8, 3}, {6, 2, 9, 5, 5, 2}, {9, 1, 2, 6, 1, 5},
            {7, 5, 6, 7, 6, 9}, {7, 2, 4, 2, 9, 7}, {4, 4, 5, 4, 6, 8}, {9, 5, 8, 5, 3, 4}, {7, 5, 7, 2, 3, 6},
            {6, 6, 8, 3, 3, 7}, {9, 4, 5, 1, 6, 7}, {8, 4, 8, 5, 3, 9}, {8, 4, 4, 7, 3, 8}, {5, 8, 2, 1, 5, 1},
            {2, 7, 1, 1, 9, 2}, {7, 4, 2, 5, 1, 3}, {3, 8, 2, 3, 4, 6}, {6, 8, 2, 8, 8, 5}, {4, 8, 1, 9, 2, 4},
            {2, 4, 8, 8, 2, 4}, {3, 2, 8, 1, 4, 3}, {9, 2, 8, 2, 9, 5}, {5, 1, 5, 5, 9, 6}, {8, 1, 5, 9, 8, 4},
            {4, 3, 5, 5, 4, 9}, {6, 2, 1, 4, 5, 2}, {4, 8, 7, 9, 6, 1}, {5, 6, 1, 3, 3, 2}, {6, 2, 1, 7, 5, 9},
            {4, 1, 5, 6, 6, 6}, {1, 4, 5, 3, 3, 7}, {9, 4, 9, 8, 7, 7}, {5, 7, 4, 5, 2, 8}, {4, 7, 1, 4, 7, 1},
            {5, 5, 4, 3, 9, 9}, {7, 3, 4, 5, 7, 7}, {9, 4, 5, 8, 4, 8}, {7, 4, 8, 8, 7, 1}, {1, 9, 1, 3, 3, 2},
            {1, 1, 6, 7, 1, 8}, {1, 2, 7, 1, 1, 3}, {9, 9, 5, 3, 2, 4}, {3, 2, 6, 3, 7, 6}, {7, 6, 2, 1, 2, 4}
    };

    private static final String scrambledNumbers = "7925163804";
    private static final String scrambledLetters = "gNQnPFpwRMhsvBWrJySOfxUDjHelCXAGaqtzEkuZKIdcVobLmYiT";
    private static final String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static String generateToken() {

        Calendar rightNow = Calendar.getInstance();
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        int minute = rightNow.get(Calendar.MINUTE);
        int day = rightNow.get(Calendar.DATE);
        int month = rightNow.get(Calendar.MONTH) + 1;
        int year = Integer.parseInt(Integer.toString(rightNow.get(Calendar.YEAR)).substring(2));
        int dayOfYear = rightNow.get(Calendar.DAY_OF_YEAR);

        Random random = new Random();
        int ran = random.nextInt(100);
        System.out.println(ran);

        int[] card = strategy[ran];
        int result = (hour + card[0]) * (minute + card[1]) * (day + card[2]) * (month + card[3])
                * (year + card[4]) * (dayOfYear + card[5]);
        int partial = result - card[0] - card[2] - card[5];
        String ranString = Integer.toString(ran);
        String pre, post;
        if (ranString.length() == 1) {
            pre = "H";
            post = ranString;
        } else {
            pre = String.valueOf(ranString.charAt(0));
            post = String.valueOf(ranString.charAt(1));

        }

        String token = pre + Integer.toString(partial).replaceAll("0", "X") + post;

        byte[] data = new byte[0];
        try {
            data = token.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "tokenError";
        }

        String baseToken = Base64.encodeToString(data, Base64.DEFAULT);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < baseToken.length(); i++) {
            char ch = baseToken.charAt(i);
            if (Character.isDigit(ch)){
                sb.append(scrambledNumbers.charAt(Character.getNumericValue(ch)));
            } else if (Character.isLetter(ch)){
                int index = letters.indexOf(ch);
                sb.append(scrambledLetters.charAt(index));
            }
        }

        return sb.toString();
    }

}
