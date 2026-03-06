package com.bytecrack

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.bytecrack.di.MusicLifecycleEntryPoint
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.games.PlayGamesSdk
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors

@HiltAndroidApp
class ByteCrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PlayGamesSdk.initialize(this)
        MobileAds.initialize(this) {}

        val musicManager = EntryPointAccessors.fromApplication(this, MusicLifecycleEntryPoint::class.java).musicManager()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                musicManager.pause()
            }
            override fun onStart(owner: LifecycleOwner) {
                musicManager.resume()
            }
        })
    }
}
