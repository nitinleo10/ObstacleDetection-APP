package com.example.obstacledetection;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String MIME_TYPE_IMAGE = "image/*";
    private ActivityResultLauncher<String> imagePickerLauncher;
    private static final double K1 = 0.01;
    private static final double K2 = 0.03;
    Bitmap image1;
    Bitmap image2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView1 = findViewById(R.id.imageView1);
        ImageView imageView2 = findViewById(R.id.imageView2);
        Button pickimg1 = findViewById(R.id.button1);
        Button pickimg2 = findViewById(R.id.button2);
        Button compare = findViewById(R.id.compare);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        if (result != null) {
                            Bitmap bitmap = getBitmapFromUri(result);
                            if (bitmap != null) {
                                if (imageView1.getTag() == null) {
                                    image1 = bitmap;
                                    imageView1.setImageBitmap(bitmap);
                                    imageView1.setTag("picked");
                                } else {
                                    image2 = bitmap;
                                    imageView2.setImageBitmap(bitmap);
                                    imageView2.setTag("picked");
                                }
                            }
                        }
                    }
                }
        );

        pickimg1.setOnClickListener(view -> openGallery());
        pickimg2.setOnClickListener(view -> openGallery());
        compare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                perform();
            }
        });

    }

    private void perform() {
        if (image1 != null && image2 != null) {
            int diff = compareImages(image1, image2);
            if(diff>5) {
                Toast.makeText(this, "Obstacle Detected", Toast.LENGTH_SHORT).show();
                sendSms(diff);
            }
            else
                Toast.makeText(this, "Obstacle not Detected", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please select both images.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        // Launch the image picker using the ActivityResultLauncher
        imagePickerLauncher.launch(MIME_TYPE_IMAGE);
    }
    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            // Use the content resolver to open the input stream and decode the bitmap
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendSms(int diff) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage("+918917655456", null, "The differnce between images is(in %): "+Integer.toString(diff), null, null);
    }
    private int compareImages(Bitmap image1, Bitmap image2) {
        if (image1 == null || image2 == null) {
            throw new IllegalArgumentException("Bitmaps cannot be null");
        }

        int width = image1.getWidth();
        int height = image1.getHeight();

        if (width != image2.getWidth() || height != image2.getHeight()) {
            throw new IllegalArgumentException("Images must have the same dimensions");
        }

        double sum = 0.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel1 = image1.getPixel(x, y);
                int pixel2 = image2.getPixel(x, y);

                double red1 = Color.red(pixel1) / 255.0;
                double green1 = Color.green(pixel1) / 255.0;
                double blue1 = Color.blue(pixel1) / 255.0;

                double red2 = Color.red(pixel2) / 255.0;
                double green2 = Color.green(pixel2) / 255.0;
                double blue2 = Color.blue(pixel2) / 255.0;

                double luma1 = 0.299 * red1 + 0.587 * green1 + 0.114 * blue1;
                double luma2 = 0.299 * red2 + 0.587 * green2 + 0.114 * blue2;

                double variance1 = (0.299 * red1 - luma1) * (0.299 * red1 - luma1)
                        + (0.587 * green1 - luma1) * (0.587 * green1 - luma1)
                        + (0.114 * blue1 - luma1) * (0.114 * blue1 - luma1);

                double variance2 = (0.299 * red2 - luma2) * (0.299 * red2 - luma2)
                        + (0.587 * green2 - luma2) * (0.587 * green2 - luma2)
                        + (0.114 * blue2 - luma2) * (0.114 * blue2 - luma2);

                double covariance = (0.299 * red1 - luma1) * (0.299 * red2 - luma2)
                        + (0.587 * green1 - luma1) * (0.587 * green2 - luma2)
                        + (0.114 * blue1 - luma1) * (0.114 * blue2 - luma2);

                double ssim = ((2 * luma1 * luma2 + K1) * (2 * covariance + K2))
                        / ((luma1 * luma1 + luma2 * luma2 + K1) * (variance1 + variance2 + K2));

                sum += ssim;
            }
        }

        double ssimAvg = sum / (height*width);
        // Compute the percentage difference

        return (int) (100.0 * (1.0 - ssimAvg));
    }

}