package com.dilerdesenvolv.cognitive;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.*;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.graphics.*;
import android.widget.*;
import android.provider.*;

import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;

public class MainActivity extends AppCompatActivity {

    private final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;
    private FaceServiceClient faceServiceClient =
            new FaceServiceRestClient("https://westcentralus.api.cognitive.microsoft.com/face/v1.0",
                    "");
    private TextView tv_info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                gallIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
            }
        });

//        Button button1 = findViewById(R.id.button1);
//        button1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
//                gallIntent.setType("image/*");
//                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
//            }
//        });

        tv_info = findViewById(R.id.tv_info);

        detectionProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ImageView imageView = findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap);

                detectAndFrame(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Detect faces by uploading face images
    // Frame faces after detection
    private void detectAndFrame(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Detecting...");

                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    new FaceServiceClient.FaceAttributeType[] {
                                            FaceServiceClient.FaceAttributeType.Age,
                                            FaceServiceClient.FaceAttributeType.FacialHair,
                                            FaceServiceClient.FaceAttributeType.Gender,
                                            FaceServiceClient.FaceAttributeType.HeadPose,
                                            FaceServiceClient.FaceAttributeType.Smile
                                    }        // returnFaceAttributes: a string like "age, gender"
                            );
                            if (result == null) {
                                publishProgress("Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(
                                    String.format("Detection Finished. %d face(s) detected",
                                            result.length));
                            return result;
                        } catch (Exception e) {
                            publishProgress("Detection failed");
                            return null;
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        detectionProgressDialog.show();
                    }

                    @Override
                    protected void onProgressUpdate(String... progress) {
                        detectionProgressDialog.setMessage(progress[0]);
                    }

                    @Override
                    protected void onPostExecute(Face[] result) {
                        detectionProgressDialog.dismiss();
                        if (result == null) {
                            return;
                        }
                        ImageView imageView = findViewById(R.id.imageView1);
                        imageView.setImageBitmap(drawFaceRectanglesOnBitmap(imageBitmap, result, tv_info));
                        imageBitmap.recycle();
                    }
                };
        detectTask.execute(inputStream);
    }

    private static Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, Face[] faces, TextView tv_info) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        int stokeWidth = 2;
        paint.setStrokeWidth(stokeWidth);
        if (faces != null) {
            String text = "";
            for (Face face : faces) {
                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);
                text += "Person: " + face.faceId + "\r\n";
                text += "Smile: " + face.faceAttributes.smile + "\r\n" +
                        "Gender: " + face.faceAttributes.gender + "\r\n" +
                        "Age: " + face.faceAttributes.age + "\r\n" +
                        "Facial Hair: beard " + face.faceAttributes.facialHair.beard + "\r\n" +
                        "Facial Hair: moustache " + face.faceAttributes.facialHair.moustache + "\r\n" +
                        "Facial Hair:  " + face.faceAttributes.facialHair.sideburns + "\r\n" +
                        "Head Pose: pitch "  + face.faceAttributes.headPose.pitch + "\r\n" +
                        "Head Pose: roll "  + face.faceAttributes.headPose.roll + "\r\n" +
                        "Head Pose: yaw "  + face.faceAttributes.headPose.yaw + "\r\n" + "\r\n";
                tv_info.setText(text);
            }
        }
        return bitmap;
    }

}
