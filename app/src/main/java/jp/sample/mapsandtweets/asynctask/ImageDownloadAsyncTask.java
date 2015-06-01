package jp.sample.mapsandtweets.asynctask;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by hiroshi.tsutsumi on 2015/06/01.
 */
public class ImageDownloadAsyncTask extends AsyncTask<String, Void, Bitmap> {
    private Marker mMarker;

    /**
     * コンストラクタ
     * @param marker    結果を反映するための対象のマーカー
     */
    public ImageDownloadAsyncTask(Marker marker){
        mMarker = marker;
    }

    /**
     * URLを受け取って画像(Bitmap)を返す
     * @param params
     * @return
     */
    @Override
    protected Bitmap doInBackground(java.lang.String... params) {
        try{
            if(params[0] != null){
                //URLから画像データを取得しデコード
                URL url = new URL(params[0]);
                return BitmapFactory.decodeStream(url.openStream());
            }
        } catch (MalformedURLException me) {
            me.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
        return null;
    }

    /**
     * 事後処理
     * @param bitmap    doInBackgroundの結果のBitmap
     */
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        //Nullチェック
        if(bitmap != null) {
            //対象のマーカーに画像をセット
            mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
        }
    }
}
