package com.simplemobiletools.dialer.services

import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.simplemobiletools.dialer.App
import com.simplemobiletools.dialer.activities.CallActivity
import com.simplemobiletools.dialer.helpers.CallManager
import com.simplemobiletools.dialer.helpers.CallNotificationManager

class CallService : InCallService() {
    private val callNotificationManager by lazy { CallNotificationManager(this) }
    private val callDurationHelper by lazy { (application as App).callDurationHelper }

    private val callListener = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            if (state != Call.STATE_DISCONNECTED) {
                callNotificationManager.setupNotification()
            }

            if (state == Call.STATE_ACTIVE) {
                callDurationHelper.start()
            } else if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                callDurationHelper.cancel()
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        startActivity(CallActivity.getStartIntent(this))
        CallManager.call = call
        CallManager.inCallService = this
        CallManager.registerCallback(callListener)
        callNotificationManager.setupNotification()

        val callDirection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CallManager.call!!.details.callDirection
        } else {
            Call.Details.DIRECTION_UNKNOWN
        }

        if(callDirection!=Call.Details.DIRECTION_OUTGOING) {

                CallManager.getCallContact(applicationContext) { contact ->
//                    android.util.Log.d("SEUNG",contact?.photoUri.toString())
                    if (contact?.name.equals(contact?.number)) {
                        CallManager.reject();
                    }

            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.call = null
        CallManager.inCallService = null
        callNotificationManager.cancelNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        CallManager.unregisterCallback(callListener)
        callNotificationManager.cancelNotification()
        callDurationHelper.cancel()
    }
}
