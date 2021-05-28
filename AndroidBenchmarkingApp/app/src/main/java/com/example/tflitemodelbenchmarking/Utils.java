package com.example.tflitemodelbenchmarking;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Utils {
  private static final String TAG = "Utils";
  private static final String XRAY_DIR = "xrayImages";

  public static MappedByteBuffer loadTfliteModel(Context context, String modelName) throws IOException {
    Log.v(TAG, "modelname: " + modelName);
    AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(modelName);
    FileInputStream fileInputStream =
      new FileInputStream(assetFileDescriptor.getFileDescriptor());
    FileChannel fileChannel = fileInputStream.getChannel();
    long startoffset = assetFileDescriptor.getStartOffset();
    long declaredLength = assetFileDescriptor.getDeclaredLength();
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startoffset, declaredLength);
//    return fileChannel.map(FileChannel.MapMode.READ_WRITE, startoffset, declaredLength);
  }

  /**
   * This method loads an image from assets, converts the bitmap to an int[], and then converts that
   * to a float[][][][] to pass into the model. Each value in the int[] is separated into 3 R,G,B
   * values.
   *
   * @param fileName this image can be any size, will be resized to 224x224
   *
   * @return float[1][224][224][3]
   * **/
  public static float[][][][] loadImageAsFloatArr(Context context, String fileName) {
//    Log.v(TAG, "Loading fileName: " + fileName);
    Bitmap bitmap = Utils.getBitmapFromAsset(context, XRAY_DIR + "/" + fileName);
    bitmap = resizeBitmap(bitmap, 224, 224);

    int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
    // Get all pixels and store in int array
    bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

    float[][][][] floatArray = new float[1][224][224][3];
    // Convert hexadecimal int array to RGB float array
    for (int i = 0; i < intArray.length; i++) {
      int pixel = intArray[i];
      float red = Color.red(pixel) / 255.0F;
      float green = Color.green(pixel) / 255.0F;
      float blue = Color.blue(pixel) / 255.0F;

      int xIndex = Utils.convert1Dto2D_x(i, bitmap.getWidth());
      int yIndex = Utils.convert1Dto2D_y(i, bitmap.getWidth());
      floatArray[0][xIndex][yIndex][0] = red;
      floatArray[0][xIndex][yIndex][1] = green;
      floatArray[0][xIndex][yIndex][2] = blue;
    }
    bitmap.recycle();

    return floatArray;
  }

  public static byte[][][][] loadImageAsByteArr(Context context, String fileName) {
    Log.v(TAG, "Loading fileName: " + fileName);
    Bitmap bitmap = Utils.getBitmapFromAsset(context, XRAY_DIR + "/" + fileName);
    bitmap = resizeBitmap(bitmap, 224, 224);

    int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
    // Get all pixels and store in int array
    bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

    byte[][][][] byteArray = new byte[1][224][224][3];
    // Convert hexadecimal int array to RGB float array
    for (int i = 0; i < intArray.length; i++) {
      int pixel = intArray[i];
      byte red = ((byte) Color.red(pixel));
      byte green = ((byte) Color.green(pixel));
      byte blue = ((byte) Color.blue(pixel));

      int xIndex = Utils.convert1Dto2D_x(i, bitmap.getWidth());
      int yIndex = Utils.convert1Dto2D_y(i, bitmap.getWidth());
      byteArray[0][xIndex][yIndex][0] = red;
      byteArray[0][xIndex][yIndex][1] = green;
      byteArray[0][xIndex][yIndex][2] = blue;
    }
    bitmap.recycle();

    return byteArray;
  }

  public static Bitmap getBitmapFromAsset(Context context, String filePath) {
    AssetManager assetManager = context.getAssets();
    InputStream istr;
    Bitmap bitmap = null;
    try {
      istr = assetManager.open(filePath);
      bitmap = BitmapFactory.decodeStream(istr);
    } catch (IOException e) {
      Log.e(TAG, "Could not decode stream from " + filePath);
    }
    return bitmap;
  }

  public static Bitmap resizeBitmap(Bitmap bm, int newHeight, int newWidth) {
    int width = bm.getWidth();
    int height = bm.getHeight();
    float scaleWidth = ((float) newWidth) / width;
    float scaleHeight = ((float) newHeight) / height;

    // Create a matrix for the manipulation
    Matrix matrix = new Matrix();

    // Resize the bit map
    matrix.postScale(scaleWidth, scaleHeight);

    // Recreate the new Bitmap
    Bitmap resized = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    bm.recycle();
    return resized;
  }

  public static int convert1Dto2D_x(int pix1DIndex, int width) {
    return pix1DIndex / width;
  }

  public static int convert1Dto2D_y(int pix1DIndex, int width) {
    return pix1DIndex % width;
  }

  /**
   * From model output, which is a float[14] array, get the index with the highest probability.
   **/
  public static int getIndexOfLargestFloat(float[] array) {
    if (array == null || array.length == 0) {
      return -1;
    }
    int largest = 0;
    for (int i = 1; i < array.length; i++) {
      if (array[i] > array[largest]) {
        largest = i;
      }
    }
    return largest; // if duplicates, position of the first largest found
  }

  /**
   * From model output, which is an int[14] array, get the index with the highest probability.
   **/
  public static int getIndexOfLargestByte(byte[] array) {
    if (array == null || array.length == 0) {
      return -1;
    }
    int largest = 0;
    for (int i = 1; i < array.length; i++) {
      if (array[i] > array[largest]) {
        largest = i;
      }
    }
    return largest; // if duplicates, position of the first largest found
  }
}
