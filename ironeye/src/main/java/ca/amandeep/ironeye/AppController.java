package ca.amandeep.ironeye;

import android.app.Application;

import com.google.android.gms.plus.model.people.Person;

public class AppController extends Application {

    /**
     * A singleton instance of the application class for easy access in other places
     */
    private static AppController sInstance;
    public Person currentPerson;

    /**
     * @return ApplicationController singleton instance
     */
    public static synchronized AppController getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // initialize the singleton
        sInstance = this;
    }
}
