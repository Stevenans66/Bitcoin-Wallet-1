package com.bitcoin.wallet.btc.data.live

import android.os.AsyncTask
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.Constants
import org.bitcoinj.wallet.Wallet

class WalletEncryptedLiveData(application: BitcoinApplication) :
    BaseWalletLiveData<Boolean>(application) {

    override fun onWalletActive(wallet: Wallet?) {
        load()
    }

    override fun load() {
        val wallet = wallet
        AsyncTask.execute {
            org.bitcoinj.core.Context.propagate(Constants.CONTEXT)
            postValue(wallet?.isEncrypted)
        }
    }
}
