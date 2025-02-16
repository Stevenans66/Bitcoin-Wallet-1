package com.bitcoin.wallet.btc.ui.activitys

import android.content.SharedPreferences
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.data.ExchangeRatesProvider
import com.bitcoin.wallet.btc.extension.hideKeyboard
import com.bitcoin.wallet.btc.service.BlockchainState
import com.bitcoin.wallet.btc.ui.adapter.ExchangeRatesAdapter
import com.bitcoin.wallet.btc.utils.Configuration
import com.bitcoin.wallet.btc.viewmodel.ExchangeRatesViewModel
import kotlinx.android.synthetic.main.activity_exchange_rate.*
import org.bitcoinj.core.Coin

class ExchangeRatesActivity : BaseActivity(), ExchangeRatesAdapter.OnClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val viewModel: ExchangeRatesViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ExchangeRatesViewModel::class.java)
    }
    private val config: Configuration by lazy {
        application.config
    }
    private val exAdapter: ExchangeRatesAdapter by lazy {
        ExchangeRatesAdapter(this)
    }

    override fun layoutRes(): Int {
        return R.layout.activity_exchange_rate
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar(getString(R.string.exchange_rates))
        exchange_rates_list.apply {
            layoutManager = LinearLayoutManager(this@ExchangeRatesActivity)
            setHasFixedSize(false)
            addItemDecoration(
                DividerItemDecoration(
                    this@ExchangeRatesActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
            adapter = exAdapter
        }

        viewModel.exchangeRates.observe(this, Observer<Cursor> { cursor ->
            if (cursor!!.count == 0 && viewModel.query == null) {
                exchange_rates_list_group.displayedChild = 1
            } else if (cursor.count == 0 && viewModel.query != null) {
                exchange_rates_list_group.displayedChild = 2
            } else {
                exchange_rates_list_group.displayedChild = 3
                maybeSubmitList()

                val defaultCurrency = config.exchangeCurrencyCode
                if (defaultCurrency != null) {
                    cursor.moveToPosition(-1)
                    while (cursor.moveToNext()) {
                        if (cursor.getString(
                                cursor.getColumnIndexOrThrow(ExchangeRatesProvider.KEY_CURRENCY_CODE)
                            ) == defaultCurrency
                        ) {
                            exchange_rates_list.scrollToPosition(cursor.position)
                            break
                        }
                    }
                }

                cursor.moveToPosition(0)
                val source = ExchangeRatesProvider.getExchangeRate(cursor).source
                supportActionBar?.setSubtitle(
                    if (source != null) getString(R.string.price_from, source) else null
                )
            }
        })
        config.registerOnSharedPreferenceChangeListener(this)
        viewModel.balance.observe(this, Observer<Coin> { maybeSubmitList() })
        viewModel.blockchainState.observe(this, Observer<BlockchainState> { maybeSubmitList() })
    }

    override fun onDestroy() {
        super.onDestroy()
        config.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onExchangeRateMenuClick(view: View, currencyCode: String) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.menu_exchange_rates)
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.exchange_rates_context_set_as_default) {
                config.exchangeCurrencyCode = currencyCode
                view.hideKeyboard()
                true
            } else {
                false
            }

        }
        popupMenu.show()
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        if (Configuration.PREFS_KEY_EXCHANGE_CURRENCY == key)
            maybeSubmitList()
        else if (Configuration.PREFS_KEY_BTC_PRECISION == key)
            maybeSubmitList()
    }

    private fun maybeSubmitList() {
        val exchangeRates = viewModel.exchangeRates.value
        if (exchangeRates != null) {
            exAdapter.submitList(
                ExchangeRatesAdapter.buildListItems(
                    exchangeRates, viewModel.balance.value,
                    viewModel.blockchainState.value, config.exchangeCurrencyCode, config.btcBase
                )
            )
        }
    }
}