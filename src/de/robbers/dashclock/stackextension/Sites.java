
package de.robbers.dashclock.stackextension;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.Html;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Sites {
    private Context mContext;
    private JSONArray mSites;

    public Sites(Context context) {
        mContext = context;
        loadSites();
    }

    private void loadSites() {
        AssetManager assetManager = mContext.getAssets();
        String json = null;

        try {
            InputStream inputStream = assetManager.open("sites.json");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                total.append(line);
            }
            json = total.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mSites = new JSONArray(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public CharSequence[] getNames() {
        CharSequence[] list = new CharSequence[mSites.length()];
        for (int i = 0; i < mSites.length(); i++) {
            try {
                JSONObject site = mSites.getJSONObject(i);
                list[i] = Html.fromHtml(site.getString("name"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public CharSequence[] getApiParameters() {
        CharSequence[] list = new CharSequence[mSites.length()];
        for (int i = 0; i < mSites.length(); i++) {
            try {
                JSONObject site = mSites.getJSONObject(i);
                list[i] = site.getString("api_site_parameter");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public CharSequence[] getUrls() {
        CharSequence[] list = new CharSequence[mSites.length()];
        for (int i = 0; i < mSites.length(); i++) {
            try {
                JSONObject site = mSites.getJSONObject(i);
                list[i] = site.getString("site_url");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public String getUrlFromApiParameter(String apiParameter) {
        for (int i = 0; i < mSites.length(); i++) {
            try {
                JSONObject site = mSites.getJSONObject(i);
                if (site.getString("api_site_parameter").equals(apiParameter)) {
                    return site.getString("site_url");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getNameFromApiParameter(String apiParameter) {
        for (int i = 0; i < mSites.length(); i++) {
            try {
                JSONObject site = mSites.getJSONObject(i);
                if (site.getString("api_site_parameter").equals(apiParameter)) {
                    return site.getString("name");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public int getIcon(String apiParameter) {
        if (apiParameter.equals("stackoverflow")) {
            return R.drawable.ic_stackoverflow;
        } else if (apiParameter.equals("serverfault")) {
            return R.drawable.ic_serverfault;
        } else if (apiParameter.equals("superuser")) {
            return R.drawable.ic_superuser;
        } else if (apiParameter.equals("askubuntu")) {
            return R.drawable.ic_askubuntu;
        } else if (apiParameter.equals("meta.stackoverflow")) {
            return R.drawable.ic_stackoverflow;
        } else {
            return R.drawable.ic_stackexchange;
        }
    }
}
