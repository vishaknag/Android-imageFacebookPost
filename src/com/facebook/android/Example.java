/*
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.android;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.facebook.android.SessionEvents.AuthListener;
import com.facebook.android.SessionEvents.LogoutListener;


public class Example extends Activity {

    // My Facebook Application ID must be set before running this application
    public static final String APP_ID = "132277280159308";

    private LoginButton mLoginButton;
    private TextView mText1;
    private TextView mText2;
    private EditText mTextBox;
    private Button mPostButton;
    private Gallery mImageGallery;
    private Context mContext;
    private int mImageSelected;
    private String mImageStory;
    private ImageAdapter galleryObj;
    
    private Facebook mFacebook;
    private AsyncFacebookRunner mAsyncRunner;
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        if (APP_ID == null) {
            Util.showAlert(this, "Warning", "Facebook Applicaton ID must be " +
                    "specified before running this example: see Example.java");
        }
        
        mContext = this;
        setContentView(R.layout.main);
        mLoginButton = (LoginButton) findViewById(R.id.login);
        mText1 = (TextView) Example.this.findViewById(R.id.text1);
        mText2 = (TextView) Example.this.findViewById(R.id.text2);
        mPostButton = (Button) findViewById(R.id.postButton);
        mImageGallery = (Gallery) findViewById(R.id.gallery);
        mTextBox = (EditText) findViewById(R.id.textBox); 
        
        
       	mFacebook = new Facebook(APP_ID);
       	mAsyncRunner = new AsyncFacebookRunner(mFacebook);

        SessionStore.restore(mFacebook, this);
        SessionEvents.addAuthListener(new VishakAndroidAuthListener());
        SessionEvents.addLogoutListener(new VishakAndroidLogoutListener());
        mLoginButton.init(this, mFacebook);
       
        mImageGallery.setAdapter(new ImageAdapter(this));

        mImageGallery.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
            	
            	// Store the index of the image selected in the gallery by the user
            	mImageSelected=position;
                
                //Toast.makeText(Example.this, "image selected = " + position, Toast.LENGTH_SHORT).show();
            }
        });
        mImageGallery.setVisibility(mFacebook.isSessionValid() ?
                View.VISIBLE :
                View.INVISIBLE);
        
        mTextBox.setVisibility(mFacebook.isSessionValid() ?
                View.VISIBLE :
                View.INVISIBLE);
        
        galleryObj = new ImageAdapter(this);
        mPostButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	
            	// Create a bundle - consists of image and the title to be posted to facebook wall
            	Bundle postContents = new Bundle();
            	String selectedImageURL = galleryObj.getImageURL(mImageSelected);
            	mImageStory = mTextBox.getText().toString();
            	
            	postContents.putString("attachment", "{\"name\":\""+mImageStory+"\","
            	+"\"media\":[{\"type\":\"image\",\"src\":\""+selectedImageURL+"\",\"href\":\""+selectedImageURL+"\"}]"
            	+"}");
            	
            	// OLD REST API stream.publish used to publish data onto the wall
            	mFacebook.dialog(mContext, "stream.publish", postContents, new VishakAndroidDialogListener());
            }
        });
        
        mPostButton.setVisibility(mFacebook.isSessionValid() ?
                View.VISIBLE :
                View.INVISIBLE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        mFacebook.authorizeCallback(requestCode, resultCode, data);
    }

    public class VishakAndroidAuthListener implements AuthListener {

        public void onAuthSucceed() {
        	mImageGallery.setVisibility(View.VISIBLE);
        	mTextBox.setVisibility(View.VISIBLE);
        	mText1.setVisibility(View.INVISIBLE);
        	mText2.setVisibility(View.INVISIBLE);
            mPostButton.setVisibility(View.VISIBLE);
        }

        public void onAuthFail(String error) {
        	mText2.setVisibility(View.INVISIBLE);
            mText1.setText("Login Failed: " + error);
        }
    }

    public class VishakAndroidLogoutListener implements LogoutListener {
        public void onLogoutBegin() {
            mText1.setText("Logging out...");
            mText1.setVisibility(View.VISIBLE);
        }

        public void onLogoutFinish() {
        	mImageGallery.setVisibility(View.INVISIBLE);
        	mTextBox.setVisibility(View.INVISIBLE);
        	mPostButton.setVisibility(View.INVISIBLE);
            mText1.setText("You have logged out of Vishak Homework#9 ");
            mText2.setText("Android Facebook Post");
            mText1.setVisibility(View.VISIBLE);
        	mText2.setVisibility(View.VISIBLE);
        }
    }

    public class WallPostRequestListener extends BaseRequestListener {
    	// Invoked when the request action is responded by facebook server
        public void onComplete(final String response) {
            Log.d("Facebook-Example", "Got response: " + response);
            String message = "<empty>";
            try {
                JSONObject json = Util.parseJson(response);
                message = json.getString("message");
            } catch (JSONException e) {
                Log.w("Facebook-Example", "JSON Error in response");
            } catch (FacebookError e) {
                Log.w("Facebook-Example", "Facebook Error: " + e.getMessage());
            }
       
        }
    }

    // called when the stream-publish screen is filled with post text value and then published/skipped
    public class VishakAndroidDialogListener extends BaseDialogListener {

        public void onComplete(Bundle values) {
            final String postId = values.getString("post_id");
            if (postId != null) {
                Log.d("Facebook-Example", "Dialog Success! post_id=" + postId);
                
                // get the post id and send a request to get it posted on wall
                // once posted/failure then call back invoked
                mAsyncRunner.request(postId, new WallPostRequestListener());
               
            } else {
                Log.d("Facebook-Example", "No wall post made");
            }
        }
    }
    
//*****************************************************************************
// 			Displaying the Image gallery on the activity  
//*****************************************************************************    

    public class ImageAdapter extends BaseAdapter {
    	
        private int mGalleryItemBackground;
        private Context mContext;
          
        // URL strings to remote online images in USC server 
        private String[] mOnlineImages = {
        		"http://www-scf.usc.edu/~csci571/2010Fall/hw9/photo1.jpg",
        		"http://www-scf.usc.edu/~csci571/2010Fall/hw9/photo2.jpg",
        		"http://www-scf.usc.edu/~csci571/2010Fall/hw9/photo3.jpg",
        		"http://www-scf.usc.edu/~csci571/2010Fall/hw9/photo4.jpg"
        }; 

        // Constructor saving the 'parent' context
        public ImageAdapter(Context thisContext) {
            mContext = thisContext;
            TypedArray mStyleArray = obtainStyledAttributes(R.styleable.Vishak_Android_App);
            mGalleryItemBackground = mStyleArray.getResourceId(
                    R.styleable.Vishak_Android_App_android_galleryItemBackground, 0);
            
            // Recycling the allocated array memory  
            mStyleArray.recycle();
        }
        
        // Returns the count of the number of images in the gallery
        public int getCount() {
            return mOnlineImages.length;
        }
        
        // Use the array indices as unique IDs for the images
        public Object getItem(int position) {
            return position;
        }
        
        // Use the array indices as unique IDs for the images
        public long getItemId(int position) {
            return position;
        }
        
        public String getImageURL(int position) {	
        	return mOnlineImages[position];	
        }

        // New ImageView obtained based on the position is returned to be displayed 
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView mImageView = new ImageView(mContext);
            
            try {
            	// Open a new URL
            	URL mImageURL = new URL(mOnlineImages[position]);
            	URLConnection conn = mImageURL.openConnection();
            	
            	// Connect to the URL created
            	conn.connect();
            	
            	// Get the InputStream to load data from the URL connection established
            	InputStream mInputStream = conn.getInputStream();
            	
            	// Buffering - Expensive interaction with the underlying input stream is minimized
            	BufferedInputStream mBufferedInputStream = new BufferedInputStream(mInputStream);
            	
            	// Convert the buffer information into bitmap
            	Bitmap mBitmap = BitmapFactory.decodeStream(mBufferedInputStream);
            	
            	// After the usage of the input & buffer input streams, close them
            	mBufferedInputStream.close();
            	mInputStream.close();
            	
            	// Apply the Bitmap to the ImageView that will be returned. */
            	mImageView.setImageBitmap(mBitmap);
            	mImageView.setLayoutParams(new Gallery.LayoutParams(180, 120));
            	mImageView.setScaleType(ImageView.ScaleType.CENTER);
            	mImageView.setBackgroundResource(mGalleryItemBackground);
            	
            }
            catch (MalformedURLException error) {
            	Log.e("DEBUGTAG", "Remote URL Exception", error);
            }
            catch (IOException error) {
            	Log.e("DEBUGTAG", "Remote Image Exception", error);
            }
            
            return mImageView;
        }    

    }    
}