package com.mirolovic.zoran.estimoprox;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;


import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.estimote.sdk.repackaged.okhttp_v2_2_0.com.squareup.okhttp.internal.Util;
import com.orhanobut.hawk.Hawk;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private BeaconManager mBeaconMenager;



    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rig", null, null, null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Hawk.init(getApplicationContext());


        //boot beacnon menager ..... and config this is the main activity...
        mBeaconMenager = new BeaconManager(MainActivity.this);
        mBeaconMenager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);
        //using rxjava.... Observables.......
        mBeaconMenager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
                Log.d(TAG, "Ranged beacons: " + beacons);
                Observable.from(beacons)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                                //not intrested in errors ignoring them....
                        .filter(new Func1<Beacon, Boolean>() {
                            @Override
                            public Boolean call(Beacon beacon) {
                                Utils.Proximity prox = Hawk.get(beacon.getMajor() + "");
                                Utils.Proximity proxComper = Utils.computeProximity(beacon);
                                if (prox == proxComper) {
                                    Log.d("State is", "state");
                                    return false;
                                } else {
                                    Log.d("other state ", "other state");
                                    return true;
                                }

                            }
                        })
                        .subscribe(new Action1<Beacon>() {
                            @Override
                            public void call(Beacon beacon) {
                                //start range function....
                                double distance = Math.min(Utils.computeAccuracy(beacon), 20.0);
                                Log.d("distance is ----->", distance + "");
                                Utils.Proximity proxComper = Utils.computeProximity(beacon);
                                if(proxComper== Utils.Proximity.NEAR){
                                    //make web reqest in...
                                    Log.d(TAG, "entered in region ");
                                }else if(proxComper == Utils.Proximity.FAR){
                                    //make web reqest out...
                                    Log.d(TAG, "exiting in region");
                                }else if(proxComper == Utils.Proximity.UNKNOWN){
                                    Log.d(TAG, "poor soul is lost");
                                }

                                //before snake eat its tail set up trigers for far near etc...
                                Utils.Proximity prox = Utils.computeProximity(beacon);
                                Hawk.put(beacon.getMajor() + "", prox);
                            }
                        });


            }
        });



        //butstrepaj service....
        mBeaconMenager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    mBeaconMenager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (RemoteException e) {
                    Log.e(TAG, "Cannot start ranging", e);
                }
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();

        // Check if device supports Bluetooth Low Energy.
        if (!mBeaconMenager.hasBluetooth()) {
            Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
            return;
        }
    }
}
