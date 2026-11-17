package com.slidebolt.client.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.slidebolt.client.core.SlideBoltClient
import com.slidebolt.client.feature.browser.ClientController
import com.slidebolt.client.feature.browser.SlideBoltRoot
import com.slidebolt.client.platform.AndroidInstallationStore
import com.slidebolt.client.platform.AndroidNsdDiscovery
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {
    private val appScope = MainScope()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = ClientController(SlideBoltClient(), AndroidNsdDiscovery(applicationContext), AndroidInstallationStore(applicationContext), "android-tv", appScope)
        setContent { SlideBoltRoot(controller, television = true) }
    }
    override fun onDestroy() { appScope.cancel(); super.onDestroy() }
}
