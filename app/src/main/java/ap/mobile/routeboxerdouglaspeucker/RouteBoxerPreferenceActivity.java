package ap.mobile.routeboxerdouglaspeucker;

import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class RouteBoxerPreferenceActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        this.getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new RouteBoxerPreferenceFragment())
                .commit();
    }
}
