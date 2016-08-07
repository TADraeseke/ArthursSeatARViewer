package td.com.nautilus.arthursseatarviewer;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

//Defines the CameraView as a class
public class CameraView extends SurfaceView implements SurfaceHolder.Callback{

    private SurfaceHolder SurfHolder;
    private Camera camera;

    public CameraView(Context context, Camera inCamera){
        //Instantiate CameraView
        super(context);
        camera = inCamera;
        //Set display to horizontal
        camera.setDisplayOrientation(0);
        //Get holder and set class as callback
        SurfHolder = getHolder();
        SurfHolder.addCallback(this);
        SurfHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    @Override
    //On surfaceCreated, set camera to draw to surfaceHolder
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try{
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException error) {
            //On error, report it
            Log.d("ERROR", "Camera error in surfaceCreated: " + error.getMessage());
        }
    }

    @Override
    //On surface change, stop the camera feed, rotate and then restart
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

        //If surface is null
        if(SurfHolder.getSurface() == null) {
            //Stop camera preview
            try {
                camera.stopPreview();
            } catch (Exception error) {
                Log.d("ERROR", "Camera error in surfaceChanged: " + error.getMessage());
            }

            //Start camera preview
            try {
                camera.setPreviewDisplay(SurfHolder);
                camera.startPreview();
            } catch (IOException error) {
                Log.d("ERROR", "Camera error on surfaceChanged " + error.getMessage());
            }
        }
    }

    @Override
    // On surfaceDestroyed stop camera
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        camera.stopPreview();
        camera.release();
    }
}