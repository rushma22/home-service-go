package lk.rush.homeservicego.utils;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeDetector implements SensorEventListener {

    // Minimum g-force acceleration required to count as a shake
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.5f;
    // Cooldown time in milliseconds between shake events to prevent multiple triggers
    private static final int SHAKE_SLOP_TIME_MS = 1500;

    private OnShakeListener mListener;
    private long mShakeTimestamp;

    public interface OnShakeListener {
        void onShake();
    }

    public void setOnShakeListener(OnShakeListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for simple shake detection
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mListener != null && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            // gForce will be approx 1.0 when the device is stationary
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                final long now = System.currentTimeMillis();
                // Ignore shake events that happen too close together
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return;
                }

                mShakeTimestamp = now;
                mListener.onShake();
            }
        }
    }
}
