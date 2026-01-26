package com.geometrics.app

import android.os.Build

class Constants {

    companion object {
        // For Android emulator, use local network IP to access host machine
        // For real device, use the actual IP address of your backend server
        // You can detect emulator using Build.FINGERPRINT.contains("generic")
        private val isEmulator: Boolean
            get() = Build.FINGERPRINT.contains("generic") 
                    || Build.FINGERPRINT.contains("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || "google_sdk" == Build.PRODUCT

        val BACKEND_URL: String
            get() = if (isEmulator) {
                "http://192.168.1.11:3001"
            } else {
                "http://192.168.1.11:3001"
            }

        val MQTT_BROKER_HOST: String
            get() = if (isEmulator) {
                "192.168.1.11"
            } else {
                "192.168.1.11"
            }

        const val MQTT_BROKER_PORT = 1883
        const val MQTT_TOPIC = "incidents/nearby"
        const val NEARBY_RADIUS_KM = 5.0
    }
}
