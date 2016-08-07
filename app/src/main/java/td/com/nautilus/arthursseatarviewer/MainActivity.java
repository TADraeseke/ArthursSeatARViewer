package td.com.nautilus.arthursseatarviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    //Instantiation of Variables - Variables here are accessible class wide

    //Create a new instance of Camera and CameraView
    private Camera mCamera = null;
    private CameraView mCameraView = null;

    //Instantiate ImageViews
    ImageView trackTut = null;
    ImageView drawerTut = null;
    ImageView horizon = null;
    ImageView paths = null;
    ImageView contours = null;
    ImageView surfGeo = null;
    ImageView surfGeoAnn = null;
    ImageView veg = null;

    //Create a new instance of ListView and DrawerLayout
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;

    //Event handler variables
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    boolean tracking = false;
    //First time tracking toggle
    boolean firstTimeTrack = true;

    //Sensor fusion instantiation

    private SensorManager mSensorManager = null;
    //Angular speeds from gyro
    private float[] gyro = new float[3];
    //Rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
    //Orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
    //Magnetic field vector
    private float[] magnet = new float[3];
    //Accelerometer vector
    private float[] accel = new float[3];
    //Orientation angles from accelerometer and magnetometer
    private float[] accMagOrientation = new float[3];
    //Fused orientation angles
    private float[] fusedOrientation = new float[3];
    //Accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];

    //Rate of sensor reading
    public static final int TIME_CONSTANT = 10;
    //How much weight to give gyroscope readings over accelerometer and magnetometer
    //A value of 1.0f effectively removes any input from magnetometer and accelerometer
    public static final float FILTER_COEFFICIENT = 1.0f;
    private Timer fuseTimer = new Timer();

    //Instantiate SetPosition variables
    float initX = 0;
    float initY = 0;
    float initZ = 0;

    //User and Arthur's Seat Locs, bearingToArthur'sSeat
    private Location userLoc = new Location("");
    private Location arthursSeat = new Location("");
    double bearingToAS = 0;

    private Location nearPollock = new Location("");
    private Location library = new Location("");
    private Location RRSouth = new Location("");
    private Location DECircle = new Location("");

    HashMap<String, Location> listOfViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make window full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //Define Arthur's Seat Loc
        arthursSeat.setLatitude(55.944574);
        arthursSeat.setLongitude(-3.161836);

        //Define View locations
        nearPollock.setLatitude(55.941716);
        nearPollock.setLongitude(-3.170142);
        library.setLatitude(55.94260);
        library.setLongitude(-3.188406);
        RRSouth.setLatitude(55.94278);
        RRSouth.setLongitude(-3.16608);
        DECircle.setLatitude(55.94958);
        DECircle.setLongitude(-3.17440);

        //Add Views to list
        listOfViews.put("RRSouth", RRSouth);
        listOfViews.put("nearPollock", nearPollock);
        listOfViews.put("library", library);
        listOfViews.put("DECircle", DECircle);

        //Get GPS Position
        GPS gps = new GPS(getBaseContext());
        userLoc.setLatitude(gps.getLatitude());
        userLoc.setLongitude(gps.getLongitude());
        bearingToAS = bearing(userLoc, arthursSeat);

        //Get nearestView to user's initial position
        String nearestView = getNearestView(userLoc);

        //Initialize gyro variables
        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        //Initialize gyro Matrix variables
        gyroMatrix[0] = 1.0f;
        gyroMatrix[1] = 0.0f;
        gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f;
        gyroMatrix[4] = 1.0f;
        gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f;
        gyroMatrix[7] = 0.0f;
        gyroMatrix[8] = 1.0f;

        // Assign ImageViews
        trackTut = (ImageView) findViewById(R.id.trackTut);
        drawerTut = (ImageView) findViewById(R.id.drawerTut);
        horizon = (ImageView) findViewById(R.id.horizon);
        paths = (ImageView) findViewById(R.id.paths);
        contours = (ImageView) findViewById(R.id.contours);
        surfGeo = (ImageView) findViewById(R.id.surfGeo);
        surfGeoAnn = (ImageView) findViewById(R.id.surfGeoAnn);
        veg = (ImageView) findViewById(R.id.veg);


        //Download layers. First part of the DownloadFileFromUL call gives the name the file will be
        //stored as locally. Second part gives the associated URL. URL is partially formed by the
        //result of running the nearestView. ie if nearest view is determined to be the library,
        //nearestView will hold the value of string "library".
        new DownloadFileFromURL("TrackTut").execute("http://www.geos.ed.ac.uk/~s0571384/TrackTut.png");
        Toast.makeText(this, "Track tutorial layer downloaded.", Toast.LENGTH_SHORT).show();
        new DownloadFileFromURL("DrawerTut").execute("http://www.geos.ed.ac.uk/~s0571384/DrawerTut.png");
        Toast.makeText(this, "Drawer tutorial layer downloaded.", Toast.LENGTH_SHORT).show();
        new DownloadFileFromURL("Horizon").execute("http://www.geos.ed.ac.uk/~s0571384/" + nearestView + "/Horizon.png");
        Toast.makeText(this, "Horizon layer downloaded.", Toast.LENGTH_SHORT).show();
        new DownloadFileFromURL("Paths").execute("http://www.geos.ed.ac.uk/~s0571384/" + nearestView + "/Paths.png");
        Toast.makeText(this, "Path layer downloaded.", Toast.LENGTH_SHORT).show();
        new DownloadFileFromURL("Contours").execute("http://www.geos.ed.ac.uk/~s0571384/" + nearestView + "/Contours.png");
        Toast.makeText(this, "Contour layer downloaded.", Toast.LENGTH_SHORT).show();
        new DownloadFileFromURL("SurfGeo").execute("http://www.geos.ed.ac.uk/~s0571384/" + nearestView + "/SurfGeo.png");
        Toast.makeText(this, "Surface Geology layer downloaded.", Toast.LENGTH_SHORT).show();
        new DownloadFileFromURL("SurfGeoAnn").execute("http://www.geos.ed.ac.uk/~s0571384/" + nearestView + "/SurfGeoAnn.png");
        Toast.makeText(this, "Surface Geology Annotations layer downloaded.", Toast.LENGTH_SHORT).show();
        new DownloadFileFromURL("Veg").execute("http://www.geos.ed.ac.uk/~s0571384/" + nearestView + "/Veg.png");
        Toast.makeText(this, "Vegetation layer downloaded.", Toast.LENGTH_SHORT).show();

        //Load Image Tutorial and Horizon
        loadImageFromStorage("TrackTut.png", 0);

        loadImageFromStorage("Horizon.png", -2);

        // Open mCamera and set to cameraView
        try {
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e) {
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }
        if (mCamera != null) {
            //create a SurfaceView to show camera data
            mCameraView = new CameraView(this, mCamera);
            FrameLayout cameraView = (FrameLayout) findViewById(R.id.cameraView);
            //add the SurfaceView to the layout
            cameraView.addView(mCameraView);
        }

        //Draw exit button
        ImageButton imgClose = (ImageButton) findViewById(R.id.imgClose);
        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.exit(0);
            }
        });

        //Draw tracking button
        final Button trackButton = (Button) findViewById(R.id.trackButton);

        //Tracking button listener
        trackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Uses sensor data at time of button being pressed as init values for X,Y,Z
                if (tracking == false) {
                    tracking = true;
                    initX = (float) (((-1) * Math.toDegrees(fusedOrientation[0])) % 360);
                    initY = (float) ((-1) * Math.toDegrees(fusedOrientation[2]));
                    initZ = (float) ((-1) * Math.toDegrees(fusedOrientation[1]));
                    trackTut.setImageResource(android.R.color.transparent);
                    if (firstTimeTrack == true) {
                        loadImageFromStorage("DrawerTut.png", -1);
                        firstTimeTrack = false;
                    }
                    //Change text of tracking button
                    trackButton.setText("Tracking (ON)");
                } else {
                    tracking = false;
                    initX = 0;
                    initY = 0;
                    initZ = 0;
                    //Change text of tracking button
                    trackButton.setText("Tracking (OFF)");
                }
            }
        });


        //Define layerList
        mDrawerList = (ListView) findViewById(R.id.layerList);
        //Allow multiple selections
        mDrawerList.setChoiceMode(mDrawerList.CHOICE_MODE_MULTIPLE);
        //Define drawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        //Build drawer items as separate function
        addDrawerItems();
        //Setup drawer as separate function
        setupDrawer();

        //Instantiate sensorManager
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        //Instantiate event listener as separate function
        initSensorListeners();

        //After one second, start calculateFusedOrientationTask
        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                1000, TIME_CONSTANT);
    }

    //Start accelerometer, gyroscope and magnetometer sensors with fastest possible update speed
    public void initSensorListeners() {
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    //Function for handling changed state in sensors (called extremely often)
    //On each type of sensor change, update arrays, except for gyros
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            //Accelerometer data copied into array and used to update calculation
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;
            //On gyroscope change call gyroFunction
            case Sensor.TYPE_GYROSCOPE:
                gyroFunction(event);
                break;
            //Magnetometer data copied into array
            case Sensor.TYPE_MAGNETIC_FIELD:
                // copy new magnetometer data into magnet array
                System.arraycopy(event.values, 0, magnet, 0, 3);
                break;
        }
        //Each time a SensorChange is detected call setPosition
        setPosition();
    }

    @Override
    //Required function for SensorEventListener, though unused.
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //Function (Brute Force) which determines the closest viewing location and returns its name.
    //That name is used to form the URL for accessing the correct overlay data.
    public String getNearestView(Location currentLoc) {
        //Instantiate nearest to be null, distance to a very high value and currDist to 0
        String nearest = null;
        double distance = Double.MAX_VALUE;
        double currDist = 0;
        //Iterate through the list and evaluate if the distance between the user and each view is
        //less than the currently established shortest distance.
        for (String key : listOfViews.keySet()) {
            currDist = currentLoc.distanceTo(listOfViews.get(key));
            if (currDist < distance) {
                nearest = key;
                distance = currDist;
            }
        }
        //nearest is the String name of the nearest view rather than the location object. Since it
        //is used to form the view's URL.
        return nearest;
    }

    //On successful generation of RotationMatrix, get orientation from fusedData.
    public void calculateAccMagOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }


    public static final float EPSILON = 0.000000001f;

    private void getRotationVectorFromGyro(float[] gyroValues, float[] deltaRotationVector, float timeFactor) {
        float[] normValues = new float[3];

        //Calculate the angular speed of the sample
        float omegaMagnitude =
                (float) Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        //Normalize the rotation vector if greater than epsilon
        if (omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        //Integrate around this axis with the angular speed by the timestep
        //in order to get a delta rotation from this sample over the timestep
        //Converts the axis-angle representation of the delta rotation
        //into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;

    public void gyroFunction(SensorEvent event) {
        //On first call if accMagOrientation is null, return nothing
        if (accMagOrientation == null) {
            return;
        }

        //Instantiate gyroscope rotation matrix
        if (initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        //If this is not the first call to gyroFunction, copy new gyro values to the gyro array and
        //convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        //After measurement, save timestamp for next interval
        timestamp = event.timestamp;

        //Rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        //Perform matrix multiplication between gyroMatrix and the delta matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        //Get the gyroscope orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    //Trigonometric math to get rotation matrix from orientation matrix passed in
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float) Math.sin(o[1]);
        float cosX = (float) Math.cos(o[1]);
        float sinY = (float) Math.sin(o[2]);
        float cosY = (float) Math.cos(o[2]);
        float sinZ = (float) Math.sin(o[0]);
        float cosZ = (float) Math.cos(o[0]);

        //Rotation about x-axis (Pitch)
        xM[0] = 1.0f;
        xM[1] = 0.0f;
        xM[2] = 0.0f;
        xM[3] = 0.0f;
        xM[4] = cosX;
        xM[5] = sinX;
        xM[6] = 0.0f;
        xM[7] = -sinX;
        xM[8] = cosX;

        //Rotation about y-axis (Roll)
        yM[0] = cosY;
        yM[1] = 0.0f;
        yM[2] = sinY;
        yM[3] = 0.0f;
        yM[4] = 1.0f;
        yM[5] = 0.0f;
        yM[6] = -sinY;
        yM[7] = 0.0f;
        yM[8] = cosY;

        //Rotation about z-axis (Azimuth)
        zM[0] = cosZ;
        zM[1] = sinZ;
        zM[2] = 0.0f;
        zM[3] = -sinZ;
        zM[4] = cosZ;
        zM[5] = 0.0f;
        zM[6] = 0.0f;
        zM[7] = 0.0f;
        zM[8] = 1.0f;

        //Rotation order is y, x, z (Roll, Pitch, Azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    //Multiply the two matrices passed in and return the resulting matrix
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }


    //!!!Incomplete!!! Attempts to set an initial position for GI layers based on GPS coordinate and
    //subsequent bearing towards Arthur's Seat.
    //float initX = (float) bearingToAS;

    //Updates the position of GI layers based on input from the fused sensors.
    void setPosition() {
        //
        float XDegree = ((float) (Math.toDegrees(fusedOrientation[0])) % 360);
        float YDegree = (float) Math.toDegrees(fusedOrientation[2]) + 45;
        float ZDegree = (float) Math.toDegrees(fusedOrientation[1]);

        //Instantiate a new translateAnimation
        TranslateAnimation transAm;
        //Get width of screen
        float maxX = trackTut.getMeasuredWidth();
        float maxY = trackTut.getMeasuredHeight();

        //If tracking is on, translate GI layers based on input
        if (tracking) {
            AnimationSet anSet = new AnimationSet(true);
            anSet.setFillAfter(true);

            //Translate movement in X 0 - 360 degrees. Translate movement in Y 0 - 180 degrees
            transAm = new TranslateAnimation(
                    Animation.ABSOLUTE, 0,
                    Animation.ABSOLUTE, (maxX) - (((XDegree + (initX + 90)) * maxX) / 90),
                    Animation.ABSOLUTE, 0,
                    Animation.ABSOLUTE, (maxY) - (((YDegree + initY) * maxY)) / 45);

            //Interpolate movement between points linearly
            transAm.setInterpolator(new LinearInterpolator());
            //Translation persists after conclusion of animation
            transAm.setFillAfter(true);

            //Rotate from centre of layer to counter for the phone rotating on Z axis.
            RotateAnimation rotAm = new RotateAnimation(0, ZDegree, (maxX / 2), (maxY / 2));

            //Combine both translation and rotation into a single animation.
            anSet.addAnimation(transAm);
            anSet.addAnimation(rotAm);

            //Run animation on all layers.
            horizon.startAnimation(anSet);
            paths.startAnimation(anSet);
            contours.startAnimation(anSet);
            surfGeo.startAnimation(anSet);
            surfGeoAnn.startAnimation(anSet);
            veg.startAnimation(anSet);
        }
    }

    //Calculates degrees (0-360) bearing between two coordinates.
    protected static double bearing(Location user, Location target) {
        double lon1 = user.getLongitude();
        double lon2 = target.getLongitude();
        double lat1 = Math.toRadians(user.getLatitude());
        double lat2 = Math.toRadians(target.getLatitude());
        double lonDiff = Math.toRadians(lon2 - lon1);
        double y = Math.sin(lonDiff) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lonDiff);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }


    //Pulls image from local storage based on an input key and draws it based on layerNum
    private void loadImageFromStorage(String key, int layerNum) {
        //Default storage location for tested device, may vary by device.
        String root = "storage/emulated/0";

        try {
            File f = new File(root, key);
            System.out.println("Root is:" + root);
            System.out.println("Key is:" + key);
            //Decode the file named in key located in root folder.
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));

            //Set tutorial images
            if (layerNum == 0) {
                trackTut.setImageBitmap(b);
            }
            if (layerNum == -1) {
                drawerTut.setImageBitmap(b);
            }

            //Load Horizon Line
            if (layerNum == -2) {
                horizon.setImageBitmap(b);
                System.out.println("Loading horizon");
            } else if (layerNum != -1) { //Don't remove on second tutorial loading.
                horizon.setImageResource(android.R.color.transparent);
            }

            if (layerNum == 1) {//Paths layer
                paths.setImageBitmap(b);
                trackTut.setImageResource(android.R.color.transparent);
                drawerTut.setImageResource(android.R.color.transparent);
            } else if (layerNum == 2) {//Contour layer
                contours.setImageBitmap(b);
                trackTut.setImageResource(android.R.color.transparent);
                drawerTut.setImageResource(android.R.color.transparent);
            } else if (layerNum == 3) { //SurfGeo layer
                surfGeo.setImageBitmap(b);
                surfGeo.setAlpha(127);
                trackTut.setImageResource(android.R.color.transparent);
                drawerTut.setImageResource(android.R.color.transparent);
                //Filters SGFilter = new Filters();
                //Bitmap DistSG = SGFilter.barrel(b,10.0f);
                //surfGeo.setImageBitmap(DistSG);
            } else if (layerNum == 4) { //SurfaceGeo Annotation
                surfGeoAnn.setImageBitmap(b);
                trackTut.setImageResource(android.R.color.transparent);
                drawerTut.setImageResource(android.R.color.transparent);
            } else if (layerNum == 5) { //Vegetation
                veg.setImageBitmap(b);
                veg.setAlpha(127);
                trackTut.setImageResource(android.R.color.transparent);
                drawerTut.setImageResource(android.R.color.transparent);
            }
        }
        //Report error if file not found.
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Builds items for drawer and handles as setOnItemClickListener
    private void addDrawerItems() {
        final String[] layersArray = {"Paths", "Contours", "Surface Geology", "Vegetation"};
        mAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.checkbox, layersArray);
        mDrawerList.setAdapter(mAdapter);

        //On item click listener
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView lv = (ListView) parent;
                if (lv.isItemChecked(0)) { //Paths
                    loadImageFromStorage("Paths.png", 1);
                } else {
                    paths.setImageResource(android.R.color.transparent);
                }
                if (lv.isItemChecked(1)) { //Contours
                    loadImageFromStorage("Contours.png", 2);
                } else {
                    contours.setImageResource(android.R.color.transparent);
                }
                if (lv.isItemChecked(2)) { //Surface Geology
                    loadImageFromStorage("SurfGeo.png", 3);
                    loadImageFromStorage("SurfGeoAnn.png", 4);
                } else {
                    surfGeo.setImageResource(android.R.color.transparent);
                    surfGeoAnn.setImageResource(android.R.color.transparent);
                }
                if (lv.isItemChecked(3)) { //Vegetation
                    loadImageFromStorage("Veg.png", 5);
                } else {
                    veg.setImageResource(android.R.color.transparent);
                }
            }
        });
    }

    //Sets up drawer state
    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawerOpen, R.string.drawerClose) {

            //Called when drawer opened
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            //Called when drawer closed
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }
        };
        //Set mDrawerToggle to true
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        //Set mDrawerLayout listener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    //Fuses orientation from gyroscope and accelerometer/magnetometer
    class calculateFusedOrientationTask extends TimerTask {

        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
            fusedOrientation[0] =
                    FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];

            fusedOrientation[1] =
                    FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];

            fusedOrientation[2] =
                    FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];

            //Overwrite gyro matrix and orientation with fused orientation to compensate for drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
        }
    }
}