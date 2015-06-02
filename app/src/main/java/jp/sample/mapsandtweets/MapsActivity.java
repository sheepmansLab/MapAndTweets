package jp.sample.mapsandtweets;

import android.content.Intent;
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

import java.util.ArrayList;
import java.util.List;

import jp.sample.mapsandtweets.asynctask.ImageDownloadAsyncTask;
import jp.sample.mapsandtweets.asynctask.SearchTweetsAsyncTask;
import jp.sample.mapsandtweets.util.CommonConst;
import jp.sample.mapsandtweets.util.TwitterUtil;
import twitter4j.Status;

public class MapsActivity extends FragmentActivity{
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    //MMを初期表示
    private LatLng mLocation = new LatLng(35.458412, 139.632402);    //MMパークビル
    //Map上のマーカーのリスト
    private List<Marker> mListMarker;

    /**
     * Activityが作成された際に呼ばれる
     * @param savedInstanceState 画面情報を保持するBundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
    }

    /**
     * Activityが開始(再開)する時に呼ばれる
     */
    @Override
    protected void onResume() {
        super.onResume();
        //Twitterの認証有無をチェックする
        if(!TwitterUtil.hasAccessToken(this)){
            //認証データが無い場合認証画面へ遷移
            Intent intent = new Intent(this, TwitterAuthActivity.class);
            startActivityForResult(intent, CommonConst.REQUEST_CODE_TWITTER_AUTH);
        } else {
            //認証済みの場合画面情報をセットする
            setUpMapIfNeeded();
            //Activity上に配置したボタンをIDから取得
            Button btnMapSearch = (Button)findViewById(R.id.btnMapSearch);
            //ボタンにクリックイベントをセット
            btnMapSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO タッチした位置をベースに検索しているので、Mapの中心位置を検索対象にしたい
                    //検索処理 ネットワーク接続する場合は非同期処理にする必要がある
                    new SearchTweetsAsyncTask(MapsActivity.this).execute(mLocation);
                }
            });
        }
    }

    /**
     * Mapのセットアップの有無を判定し、必要ならば実施する
     * (自動生成されるメソッド)
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
     * Mapのセットアップ
     * (自動生成されるメソッドを一部変更)
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
     * @param location  位置情報LatLng
     */
    private void moveCamera(LatLng location){
        //TODO ズームや傾きなどがリセットされてしまうので画面情報を保持したい
        float zoom = 15.0f;     //拡大
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

    /**
     * ツイートの結果を受け取りMapに落とす
     * @param statuses　ツイートの検索結果
     */
    public void protMarkers(List<Status> statuses) {
        //マーカーのリストが無い場合作成する
        if(mListMarker == null){
            //後から不特定のマーカーを操作するのは面倒なのでリストにまとめて参照しやすくする
            mListMarker = new ArrayList<Marker>();
        } else {
            //Map上のマーカーをクリアする
            for(Marker m : mListMarker){
                //マーカーの削除
                m.remove();
            }
        }
        //tweetのリストの件数分処理する
        for(Status status : statuses){
            //位置情報を持っているものだけ
            if(status.getGeoLocation() != null){
                //位置情報から座標を取得
                LatLng pos = new LatLng(status.getGeoLocation().getLatitude()
                                        ,status.getGeoLocation().getLongitude());
                //Mapにマーカーをセット
                Marker marker = mMap.addMarker(new MarkerOptions().position(pos));
                //タイトルに表示名、ユーザ名、作成日付をセット
                //TODO 日付の表示形式は面倒なのでそのまま
                marker.setTitle(status.getUser().getName()
                        + "(@" + status.getUser().getScreenName() +')'
                        + " " + status.getCreatedAt());
                //スニペットにメッセージをセット
                marker.setSnippet(status.getText());
                //アイコン画像をDLしてアイコンにセットする
                //NW越しの操作のため非同期処理(AsyncTask)で実行
                new ImageDownloadAsyncTask(marker)
                        .execute(status.getUser().getProfileImageURL());
                //マーカーをリストに追加
                mListMarker.add(marker);
            }
        }
    }
}
