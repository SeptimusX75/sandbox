package com.sandbox

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.Observable

private val gson = Gson().newBuilder().create()

class Datastore(application: Application) {

  private val history: SharedPreferences by lazy {
    application.getSharedPreferences("com.sandbox.datastore.history", MODE_PRIVATE)
  }

  private val eventRelay: Relay<Set<EventHistory>> by lazy {
    BehaviorRelay.createDefault(getEventHistory())
  }

  fun eventHistory(): Observable<Set<EventHistory>> = eventRelay.hide()

  fun saveEvent(event: String, properties: Iterable<String>? = null) {
    val trimmed = event.trim()
    history.edit { putString(trimmed, gson.toJson(EventHistory(trimmed, properties?.toSet()))) }
    eventRelay.accept(getEventHistory())
  }


  private fun getEventHistory(): Set<EventHistory> = history.all.values
    .filterIsInstance<String>()
    .map { gson.fromJson(it, EventHistory::class.java) }
    .toSet()
}

data class EventHistory(val value: String, val properties: Set<String>? = null)