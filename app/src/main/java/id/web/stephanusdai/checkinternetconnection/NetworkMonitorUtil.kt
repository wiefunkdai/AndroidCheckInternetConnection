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

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class NetworkMonitorUtil {
    public enum class ConnectionType {
        WIFI, CELLULAR
    }

    public interface OnNetworkChangeListener {
        fun onNetworkChanged(isAvailable: Boolean, type: ConnectionType?)
    }

    public interface OnHostConnectionListener {
        fun onHostConnected(isConnected: Boolean)
    }

    private var urlWebsite: String? = null
    private var onNetworkChangeListener: OnNetworkChangeListener? = null
    private var onHostConnectionListener: OnHostConnectionListener? = null
    private var callbackNetworkChange: ((isAvailable: Boolean, type: ConnectionType?) -> Unit)? = null
    private var callbackHostConnected: ((isConnected: Boolean) -> Unit)? = null

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var context: Context

    public constructor(context: Context) {
        this.context = context
        this.registerComponents(context)
    }

    public constructor(context: Context, urlCheck: String) {
        this.context = context
        this.urlWebsite = urlCheck
        this.registerComponents(context)
    }

    public fun setHostUrlWebsite(urlLink: String) {
        this.urlWebsite = urlLink
    }

    public fun setOnNetworkChangeListener(listener: OnNetworkChangeListener) {
        this.onNetworkChangeListener = listener
    }

    public fun setOnHostConnectionListener(listener: OnHostConnectionListener) {
        this.onHostConnectionListener = listener
    }

    public fun setOnNetworkChanged(callback: (isAvailable: Boolean, type: ConnectionType?) -> Unit) {
        this.callbackNetworkChange = callback
    }

    public fun setOnHostConnected(callback: (isConnected: Boolean) -> Unit) {
        this.callbackHostConnected = callback
    }

    @Suppress("DEPRECATION")
    public fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (connectivityManager.activeNetwork == null) {
                this.setNetworkChange(false, null)
            }

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    super.onLost(network)
                    this@NetworkMonitorUtil.setNetworkChange(false, null)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    when {
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            this@NetworkMonitorUtil.setNetworkChange(true, ConnectionType.WIFI)
                        } else -> {
                            this@NetworkMonitorUtil.setNetworkChange(true, ConnectionType.CELLULAR)
                        }
                    }
                }
            }
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val intentFilter = IntentFilter()
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
            context.registerReceiver(attachNetworkChangeReceiver, intentFilter)
        }
    }

    public fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } else {
            context.unregisterReceiver(attachNetworkChangeReceiver)
        }
    }

    @Suppress("DEPRECATION")
    private val attachNetworkChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo

            if (activeNetworkInfo != null) {
                when (activeNetworkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> {
                        this@NetworkMonitorUtil.setNetworkChange(true, ConnectionType.WIFI)
                    } else -> {
                        this@NetworkMonitorUtil.setNetworkChange(true, ConnectionType.CELLULAR)
                    }
                }
            } else {
                this@NetworkMonitorUtil.setNetworkChange(false, null)
            }
        }
    }

    private fun setNetworkChange(isAvailable: Boolean, type: ConnectionType?) {
        if (this.callbackNetworkChange != null) {
            this.callbackNetworkChange?.invoke(isAvailable, type)
        }
        if (this.onNetworkChangeListener != null) {
            this.onNetworkChangeListener?.onNetworkChanged(isAvailable, type)
        }
        if (this.urlWebsite != null) {
            val isConnected = checkWebsiteConnection(context, this.urlWebsite!!, { isConnected ->
                if (this.callbackHostConnected != null) {
                    this.callbackHostConnected?.invoke(isConnected)
                }
                if (this.onHostConnectionListener != null) {
                    this.onHostConnectionListener?.onHostConnected(isConnected)
                }
            })
        }
    }

    private fun registerComponents(context: Context) {
        try {
            this.setOnNetworkChangeListener(context as OnNetworkChangeListener)
            this.setOnHostConnectionListener(context as OnHostConnectionListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @SuppressLint("ObsoleteSdkInt")
        @Suppress("DEPRECATION")
        public fun hasAvailableNetwork(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val activeNetwork =
                    connectivityManager.getNetworkCapabilities(network) ?: return false

                return when {
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    else -> false
                }
            } else {
                val networkInfo = connectivityManager.activeNetworkInfo ?: return false
                return networkInfo.isConnected
            }
        }

        @Suppress("DEPRECATION")
        public fun getConnectionType(context: Context): ConnectionType? {
            val connectionManager =
                context.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiConnection = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            val mobileDataConnection = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)

            if (wifiConnection != null) {
                if (wifiConnection.isConnectedOrConnecting) {
                    return ConnectionType.WIFI
                } else {
                    if (mobileDataConnection != null) {
                        if (mobileDataConnection.isConnectedOrConnecting) {
                            return ConnectionType.CELLULAR
                        }
                    }
                }
            }
            return null
        }

        @Suppress("DEPRECATION")
        @Throws(IOException::class)
        @OptIn(DelicateCoroutinesApi::class)
        public fun checkWebsiteConnection(context: Context, urlLink: String, callback: (isConnected: Boolean) -> Unit) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val netInfo = connectivityManager.activeNetworkInfo
            if (netInfo != null && netInfo.isConnected) {
                val SDK_INT = Build.VERSION.SDK_INT
                if (SDK_INT > 8) {
                    val policy = ThreadPolicy.Builder().permitAll().build()
                    StrictMode.setThreadPolicy(policy)
                }
                GlobalScope.launch(Dispatchers.IO) {
                    val urlConnection = URL(urlLink).openConnection()
                    if (urlConnection is HttpsURLConnection) {
                        with(urlConnection as HttpsURLConnection) {
                            setInstanceFollowRedirects(true)
                            connect()

                            withContext(Dispatchers.Main) {
                                callback(responseCode == HttpsURLConnection.HTTP_OK)
                            }
                        }
                    } else if (urlConnection is HttpURLConnection) {
                        with(urlConnection as HttpURLConnection) {
                            setInstanceFollowRedirects(true)
                            connect()

                            withContext(Dispatchers.Main) {
                                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                                    val newUrlLink = urlLink.replace("http://", "https://")
                                    checkWebsiteConnection(context, newUrlLink, { isConnected -> callback(isConnected) })
                                } else {
                                    callback(responseCode == HttpURLConnection.HTTP_OK)
                                }
                            }
                        }
                    }
                }
            } else {
                callback(false)
            }
        }

        @Suppress("DEPRECATION")
        public fun checkAddressReachable(serverAddress: String?, serverPort: Int, timeoutMS: Int): Boolean {
            var socket: Socket?
            try {
                socket = Socket()
                val socketAddress: SocketAddress = InetSocketAddress(serverAddress, serverPort)
                socket.connect(socketAddress, timeoutMS)
                if (socket.isConnected) {
                    return true
                    socket.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                socket = null
            }
            return false
        }
    }
}