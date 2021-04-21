package com.pandatv.mqttdemo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class MainActivity : AppCompatActivity() {
    var mqttAndroidClient: MqttAndroidClient? = null
    val serverUri = "tcp://iot.eclipse.org:1883"
    var clientId = "ExampleAndroidClient"
    val subscriptionTopic = "exampleAndroidTopic"
    val publishTopic = "exampleAndroidPublishTopic"
    val publishMessage = "Hello World!"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        clientId += System.currentTimeMillis()
        val fab = findViewById<View>(R.id.fab) as FloatingActionButton
        fab.setOnClickListener {
            publishMessage()
        }

        mqttAndroidClient = MqttAndroidClient(applicationContext, serverUri, clientId)
        mqttAndroidClient!!.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                if (reconnect) {
                    Log.e("馋猫", "Reconnected to : $serverURI")
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic()
                } else {
                    Log.e("馋猫", "Connected to: $serverURI")
                }
            }

            override fun connectionLost(cause: Throwable) {
                Log.e("馋猫", "The Connection was lost.")
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, message: MqttMessage) {
                Log.e("馋猫", "Incoming message: " + String(message.payload))
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })

        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = false


        try {
            //Log.e("馋猫","Connecting to " + serverUri);
            mqttAndroidClient!!.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.bufferSize = 100
                    disconnectedBufferOptions.isPersistBuffer = false
                    disconnectedBufferOptions.isDeleteOldestMessages = false
                    mqttAndroidClient!!.setBufferOpts(disconnectedBufferOptions)
                    subscribeToTopic()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e("馋猫", "Failed to connect to: $serverUri")
                }
            })
        } catch (ex: MqttException) {
            ex.printStackTrace()
        }

    }

    fun subscribeToTopic() {
        try {
            mqttAndroidClient!!.subscribe(subscriptionTopic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.e("馋猫", "Subscribed!")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e("馋猫", "Failed to subscribe")
                }
            })

            // THIS DOES NOT WORK!
            mqttAndroidClient!!.subscribe(subscriptionTopic, 0) { topic, message -> // message Arrived!
                Log.e("馋猫", "Message: " + topic + " : " + String(message.payload))
            }
        } catch (ex: MqttException) {
            Log.e("馋猫", "Exception whilst subscribing")
            ex.printStackTrace()
        }
    }

    fun publishMessage() {
        try {
            val message = MqttMessage()
            message.payload = publishMessage.toByteArray()
            mqttAndroidClient!!.publish(publishTopic, message)
            Log.e("馋猫", "Message Published")
            if (!mqttAndroidClient!!.isConnected) {
                Log.e("馋猫", mqttAndroidClient!!.bufferedMessageCount.toString() + " messages in buffer.")
            }
        } catch (e: MqttException) {
            Log.e("馋猫", "Error Publishing: " + e.message)
            e.printStackTrace()
        }
    }
}