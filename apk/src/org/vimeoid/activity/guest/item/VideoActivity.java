/**
 * 
 */
package org.vimeoid.activity.guest.item;

import org.vimeoid.R;
import org.vimeoid.activity.guest.list.VideosActivity;
import org.vimeoid.adapter.ActionItem;
import org.vimeoid.adapter.SectionedActionsAdapter;
import org.vimeoid.connection.ApiCallInfo;
import org.vimeoid.connection.VimeoApi;
import org.vimeoid.connection.simple.VimeoProvider;
import org.vimeoid.dto.simple.Video;
import org.vimeoid.util.Dialogs;
import org.vimeoid.util.Utils;

import com.fedorvlasov.lazylist.ImageLoader;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


/**
 * <dl>
 * <dt>Project:</dt> <dd>vimeoid</dd>
 * <dt>Package:</dt> <dd>org.vimeoid.activity.guest.item</dd>
 * </dl>
 *
 * <code>Video</code>
 *
 * <p>Description</p>
 *
 * @author Ulric Wilfred <shaman.sir@gmail.com>
 * @date Sep 10, 2010 12:15:38 AM 
 *
 */
public class VideoActivity extends Activity {
    
    public static final String TAG = "Video";
    
    private View titleBar;
    private ImageLoader imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.view_single_video);
        
        final Uri contentUri = getIntent().getData();
        ApiCallInfo callInfo = VimeoProvider.collectCallInfo(contentUri);        
        
        String subjectName = callInfo.subject;
        if (getIntent().hasExtra(VideosActivity.VIDEO_TITLE_EXTRA)) {
        	subjectName = getIntent().getStringExtra(VideosActivity.VIDEO_TITLE_EXTRA);
        }
        
        titleBar = findViewById(R.id.titleBar);
        ((ImageView)titleBar.findViewById(R.id.subjectIcon)).setImageResource(Utils.drawableByContent(callInfo.subjectType));
        ((TextView)titleBar.findViewById(R.id.subjectTitle)).setText(subjectName);
        ((ImageView)titleBar.findViewById(R.id.resultIcon)).setImageResource(R.drawable.info);
        
        final View progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        
        imageLoader = new ImageLoader(this, R.drawable.item_loading_small, R.drawable.item_failed_small);
        
        WebView playerView = (WebView)findViewById(R.id.videoPlayer);
        playerView.getSettings().setJavaScriptEnabled(true);
        //playerView.getSettings().setLoadsImagesAutomatically(true);
        
        final long videoId = Long.valueOf(callInfo.subject);
        final int playerHeight = getResources().getDimensionPixelSize((R.dimen.video_player_height));
        
        playerView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setVisibility(((newProgress == 0) || (newProgress == 100)) ? View.GONE : View.VISIBLE);
                if (newProgress == 100) ((ListView)findViewById(R.id.actionsList)).setSelectionAfterHeaderView();
                super.onProgressChanged(view, newProgress);
            }
        });
        
        Log.d(TAG, "Loading player: " + VimeoApi.getPlayerUrl(videoId, playerHeight));
        playerView.loadUrl(VimeoApi.getPlayerUrl(videoId, playerHeight));
        
        new LoadItemTask(Video.FULL_EXTRACT_PROJECTION).execute(contentUri);
        
    }
    
    protected void onVideoDataReceived(Video video) {
    	
    	Log.d(TAG, "video " + video.id + " data received, uploader: " + video.uploaderName);
    	((TextView)titleBar.findViewById(R.id.subjectTitle)).setText(video.title);
    	
    	((TextView)findViewById(R.id.videoDescription)).setText(Html.fromHtml(video.description));
    	
    	imageLoader.displayImage(video.mediumUploaderPortraitUrl, ((ImageView)findViewById(R.id.uploaderPortrait)));
    	
    	final SectionedActionsAdapter actionsAdapter = new SectionedActionsAdapter(this, getLayoutInflater(), imageLoader);
    	
    	int statsSection = actionsAdapter.addSection("Statistics");
    	final ActionItem commentsAction = actionsAdapter.addAction(statsSection, R.drawable.comment_video, String.valueOf(video.commentsCount));
    	if (video.commentsCount > 0) {
    		commentsAction.onClick = new OnClickListener() {				
				@Override public void onClick(View v) {
					Dialogs.makeToast(VideoActivity.this, "View comments");
				}
			};
    	}
    	if (video.tags.length > 0) actionsAdapter.addAction(statsSection, R.drawable.tag, Utils.adaptTags(video.tags, "[none]"));    	
    	actionsAdapter.addAction(statsSection, R.drawable.play, String.valueOf(video.playsCount));
    	actionsAdapter.addAction(statsSection, R.drawable.like, String.valueOf(video.likesCount));
    	
    	int infoSection = actionsAdapter.addSection("Information");
    	/* actionsAdapter.addAction(infoSection, video.smallUploaderPortraitUrl, 
    	                            Utils.format(getString(R.string.videoUploader), "name", video.uploaderName)); */
    	final ActionItem userAction = actionsAdapter.addAction(infoSection, R.drawable.contact, video.uploaderName);
    	userAction.onClick =  new OnClickListener() {				
			@Override public void onClick(View v) {
				Dialogs.makeToast(VideoActivity.this, "View user page");
			}
		};
    	actionsAdapter.addAction(infoSection, R.drawable.contact, video.uploadedOn);
    	actionsAdapter.addAction(infoSection, R.drawable.contact, video.width + "x" + video.height);
    	
    	final ListView actionsList = (ListView)findViewById(R.id.actionsList);
    	actionsList.setAdapter(actionsAdapter);
    	actionsList.invalidate();
    	actionsList.setSelectionAfterHeaderView();    	
    }
    
    protected class LoadItemTask extends AsyncTask<Uri, Void, Cursor> {

        // TODO: show progress as a dialog
        
        private final String[] projection;
        private final View progressBar;
        
        protected LoadItemTask(String[] projection) {
            this.projection = projection;
            this.progressBar = findViewById(R.id.progressBar);   
        }
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            
            progressBar.setVisibility(View.VISIBLE);            
        }

        @Override
        protected Cursor doInBackground(Uri... uris) {
            if (uris.length <= 0) return null;
            if (uris.length > 1) throw new UnsupportedOperationException("This task do not supports several params");
            return getContentResolver().query(uris[0], projection, null, null, null);
        }
        
        @Override
        protected void onPostExecute(Cursor cursor) {
            
            if (cursor != null) {
            	
            	if (cursor.getCount() > 1) throw new IllegalStateException("There must be the only one item returned");
            	
            	cursor.moveToFirst();
            	onVideoDataReceived(Video.fullFromCursor(cursor, 0));
                cursor.close();
            }
            
            progressBar.setVisibility(View.GONE);            
        }

    }
    
}
