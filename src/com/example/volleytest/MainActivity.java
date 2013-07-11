
package com.example.volleytest;

import java.io.UnsupportedEncodingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

public class MainActivity extends Activity {

    public final static String FLICKR_URL = "http://api.flickr.com/services/feeds/photos_public.gne?format=json&nojsoncallback=1";

    public final static int IMAGE_CACHE_SIZE_KB = (int)(Runtime.getRuntime().maxMemory() / 1024 / 5);

    private GridView mGrid;

    private ImageLoader imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(MainActivity.class.getSimpleName(), "#onCreate");
        setContentView(R.layout.activity_main);

        mGrid = (GridView)findViewById(R.id.grid);

        // NetworkImageView

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        requestQueue.add(new FlickrFeedRequest());

        imageLoader = new ImageLoader(requestQueue, new BitmapLruCache(IMAGE_CACHE_SIZE_KB));

        // requestQueue.start();

    }

    private final class FlickrFeedRequest extends Request<FlickrItem[]> {

        public FlickrFeedRequest(int method, String url, ErrorListener listener) {
            super(method, url, listener);
        }

        public FlickrFeedRequest() {
            super(Method.GET, FLICKR_URL, null);
        }

        @Override
        protected Response<FlickrItem[]> parseNetworkResponse(NetworkResponse response) {
            // Work in background

            Response<FlickrItem[]> result;

            String jsonString;
            try {

                // response.data
                Log.i(FlickrFeedRequest.class.getSimpleName(), "#parseNetworkResponse");

                jsonString = new String(response.data,
                        HttpHeaderParser.parseCharset(response.headers));
                JSONObject jsonObject = new JSONObject(jsonString);
                JSONArray jsonArray = jsonObject.getJSONArray("items");

                FlickrItem[] items = new FlickrItem[jsonArray.length()];

                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject flickrObject = jsonArray.getJSONObject(i);

                    String title = flickrObject.getString("title");

                    JSONObject media = flickrObject.getJSONObject("media");
                    String url = media.getString("m");

                    items[i] = new FlickrItem(title, url);
                }

                result = Response.success(items, HttpHeaderParser.parseCacheHeaders(response));
                Log.i(FlickrFeedRequest.class.getSimpleName(), result.toString());

            } catch (JSONException e) {

                // parse error that grabbed from server
                result = Response.error(new ParseError(e));

            } catch (UnsupportedEncodingException e) {

                result = Response.error(new ParseError(e));
            }

            return result;
        }

        @Override
        protected void deliverResponse(FlickrItem[] response) {
            // execute on main thread
            Log.i(FlickrFeedRequest.class.getSimpleName(), "#deliverResponse");
            mGrid.setAdapter(new FlickrItemsAdapter(getApplicationContext(), response, imageLoader));

        }
    }

    public static class FlickrItemsAdapter extends BaseAdapter {

        private Context mContext;

        private FlickrItem[] mData;

        private ImageLoader mLoader;

        public FlickrItemsAdapter(Context mContext, FlickrItem[] mData, ImageLoader loader) {
            super();
            this.mContext = mContext;
            this.mData = mData;
            this.mLoader = loader;
        }

        @Override
        public int getCount() {
            return mData.length;
        }

        @Override
        public Object getItem(int position) {
            return mData[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.flickr_item, parent,
                        false);
            }
            ((TextView)convertView.findViewById(R.id.flickr_title)).setText(mData[position].title);
            ((NetworkImageView)convertView.findViewById(R.id.image)).setImageUrl(
                    mData[position].url, mLoader);

            return convertView;
        }

    }

}
