package com.pitb.htmlimageparser;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLImageParserActivity extends AppCompatActivity {
    private final String TAG = HTMLImageParserActivity.class.getSimpleName();

    private LinearLayout linearLayout;
    private ViewDialog viewDialog;

    private Spanned htmlTxt;
    private String imgRegex = "(?i)<img[^>]+?src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";

//    private String headerRegex = "(?i)<h([1-6].*?)>(.*?)</h([1-6])>";
//    private String paraRegex = "(?i)<p>(.*?)</p>";

    private int imageIndex;
    public int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    private void initViews() {
        viewDialog = new ViewDialog(this);
        showCustomLoadingDialog();

        linearLayout = (LinearLayout) findViewById(R.id.layout);
    }

    private void parseHtml() {
        try {
            String data = htmlToStringConversion();

            if (data != null) {
                int count = 0;
                Pattern p = Pattern.compile(imgRegex);
                Matcher m = p.matcher(data);
                while (m.find()) {
                    count++;
                    if (count <= 1) {
                        imageIndex = (data.indexOf(m.group(0)));
                        String textData = data.substring(0, (data.indexOf(m.group(0))));
                        htmlTxt = Html.fromHtml(textData);
                        String imgSrc = m.group(1);
                        imageParser(imgSrc, htmlTxt);
                    } else {
                        String textData = data.substring(imageIndex, (data.indexOf(m.group(0))));
                        imageIndex = (data.indexOf(m.group(0)));
                        htmlTxt = Html.fromHtml(textData.split(imgRegex)[1]);
                        String imgSrc = m.group(1);
                        imageParser(imgSrc, htmlTxt);
                    }
                }
                String d = data.substring(imageIndex, data.length());
                htmlTxt = Html.fromHtml(d.split(imgRegex)[1]);
                imageParser("", htmlTxt);


            }
        } catch (IOException e) {
            viewDialog.hideDialog();
            e.printStackTrace();
        }
    }

    public String htmlToStringConversion() throws IOException {
        StringBuilder buf = new StringBuilder();
        String str = "";
        InputStream json = getAssets().open("html.txt");
        BufferedReader in = new BufferedReader(new InputStreamReader(json, "UTF-16"));
        while ((str = in.readLine()) != null) {
            buf.append(str);
        }
        return buf.toString();
    }

    public void imageParser(final String source, final Spanned paras) throws NullPointerException {
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                try {
                    if (bitmap.equals(null)) {
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
                        TextView text = new TextView(HTMLImageParserActivity.this);
                        text.setText(paras);
                        text.setLayoutParams(params);
                        linearLayout.addView(text);
                    } else {
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);

                        TextView text = new TextView(HTMLImageParserActivity.this);
                        text.setLayoutParams(params);
                        text.setText(paras);

                        linearLayout.addView(text);

                        ImageView image = new ImageView(HTMLImageParserActivity.this);
                        image.setLayoutParams(params);
                        image.setImageBitmap(bitmap);

                        linearLayout.addView(image);

                        BitmapDrawable drawable = (BitmapDrawable) image.getDrawable();
                        Bitmap bitmap1 = drawable.getBitmap();

                        File sdCardDirectory = Environment.getExternalStorageDirectory();
                        File f_image = new File(sdCardDirectory, ++count + ".jpg");

                        FileOutputStream outStream;
                        try {
                            outStream = new FileOutputStream(f_image);

                            /* 100 to keep full quality of the image */
                            runThread(bitmap1, outStream);

                            outStream.flush();
                            outStream.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
                    TextView text = new TextView(HTMLImageParserActivity.this);
                    text.setText(paras);
                    text.setLayoutParams(params);
                    linearLayout.addView(text);
                }
                viewDialog.hideDialog();
            }

            @Override
            protected Bitmap doInBackground(String... strings) {
                Bitmap bitmap = null;
                try {
                    if (!source.equals("")) {
                        URL url = new URL(source);
                        bitmap = BitmapFactory.decodeStream((InputStream) url.getContent());
                    } else {
                    }
                } catch (IOException e) {
                    String err = (e.getMessage() == null) ? "SD Card failed" : e.getMessage();
                    Log.e(TAG, err);
                }
                return bitmap;
            }
        }.execute();
    }

    private void runThread(final Bitmap bitmap, final FileOutputStream outputStream) {
        new Thread() {
            public void run() {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                        }
                    });
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void showCustomLoadingDialog() {
        viewDialog.showDialog();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                parseHtml();
            }
        }, 3000);
    }
}

