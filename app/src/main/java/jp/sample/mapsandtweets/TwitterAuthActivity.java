package jp.sample.mapsandtweets;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import jp.sample.mapsandtweets.util.CommonConst;
import jp.sample.mapsandtweets.util.TwitterUtil;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Twitter認証用のActivity
 */
public class TwitterAuthActivity extends Activity {
    Twitter mTwitter;
    RequestToken mRequestToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String callbackUrl = getResources().getString(R.string.twitter_callback_url);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_auth);

        //認証開始のボタン
        //TODO 本来自動的にリダイレクトさせたいが無限ループしてしまうのでボタンを介している
        Button btnAuthStart = (Button)findViewById(R.id.btnAuthStart);
        btnAuthStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Twitterインスタンスを取得(未認証の場合あり)
                mTwitter = TwitterUtil.getTwitterInstance(TwitterAuthActivity.this); //コンシューマキー、コンシューマシークレットはセット済み
                //コールバックURLを指定してリクエストトークンを取得
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... voids) {
                        try {
                            //リクエストトークンを取得し認証画面のURLを返す
                            mRequestToken = mTwitter.getOAuthRequestToken(callbackUrl);
                            return mRequestToken.getAuthorizationURL();
                        } catch (TwitterException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        if(s != null) {
                            //取得したリクエストトークンからURLを取得し、コールバックURLを指定して遷移
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
                            //認証画面(Twitterのサービス)へ遷移
                            startActivity(intent);
                        }
                    }
                }.execute();
            }
        });
    }

    /**
     * 別画面(Twitter認証画面)から遷移してきた際に呼び出される
     * Manifest.xmlにSingleTaskを設定していないと呼ばれない
     * @param intent    Intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        //データがない、意図しないアクセスの場合はスルーする
        if(intent != null
                && intent.getData() != null
                && intent.getData().toString().startsWith("cda://twitter")){
            //認証データを保持させる
            String verifier = intent.getData().getQueryParameter("oauth_verifier");
            new AsyncTask<String, Void, AccessToken>() {
                /**
                 * 非同期処理で認証情報を取得する
                 * @param strings   oauth_verifier
                 * @return  アクセストークン
                 */
                @Override
                protected AccessToken doInBackground(String... strings) {
                    try {
                        //アクセストークンを取得
                        return mTwitter.getOAuthAccessToken(mRequestToken, strings[0]);
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                /**
                 * 事後処理
                 * @param accessToken   doInBackgroundの結果
                 */
                @Override
                protected void onPostExecute(AccessToken accessToken) {
                    super.onPostExecute(accessToken);
                    //認証成功処理を実施
                    successAuthorize(accessToken);
                }
            }.execute(verifier);
        }
    }

    /**
     * 認証が成功した場合の処理
     * @param accessToken　取得したAccessToken
     */
    private void successAuthorize(AccessToken accessToken){
        //AccessTokenを保存する
        TwitterUtil.storeAccessToken(this, accessToken);
        //結果コードを成功(0)とする
        setResult(CommonConst.RESULT_CODE_SUCCESS);
        //元の画面に戻る
        finish();
    }
}
