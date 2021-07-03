package com.pcordoba.speechtotexttest1;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private String status = "OK";

    public String postVoiceResult(ArrayList<String> matches, String restUrl, String restPort,
                                  final Context context, final boolean show, final boolean isLastCheckCall) {


        JSONObject matchesJson = jsonParser(matches);

        RequestQueue queue = Volley.newRequestQueue(context, new HurlStack(null, newSslSocketFactory()));
        String url = "https://" + restUrl + ":" + restPort;


        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, matchesJson,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        if (show) {
                            updateReturnedText(response);
                        } else {
                            ((MainActivity) context).checkStartingStatus("OK", isLastCheckCall);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                String message = null;
                if (volleyError instanceof NoConnectionError) {
                    message = "NoConnectionError";
                } else if (volleyError instanceof ServerError) {
                    message = "ServerError";
                } else if (volleyError instanceof AuthFailureError) {
                    message = "AuthFailureError";
                } else if (volleyError instanceof ParseError) {
                    message = "ParseError";
                } else if (volleyError instanceof NetworkError) {
                    message = "NetworkError";
                } else if (volleyError instanceof TimeoutError) {
                    message = "TimeoutError";
                }

                status = message;
                if (show) {
                    updateReturnedText(message);
                } else {
                    ((MainActivity) context).checkStartingStatus(status, isLastCheckCall);
                }
                //updateReturnedText(error.toString());
            }

        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("authorization", SignUtil.getGeneratedToken());
                return headers;
            }
        };

        jsonRequest.setShouldRetryServerErrors(true);

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                3,
                2));


        queue.add(jsonRequest);

        return status;
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
                trusted.load(in, SignUtil.KEYSTORE_PASSWORD);
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
            kmf.init(trusted, SignUtil.KEYSTORE_PASSWORD);

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
                    String resp = stringResponse.get(i).toString();
                    Matcher m = Pattern.compile("\\((.*?)\\)").matcher(resp);
                    if (m.find()) {
                        System.out.println(m.group(1));
                        list.add(resp.replace("("+m.group(1)+")", ""));
                    } else {
                        list.add(stringResponse.get(i).toString());
                    }
                } catch (JSONException e) {
                    executedCmd = "Cant extract data from status";
                }
            }
        }

        MainActivity.statusListAdapter.addAll(list);
        TextView capturedVoiceCmd = (TextView) ((Activity) context).findViewById(R.id.resultMessage);
        capturedVoiceCmd.setText(executedCmd);
    }

}
