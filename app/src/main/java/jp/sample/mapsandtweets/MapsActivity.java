package jp.sample.mapsandtweets;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import jp.sample.mapsandtweets.asynctask.SearchTweetsAsyncTask;
import jp.sample.mapsandtweets.util.TwitterUtil;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.Authorization;
import twitter4j.auth.RequestToken;

public class MapsActivity extends FragmentActivity {
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    //MMを初期表示
    private LatLng mLocation = new LatLng(35.458412, 139.632402);    //MMパークビル

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        //Activity上に配置したボタンをIDから取得
        Button btnMapSearch = (Button)findViewById(R.id.btnMapSearch);
        //ボタンにクリックイベントをセット
        btnMapSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //検索処理 ネットワーク接続する場合は非同期処理にする必要がある
                SearchTweetsAsyncTask task = new SearchTweetsAsyncTask(MapsActivity.this);
                task.execute(mLocation);
            }
        });

        if(!TwitterUtil.hasAccessToken(this)){
            startTwitterAuth();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final String CALLBACK_URL = getResources().getString(R.string.twitter_callback_url);
        if(intent == null || intent.getData() == null
                || !intent.getData().toString().startsWith(CALLBACK_URL)){
            return;
        }
        String verifier = intent.getData().getQueryParameter("oauth_verifier");

        AsyncTask<String, Void, AccessToken> task = new AsyncTask<String, Void, AccessToken>() {
            @Override
            protected AccessToken doInBackground(String... params) {
//                try {
//                    return mTwitter.getOAuthAccessToken(mRequestToken, params[0]);
//                } catch (TwitterException e) {
//                    e.printStackTrace();
//                }
                return null;
            }

            @Override
            protected void onPostExecute(AccessToken accessToken) {
                if (accessToken != null) {
                    // 認証成功！
                } else {
                    // 認証失敗
                }
            }
        };
        task.execute(verifier);
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //Mapにクリックイベントをセットする
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //クリックした位置を保持
                mLocation = latLng;
                //クリックした位置に移動
                moveCamera(mLocation);
            }
        });
        //カメラを移動
        moveCamera(mLocation);
    }

    /**
     * 指定した座標に表示を移動させる
     * @param location
     */
    private void moveCamera(LatLng location){
        float zoom = 18.0f;     //拡大
        float bearing = 0.0f;   //向き
        float tilt = 0.0f;      //傾き
        //カメラの位置情報を生成
        CameraPosition pos = new CameraPosition.Builder()
                                                .target(location)   //表示座標
                                                .zoom(zoom)         //拡大
                                                .bearing(bearing)   //向き
                                                .tilt(tilt)         //傾き
                                                .build();
        //マップに反映
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
    }

    private void startTwitterAuth(){
        final String CALLBACK_URL = getResources().getString(R.string.twitter_callback_url);

        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    RequestToken requestToken = TwitterUtil.getTwitterInstance(MapsActivity.this)
                            .getOAuthRequestToken(CALLBACK_URL);
                    return requestToken.getAuthorizationURL();
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String url) {
                super.onPostExecute(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        };
        task.execute();
    }

    /**
     * ツイートの結果を受け取りMapに落とす
     * @param statuses　ツイートの検索結果
     */
    public void callback(List<Status> statuses) {
        //tweetのリスト
        for(Status status : statuses){
            //位置情報を持っているものだけ
            if(status.getGeoLocation() != null){
                //位置情報から座標を取得
                LatLng pos = new LatLng(status.getGeoLocation().getLatitude()
                                        ,status.getGeoLocation().getLongitude());
                //Mapにマーカーをセット
                Marker marker = mMap.addMarker(new MarkerOptions().position(pos));
                marker.setTitle(status.getText());
            }
        }
    }
}
