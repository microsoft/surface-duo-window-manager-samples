package com.example.travelplanner

import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
    }

    override fun onResume() {
        super.onResume()
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            permissionsOK()
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissionsOK()
        }
    }

    private fun permissionsOK() {
        if (!checkNetwork()) {
            AlertDialog.Builder(this).setTitle(R.string.text_no_internet_title).setMessage(R.string.text_no_internet_message)
                .setPositiveButton(resources.getString(R.string.dialog_yes)) { _, _ -> permissionsOK() }
                .show()
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun checkNetwork(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network: Network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities: NetworkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
//
//    inner class NetworkCallback: ConnectivityManager.NetworkCallback() {
//        override fun onUnavailable() {
//            super.onUnavailable()
//        }
//    }
}
