package com.bitcoin.wallet.btc.data.live

import android.content.*
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.service.BlockchainService
import com.bitcoin.wallet.btc.service.BlockchainState

class BlockchainStateLiveData(private val application: BitcoinApplication) :
    LiveData<BlockchainState>(),
    ServiceConnection {
    private val broadcastManager: LocalBroadcastManager =
        LocalBroadcastManager.getInstance(application)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, broadcast: Intent) {
            value = BlockchainState.fromIntent(broadcast)
        }
    }

    override fun onActive() {
        broadcastManager.registerReceiver(
            receiver,
            IntentFilter(BlockchainService.ACTION_BLOCKCHAIN_STATE)
        )
        if (application != null) {
            application.bindService(
                Intent(application, BlockchainService::class.java),
                this,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onInactive() {
        application.unbindService(this)
        broadcastManager.unregisterReceiver(receiver)
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        val blockchainService = (service as BlockchainService.LocalBinder).service
        value = blockchainService.blockchainState
    }

    override fun onServiceDisconnected(name: ComponentName) {}
}
