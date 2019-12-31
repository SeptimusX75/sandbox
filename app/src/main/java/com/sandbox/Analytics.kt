package com.sandbox

import com.amplitude.api.AmplitudeClient
import com.heapanalytics.android.Heap
import com.mixpanel.android.mpmetrics.MixpanelAPI

class Analytics(private val providers: Collection<Provider>) {

  fun logEvent(event: String, properties: Map<String, String>) =
    providers.forEach { provider -> provider.track(event, properties) }

  fun uploadEvents() = providers.forEach { provider -> provider.publish() }

  interface Provider {
    fun publish()
    fun track(event: String, properties: Map<String, String>)
  }
}

class AmplitudeProvider(private val client: AmplitudeClient) : Analytics.Provider {
  override fun publish() = client.uploadEvents()
  override fun track(event: String, properties: Map<String, String>) =
    client.logEvent(event, properties.toJson())
}

class HeapProvider : Analytics.Provider {
  override fun publish() {}
  override fun track(event: String, properties: Map<String, String>) = Heap.track(event, properties)
}

class MixPanelProvider(private val client: MixpanelAPI) : Analytics.Provider {
  override fun publish() = client.flush()
  override fun track(event: String, properties: Map<String, String>) =
    client.track(event, properties.toJson())
}
