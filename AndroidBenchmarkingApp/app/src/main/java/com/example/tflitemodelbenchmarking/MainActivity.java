package com.example.tflitemodelbenchmarking;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.PowerManager;
import android.os.SystemClock;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.TextView;

import java.io.IOException;

import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.gpu.CompatibilityList;
//import org.tensorflow.lite.gpu.GpuDelegate;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "TFliteModelBenchmarking";
  private static final String XRAY_DIR = "xrayImages";
  private static final int toastDuration = Toast.LENGTH_SHORT;
  private static PowerManager.WakeLock wakeLock;
  private static String[] labels = {"Atelectasis", "Cardiomegaly", "Effusion", "Infiltration",
    "Mass", "Nodule", "Pneumonia", "Pneumothorax", "Consolidation", "Edema", "Emphysema",
    "Fibrosis", "Pleural_Thickening", "Hernia"};
  private int runCount = 1;

  // Initialize interpreter with GPU delegate
  Interpreter.Options options = new Interpreter.Options();
//  CompatibilityList compatList = new CompatibilityList();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
      "Benchmarking::RunPerformanceTest");
  }

  public void run_all(View view) throws IOException {
    // Reset text if necessary
    TextView resultsBox = (TextView) findViewById(R.id.resultsView);
    resultsBox.setText(R.string.results_will_appear_here_avg_time_img);
    resultsBox.append("\nTest #" + runCount++);

    int numIterations = 1; // Increase this to get a more accurate result
    runTestNTimes("chexnet_kaggle_tflite_no_quantization.tflite", numIterations, MODEL_TYPE.FLOAT_MODEL, "Baseline");
    runTestNTimes("QAT_int_8_v1.tflite", numIterations, MODEL_TYPE.FLOAT_MODEL, "QAT Int8");
    runTestNTimes("chexnet_kaggle_tflite_dynamic_quantization.tflite", numIterations, MODEL_TYPE.FLOAT_MODEL, "DynQuant");
    runTestNTimes("chexnet_kaggle_tflite_fp16.tflite", numIterations, MODEL_TYPE.FLOAT_MODEL, "FP16");

    Log.d(TAG, "Done with all tests.");
  }

  public float runTestNTimes(String modelName, int numIterations, MODEL_TYPE modelType, String label) throws IOException {
    wakeLock.acquire(10*60*1000L /*10 minutes*/);
    Log.d(TAG, "Running test for " + modelName + " " + numIterations + " times.");

    float testStartTimeMs = SystemClock.uptimeMillis();
    float testTime = 0.0f;
    float avgTimePerImage;
    float totalTestTime;
    float minLatency = Float.MAX_VALUE;
    float maxLatency = Float.MIN_VALUE;

    String[] xRayList = getAssets().list(XRAY_DIR);
    int numImages = xRayList.length;

    if (xRayList.length == 0) {
      Log.e(TAG, "No xray images found. Tests not run.");
      return Float.NaN;
    } else {
      for (int i = 0; i < numIterations; i++) {
        // Run test on all images in directory
        for (String xray : xRayList) {
          Log.v(TAG, "Running inference on image: " + xray);
          // Pass image into model, time the inference, and close the model
          float runTime = timeInference(modelName, xray, modelType);
          testTime += runTime;
          minLatency = Math.min(minLatency, runTime);
          maxLatency = Math.max(maxLatency, runTime);
        }
      }

      // Log stats
      float endTimeMs = SystemClock.uptimeMillis();
      avgTimePerImage = testTime / (numImages * numIterations);
      totalTestTime = endTimeMs - testStartTimeMs;
      Log.d(TAG, "Completed test of " + label + " Model in " + totalTestTime + " ms on " + (numImages * numIterations) + " images.");
      Log.d(TAG, "Average inference time per image: " + avgTimePerImage + " for model: " + modelName);
      Log.d(TAG, "Min latency: " + minLatency + " ms.");
      Log.d(TAG, "Max latency: " + maxLatency + " ms.");
    }
    wakeLock.release();
    TextView resultsBox = (TextView) findViewById(R.id.resultsView);
    resultsBox.append("\n" + label + ": " + avgTimePerImage + " ms; " + totalTestTime + " total" + "\n");
    return avgTimePerImage;
  }

  private float timeInference(String modelName, String image, MODEL_TYPE modelType) throws IOException {
//    // Set interpreter options for CPU/GPU
//    if(compatList.isDelegateSupportedOnThisDevice()){
//      // if the device has a supported GPU, add the GPU delegate
//      GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
//      GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
//      options.addDelegate(gpuDelegate);
//      Log.d(TAG, "Delegate IS supported on this device");
//    } else {
      // if the GPU is not supported, run on 4 threads
      options.setNumThreads(2);
//      Log.d(TAG, "Delegate is NOT supported on this device");
//    }

    // Initialize interpreter and load model
    Interpreter interpreter;
    try {
      interpreter =
        new Interpreter(Utils.loadTfliteModel(getApplicationContext(), modelName), options);
    } catch (Exception e) {
      Log.e(TAG, "Creating interpreter FAILED with msg: " + e.getMessage());
      return -1.0f;
    }
//    Log.v(TAG, "Model loaded: " + modelName);

    // Run model, and time inference
    float time = SystemClock.uptimeMillis();
    if (modelType == MODEL_TYPE.INT_MODEL) {
      byte[][][][] inputByte = Utils.loadImageAsByteArr(getApplicationContext(), image);
      byte[][] outputByte = new byte[1][14];
//      interpreter.resizeInput(0, [4, 1, 224, 224, 3]);
      interpreter.run(inputByte, outputByte);
//      Log.v(TAG, "Output: " + Arrays.toString(outputByte[0]));
//      Log.v(TAG, "Likeliest label: " + labels[Utils.getIndexOfLargestByte(outputByte[0])]);
    } else {
      float[][][][] inputFloat = Utils.loadImageAsFloatArr(getApplicationContext(), image);
      float[][] outputFloat = new float[1][14];
      interpreter.run(inputFloat, outputFloat);
//      Log.v(TAG, "Output: " + Arrays.toString(outputFloat[0]));
//      Log.v(TAG, "Likeliest label: " + labels[Utils.getIndexOfLargestFloat(outputFloat[0])]);
    }

    // Log inference result and clean up
    float runTimeMs = SystemClock.uptimeMillis() - time;
    interpreter.close();
    Log.v(TAG, "Inference done in " + runTimeMs + " ms.");

    return runTimeMs;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}

enum MODEL_TYPE {FLOAT_MODEL, INT_MODEL};

