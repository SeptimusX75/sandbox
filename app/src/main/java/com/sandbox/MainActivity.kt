package com.sandbox

import android.app.Application
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.amplitude.api.Amplitude
import com.amplitude.api.AmplitudeClient
import com.heapanalytics.android.Heap
import com.jakewharton.rxbinding3.widget.itemClickEvents
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.sandbox.rxbindings.dismissEvents
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_property.view.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
  private val analytics by lazy { Analytics(application) }
  private val cache by lazy { Datastore(application) }
  private val disposables by lazy { CompositeDisposable() }

  override fun onDestroy() {
    disposables.dispose()
    super.onDestroy()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    cache.eventHistory()
      .map { it.map(EventHistory::value) }
      .subscribeBy { history ->
        ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, history.toList())
          .apply {
            eventName.setAdapter(this)
          }
      }
      .addTo(disposables)

    eventName.itemClickEvents().map { Unit }
      .mergeWith(eventName.dismissEvents())
      .map { eventName.value }
      .withLatestFrom(cache.eventHistory()) { name: String, history: Set<EventHistory> ->
        history.firstOrNull { it.value == name }?.properties ?: emptySet()
      }
      .distinctUntilChanged()
      .subscribeBy { propertySet ->
        propertyLayout.apply {
          removeAllViews()
          addView(addPropertyButton)
        }
        propertySet.forEach { createPropertyEntry(it) }
      }
      .addTo(disposables)

    addPropertyButton.setOnClickListener { createPropertyEntry() }
    fireEventButton.setOnClickListener {
      val indexOfButton = propertyLayout.indexOfChild(addPropertyButton)
      val propertySequence = propertyLayout.children
        .filterIndexed { index, _ -> index != indexOfButton }
        .map { view -> view.propertyKey.value to view.propertyValue.value }
      val json = propertySequence
        .filter { (_, value) -> value.isNotBlank() }
        .toMap()
      analytics.apply {
        cache.saveEvent(eventName.value, propertySequence.map(Pair<String, String>::first).toList())
        logEvent(eventName.value, json)
        uploadEvents()
      }
    }
  }

  private fun createPropertyEntry(property: String? = null) {
    layoutInflater.inflate(
      R.layout.layout_property,
      propertyLayout,
      false
    ).run {
      propertyKey.setText(property)
      propertyLayout.addView(
        this,
        propertyLayout.childCount - 1
      )
      clearButton.setOnClickListener {
        propertyLayout.removeView(this)
      }
    }
  }
}

fun Map<String, Any>.toJson(): JSONObject = json {
  forEach { (key, value) -> put(key, value) }
}

fun json(jsonAction: JSONObject.() -> Unit): JSONObject = JSONObject().apply(jsonAction)

class Analytics(app: Application) {
  fun logEvent(event: String, properties: Map<String, String>) {
    amplitude.logEvent(event, properties.toJson())
    mixPanel.track(event, properties.toJson())
    Heap.track(event, properties)
  }

  fun uploadEvents() {
    amplitude.uploadEvents()
    mixPanel.flush()
  }

  private val amplitude: AmplitudeClient by lazy {
    Amplitude.getInstance()
      .initialize(app, "91bbf2e9f4276cb2c5d35e4dc8514761")
      .enableForegroundTracking(app)
  }

  private val mixPanel by lazy {
    MixpanelAPI.getInstance(app, "c7b12b5f6540b49c30db2bd8ea5b62bc")
  }
}

val EditText.value get() = text.toString()