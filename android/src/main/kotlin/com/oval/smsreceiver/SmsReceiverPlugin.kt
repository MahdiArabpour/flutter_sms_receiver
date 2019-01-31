package com.oval.smsreceiver

import android.app.Activity
import android.content.IntentFilter
import com.google.android.gms.auth.api.phone.SmsRetriever
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class SmsReceiverPlugin(private val activity: Activity,
                        private val methodChannel: MethodChannel): MethodCallHandler {
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "com.oval.sms_receiver")
      channel.setMethodCallHandler(SmsReceiverPlugin(registrar.activity(), channel))
    }
  }

  private val smsBroadcastReceiver by lazy { SMSBroadcastReceiver() }


  override fun onMethodCall(call: MethodCall, result: Result) {
    when(call.method) {
      "getPlatformVersion" -> {
        result.success("Android ${android.os.Build.VERSION.RELEASE}")
      }
      "startListening" -> {
        val client = SmsRetriever.getClient(activity)
        val retriever = client.startSmsRetriever()

        retriever.addOnSuccessListener {
          val listener = object:SMSBroadcastReceiver.Listener {
            override fun onSMSReceived(pin: String) {
              print("HEYYY received")
              methodChannel.invokeMethod("onSmsReceived", pin)
              activity.unregisterReceiver(smsBroadcastReceiver)
            }
            override fun onTimeout() {
              methodChannel.invokeMethod("onTimeout", null)
              activity.unregisterReceiver(smsBroadcastReceiver)
            }
          }
          smsBroadcastReceiver.injectListener(listener)
          activity.registerReceiver(smsBroadcastReceiver, IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION))
        }
        retriever.addOnFailureListener {
          print("HEYYY failure")
          methodChannel.invokeMethod("onFailureListener", null)
        }
        result.success(true)
      }
      else -> {
        result.notImplemented()
      }
    }
  }
}
