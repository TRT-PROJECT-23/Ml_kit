package com.google.mlkit.vision.demo.java;

import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.StrictMode;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.google.mlkit.vision.demo.BuildConfig;
import com.google.mlkit.vision.demo.R;

/** Demo app chooser which allows you pick from all available testing Activities. */
public final class ChooserActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
  private static final String TAG = "ChooserActivity";

  @SuppressWarnings("NewApi") // CameraX is only available on API 21+
  private static final Class<?>[] CLASSES =
          new Class<?>[]{
                  StillImageActivity.class,
          };

  private static final int[] DESCRIPTION_IDS =
          VERSION.SDK_INT < VERSION_CODES.LOLLIPOP
                  ? new int[]{
                  R.string.desc_camera_source_activity, R.string.desc_still_image_activity,
          }
                  : new int[]{
                  R.string.desc_camera_source_activity,
                  R.string.desc_still_image_activity,
                  R.string.desc_camerax_live_preview_activity,
                  R.string.desc_cameraxsource_demo_activity,
          };

  // Add custom names array to hold the names to be displayed in the list
  private static final String[] CUSTOM_NAMES =
          new String[]{
                  "Blood Glucose",
                  // Add custom names for additional activities if needed
          };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (BuildConfig.DEBUG) {
      StrictMode.setThreadPolicy(
              new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
      StrictMode.setVmPolicy(
              new StrictMode.VmPolicy.Builder()
                      .detectLeakedSqlLiteObjects()
                      .detectLeakedClosableObjects()
                      .penaltyLog()
                      .build());
    }
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");

    setContentView(R.layout.activity_chooser);

    // Set up ListView and Adapter
    ListView listView = findViewById(R.id.test_activity_list_view);

    MyArrayAdapter adapter = new MyArrayAdapter(this, android.R.layout.simple_list_item_2, CLASSES);
    adapter.setDescriptionIds(DESCRIPTION_IDS);

    // Set custom names in the adapter
    adapter.setCustomNames(CUSTOM_NAMES);

    listView.setAdapter(adapter);
    listView.setOnItemClickListener(this);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    Class<?> clicked = CLASSES[position];
    startActivity(new Intent(this, clicked));
  }

  private static class MyArrayAdapter extends ArrayAdapter<Class<?>> {
    private final Context context;
    private final Class<?>[] classes;
    private int[] descriptionIds;
    private String[] customNames;

    MyArrayAdapter(Context context, int resource, Class<?>[] objects) {
      super(context, resource, objects);

      this.context = context;
      classes = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View view = convertView;

      if (convertView == null) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(android.R.layout.simple_list_item_2, null);
      }

      // Get the display name: custom name or default class name
      String displayName = (customNames != null && customNames.length > position)
              ? customNames[position]
              : classes[position].getSimpleName();

      ((TextView) view.findViewById(android.R.id.text1)).setText(displayName);
      ((TextView) view.findViewById(android.R.id.text2)).setText(descriptionIds[position]);

      return view;
    }

    void setDescriptionIds(int[] descriptionIds) {
      this.descriptionIds = descriptionIds;
    }

    // Method to set custom names
    void setCustomNames(String[] customNames) {
      this.customNames = customNames;
    }
  }
}
