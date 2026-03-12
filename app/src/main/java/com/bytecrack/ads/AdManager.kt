package com.bytecrack.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona Interstitial y Rewarded ads de AdMob.
 * Usa IDs de test durante desarrollo. Reemplazar en producción.
 */
@Singleton
class AdManager @Inject constructor() {

    companion object {
        private const val TAG_REWARDED = "ByteCrack.RewardedAd"
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    @Volatile
    private var interstitialAd: InterstitialAd? = null

    @Volatile
    private var rewardedAd: RewardedAd? = null

    fun loadInterstitial(activity: Activity, onLoaded: (() -> Unit)? = null) {
        InterstitialAd.load(
            activity,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        onDismissed: () -> Unit = {}
    ): Boolean {
        val ad = interstitialAd ?: return false
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onDismissed()
                loadInterstitial(activity)
            }
        }
        ad.show(activity)
        return true
    }

    fun loadRewarded(activity: Activity, onLoaded: (() -> Unit)? = null, onFailed: (() -> Unit)? = null) {
        RewardedAd.load(
            activity,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG_REWARDED, "Rewarded ad failed to load: code=${error.code} domain=${error.domain} message=${error.message}")
                    onFailed?.invoke()
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onDismissed: () -> Unit = {}
    ): Boolean {
        val ad = rewardedAd ?: return false
        rewardedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onDismissed()
                loadRewarded(activity)
            }
        }
        ad.show(activity) { _ ->
            onReward()
        }
        return true
    }

    fun isInterstitialReady(): Boolean = interstitialAd != null
    fun isRewardedReady(): Boolean = rewardedAd != null
}
