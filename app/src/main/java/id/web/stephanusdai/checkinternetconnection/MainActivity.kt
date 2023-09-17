/*
 * AndroidCheckInternetConnection by Stephanus Dai
 * @fullname : Stephanus Bagus Saputra
 *             ( 戴 Dai 偉 Wie 峯 Funk )
 * @email    : wiefunk@stephanusdai.web.id
 * @contact  : http://t.me/wiefunkdai
 * @support  : http://opencollective.com/wiefunkdai
 * @weblink  : http://www.stephanusdai.web.id
 * Copyright (c) ID 2023 Stephanus Bagus Saputra. All rights reserved.
 * Terms of the following https://stephanusdai.web.id/p/license.html
 */

package id.web.stephanusdai.checkinternetconnection

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class MainActivity() : AppCompatActivity(), NetworkMonitorUtil.OnNetworkChangeListener {
    private val urlWebsiteLink: String = "http://www.stephanusdai.web.id"
    private lateinit var networkMonitor: NetworkMonitorUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        networkMonitor = NetworkMonitorUtil(this, urlWebsiteLink)

        /* networkMonitor.setOnNetworkChangeListener { isAvailable, type ->
            runOnUiThread {
                this.attachNetworkChanged(isAvailable, type)
            }
        } */
    }

    override fun onResume() {
        super.onResume()
        networkMonitor.start()
    }

    override fun onStop() {
        super.onStop()
        networkMonitor.stop()
    }

    override fun onNetworkChanged(isAvailable: Boolean, type: NetworkMonitorUtil.ConnectionType?) {
        runOnUiThread {
            this.attachNetworkChanged(isAvailable, type)
        }
    }

    private fun attachNetworkChanged(isAvailable: Boolean, type: NetworkMonitorUtil.ConnectionType?) {
        val imageNetworkStatus: ImageView = findViewById(R.id.imageNetworkStatus) as ImageView
        val labelNetworkStatus: TextView = findViewById(R.id.labelNetworkStatus) as TextView
        val urlWebsiteStatus: TextView = findViewById(R.id.urlWebsiteStatus) as TextView
        when (isAvailable) {
            true -> {
                when (type) {
                    NetworkMonitorUtil.ConnectionType.WIFI -> {
                        imageNetworkStatus.apply {
                            setImageResource(R.drawable.ic_baseline_wifinetwork_green)
                        }
                        labelNetworkStatus.apply {
                            setText("Wifi Connection")
                            setTextColor(Color.rgb(0, 71, 0))
                        }
                    }
                    NetworkMonitorUtil.ConnectionType.CELLULAR -> {
                        imageNetworkStatus.apply {
                            setImageResource(R.drawable.ic_baseline_mobilenetwork_orange)
                        }
                        labelNetworkStatus.apply {
                            setText("Mobile Data Connection")
                            setTextColor(Color.rgb(141, 170, 0))
                        }
                    }
                    else -> { }
                }
            }
            false -> {
                imageNetworkStatus.apply {
                    setImageResource(R.drawable.ic_baseline_notfoundweb_red)
                }
                labelNetworkStatus.apply {
                    setText("No Connection")
                    setTextColor(Color.rgb(149, 0, 0))
                }
            }
        }
        networkMonitor.setOnHostConnected({ isConnected ->
            runOnUiThread {
                urlWebsiteStatus.apply {
                    setText(urlWebsiteLink)
                    if (isConnected) {
                        setTextColor(Color.rgb(0, 71, 0))
                    } else {
                        setTextColor(Color.rgb(149, 0, 0))
                    }
                }
            }
        })
    }

    /* private fun attachNetworkChanged(isAvailable: Boolean, type: NetworkMonitorUtil.ConnectionType?) {
        var messageStatus: String = "Status Network"

        when (isAvailable) {
            true -> {
                when (type) {
                    NetworkMonitorUtil.ConnectionType.WIFI -> {
                        messageStatus += " is Running with Wifi Device"
                    }
                    NetworkMonitorUtil.ConnectionType.CELLULAR -> {
                        messageStatus += " is Running with Mobile Data Device"
                    }
                    else -> { }
                }
            }
            false -> {
                messageStatus += " is Running with No Available Connection"
            }
        }
        networkMonitor.setOnHostConnected({ isConnected ->
            runOnUiThread {
                when(isConnected) {
                    true -> {
                        messageStatus += " and Website " + urlWebsiteLink + " is Connected"
                    }
                    false -> {
                        messageStatus += " and Website " + urlWebsiteLink + " is Error Response"
                    }
                }
                Toast.makeText(this, messageStatus, Toast.LENGTH_LONG).show()
            }
        })
    } */
}
