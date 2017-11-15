package com.sysfore.camerademo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;

/**
 * Created by Shyam on 6/20/2017.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MakePhotoActivity extends AppCompatActivity {

    TextView HDR;
    boolean FLAG = false;

    private static final String TAG = "AndroidCameraApi";
    private Button takePictureButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

//    Camera Timer

    private final static int DELAY = 3000;
    private final Handler handler = new Handler();
    private final Timer timer = new Timer();

//    Beeper

    final private ToneGenerator beeper =
            new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.8F);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HDR = (TextView) findViewById(R.id.hdr);
//Remove title bar
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;


        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                v.startAnimation(buttonClick);
                takePicture();

//                CallAutomaticPanohead();


            }

        });
        HDR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FLAG == false) {
                    FLAG = true;
                    HDR.setTypeface(null, Typeface.BOLD);
                    Toast.makeText(MakePhotoActivity.this, "HDR Enabled", Toast.LENGTH_SHORT).show();

                } else
                    FLAG = false;
//                    HDR.setTypeface(null, Typeface.NORMAL);
            }
        });
    }

    private void CallAutomaticPanohead() {

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 4; i++) {

                        if (i == 0) {
//                            takePicture();
//                            beeper.startTone(ToneGenerator.TONE_PROP_ACK);
//                            Toast.makeText(MakePhotoActivity.this, "Starting Capture...", Toast.LENGTH_SHORT).show();
                            sleep(2000);
                        } else if (i >= 1 && i <= 3) {

                            sleep(3000);
                            takePicture();
                            sleep(100);

//                            Toast.makeText(MakePhotoActivity.this, "Starting Capture...", Toast.LENGTH_SHORT).show();
                        }
                    }

//                    takePictureButton.setEnabled(true);


                } catch (InterruptedException ex) {
                    Log.i("error", "thread");
                }

            }


        };
        thread.start();
//        takePictureButton.setEnabled(true);
    }

//--------------------------------------- takepicture() function  -----------------------------------

//    int CONTROL_AF_MODE_CONTINUOUS_PICTURE = 11;

    protected void takePicture() {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 4096;
            int height = 2304;

            int len = jpegSizes.length;

            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[len - 2].getWidth();
                height = jpegSizes[len - 2].getHeight();

                Log.e(TAG, " length = " + len);

                Log.e(TAG, width + " and  " + height);
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);

            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(reader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            /**HDR*/
            if (FLAG == true) {
                captureBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.CONTROL_SCENE_MODE_HDR);

            }
            /**Settings for Flash*/
//            captureBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);

            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);


//            captureBuilder.set(CaptureRequest.EDGE_MODE,
//                    CaptureRequest.EDGE_MODE_OFF);

//            captureBuilder.set(
//                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
//                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
//
//            captureBuilder.set(
//                    CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,
//                    CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF);
//
//            captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,
//                    CaptureRequest.NOISE_REDUCTION_MODE_OFF);

//            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
//                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);

//            captureBuilder.set(CaptureRequest.CONTROL_AE_LOCK, true);
//            captureBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, true);


            //Set the JPEG quality here like so
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 90);

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

//            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                    CaptureRequest.CONTROL_AF_MODE_AUTO);


            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));


//
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/" + "IMG_" + timeStamp + ".jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
//                        image = reader.acquireLatestImage();
                        image = reader.acquireNextImage();


                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
//                    Toast.makeText(MakePhotoActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    beeper.startTone(ToneGenerator.TONE_PROP_ACK);

                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(MakePhotoActivity.this, "Image Saved", Toast.LENGTH_SHORT).show();
//            Toast.makeText(MakePhotoActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();

//            unlockFocus();
            createCameraPreview();
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MakePhotoActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MakePhotoActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MakePhotoActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
}


//--------------------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------------------
//    private final TimerTask task1 = new TimerTask() {
//        private int counter = 0;
//
//        public void run() {
//            handler.post(new Runnable() {
//                public void run() {
//
//
//                    if (counter == 0) {
//                        try {
//                            Thread.sleep(3000);
//                            Toast.makeText(MakePhotoActivity.this, "Count: " + counter, Toast.LENGTH_SHORT).show();
//
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    if (counter >= 1 && counter <= 5) {
//                        try {
//                            Log.e(String.valueOf(counter), "Ring1");
//                            takePicture();
//                            Thread.sleep(3000);    //miliseconds
//
//                            beeper.startTone(ToneGenerator.TONE_PROP_ACK);
//                            Toast.makeText(MakePhotoActivity.this, "Count: " + counter, Toast.LENGTH_SHORT).show();
//
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    if (counter == 13) {
//                        try {
//                            Thread.sleep(3000);
//                            Log.e(String.valueOf(counter), "Servoup");
//                            Toast.makeText(MakePhotoActivity.this, "dont take here", Toast.LENGTH_SHORT).show();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    if (counter >= 14 && counter <= 21) {
//                        try {
//                            Thread.sleep(3000);    //miliseconds
//                            Log.e(String.valueOf(counter), "Ring2");
//                            takePicture();
//                            beeper.startTone(ToneGenerator.TONE_PROP_ACK);
//                            Toast.makeText(MakePhotoActivity.this, "Count: " + counter, Toast.LENGTH_SHORT).show();
////                        Thread.sleep(1000);
//
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//
//                    if (counter == 22) {
//                        try {
//                            Thread.sleep(3000);
//                            Log.e(String.valueOf(counter), "Servodown");
////                        Toast.makeText(MakePhotoActivity.this, "dont take here", Toast.LENGTH_SHORT).show();
//                            Toast.makeText(MakePhotoActivity.this, "Count: " + counter, Toast.LENGTH_SHORT).show();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    if (counter >= 23 && counter <= 30) {
//                        try {
////                        Toast.makeText(MakePhotoActivity.this, "Panohead"+counter, Toast.LENGTH_SHORT).show();
//                            Thread.sleep(3000);    //miliseconds
//                            Log.e(String.valueOf(counter), "Ring3");
//                            takePicture();
//                            beeper.startTone(ToneGenerator.TONE_PROP_ACK);
//                            Toast.makeText(MakePhotoActivity.this, "Count: " + counter, Toast.LENGTH_SHORT).show();
////                        Thread.sleep(1000);
//
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    if (counter == 6) {
//                        try {
//                            Thread.sleep(3000);
//                            Log.e(String.valueOf(counter), "Servoinitial");
////                        Toast.makeText(MakePhotoActivity.this, "dont take here", Toast.LENGTH_SHORT).show();
//                            Toast.makeText(MakePhotoActivity.this, "Count: " + counter, Toast.LENGTH_SHORT).show();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        timer.cancel();
//                    }
//
//                }
//            });
//
//            counter++;
//        }
//    };
//--------------------------------------------------------------------------------------------------

