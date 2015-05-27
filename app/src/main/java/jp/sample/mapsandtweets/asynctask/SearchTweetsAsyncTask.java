package jp.sample.mapsandtweets.asynctask;

import android.app.Activity;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jp.sample.mapsandtweets.MapsActivity;
import jp.sample.mapsandtweets.R;
import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

/**
 */
public class SearchTweetsAsyncTask extends AsyncTask<LatLng, Void, List<Status>> {
    Activity mActivity;

    public SearchTweetsAsyncTask(Activity activity){
        mActivity = activity;
    }

    /**
     * 非同期処理の実行　ツイートを検索する
     * @param params    検索対象の位置情報
     * @return  ツイートのList
     */
    @Override
    protected List<twitter4j.Status> doInBackground(LatLng... params) {
        List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();
        LatLng location = params[0];

        //Twitterインスタンの生成
        Twitter twitter = new TwitterFactory().getInstance();

        //検索用のクエリを生成
        Query query = new Query();
        //日本に設定
        query.setLang("ja");
        query.setLocale("ja");
        //検索用の位置情報
        GeoLocation geo = new GeoLocation(location.latitude, location.longitude);
        //指定位置から半径5キロを検索
        query.setGeoCode(geo, 5, Query.KILOMETERS);
        //件数は100件まで
        query.setCount(100);

        QueryResult qr = null;
        try {
            //検索処理
            qr = twitter.search(query);
            statuses = qr.getTweets();

            //位置情報を持っていないものを省く
            Iterator<twitter4j.Status> ite = statuses.iterator();
            while(ite.hasNext()){
                twitter4j.Status s = (twitter4j.Status)ite.next();
                if(s.getGeoLocation() == null){
                    ite.remove();
                }
            }
        } catch (TwitterException e) {
            //TODO エラー処理は割愛
            e.printStackTrace();
        }
        return statuses;
    }

    /**
     * 非同期処理の終了時に呼ばれるメソッド
     * @param statuses  doInBackgroundの処理結果
     */
    @Override
    protected void onPostExecute(List<twitter4j.Status> statuses) {
        super.onPostExecute(statuses);
        //TODO ホントはInterfaceを整備したいがサンプルのため割愛
        if(mActivity instanceof MapsActivity){
            //非同期処理の結果をActivityへ渡す
            ((MapsActivity)mActivity).callback(statuses);
        }
    }
}
