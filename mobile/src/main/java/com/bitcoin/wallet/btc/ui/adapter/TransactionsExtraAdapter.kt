package com.bitcoin.wallet.btc.ui.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.DataPagingListAdapter
import com.bitcoin.wallet.btc.extension.inflate
import com.bitcoin.wallet.btc.model.explorer.address.AddressResponse
import com.bitcoin.wallet.btc.model.explorer.details.BlockDetailResponse
import com.bitcoin.wallet.btc.model.explorer.transaction.TxsItem
import com.bitcoin.wallet.btc.repository.data.TransactionsDataSource
import com.bitcoin.wallet.btc.ui.fragments.WalletAddressBottomDialog
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_explorer_address.*
import kotlinx.android.synthetic.main.item_explorer_title.*
import kotlinx.android.synthetic.main.item_explorer_title.tvTitle
import kotlinx.android.synthetic.main.item_explorer_transactions.*
import kotlinx.android.synthetic.main.item_transaction_extra_top.*
import java.text.NumberFormat
import java.util.*

class TransactionsExtraAdapter(private val retryCallback: () -> Unit,
                               private val showQrCode: (String?) -> Unit) :
    DataPagingListAdapter<Any, RecyclerView.ViewHolder>(
        diffCallback = object : DiffUtil.ItemCallback<Any>() {
            override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
                return oldItem != newItem
            }
        }
    ) {

    private val loc = Locale.US
    val number: NumberFormat = NumberFormat.getInstance(loc).apply {
        minimumFractionDigits = 8
        maximumFractionDigits = 8
    }

    override fun createBinding(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_explorer_transactions -> TransactionsViewHolder(parent.inflate(R.layout.item_explorer_transactions))
            R.layout.item_network_state -> NetworkViewHolder.create(parent, retryCallback)
            R.layout.item_explorer_title -> TitleViewHolder(parent.inflate(R.layout.item_explorer_title))
            R.layout.item_explorer_address -> AddressViewHolder(parent.inflate(R.layout.item_explorer_address))
            R.layout.item_transaction_extra_top -> BlocksViewHolder(parent.inflate(R.layout.item_transaction_extra_top))
            else -> throw IllegalArgumentException("unknown view type")
        }
    }

    override fun bind(binding: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_explorer_transactions -> {
                if (binding is TransactionsViewHolder) {
                    binding.apply {
                        val txs = getItem(position) as TxsItem
                        tvAgeT.time = txs.time?.times(1000) ?: 0
                        tvTransactionId.text = txs.txid
                        tvConfirmT.text = txs.confirmations?.toString()
                        tvInputs.text = txs.vin?.size?.toString()
                        tvOutputs.text = txs.vout?.size?.toString()
                        tvOutput.text = number.format(txs.valueOut ?: 0)?.plus(" BTC")
                        tvFees.text = number.format(txs.fees ?: 0)?.plus(" BTC")
                        tvSizeT.text = txs.size?.toString()
                    }
                }
            }

            R.layout.item_network_state -> {
                if (binding is NetworkViewHolder) {
                    binding.bindTo(networkState, position, true)
                }
            }

            R.layout.item_explorer_title -> {
                if (binding is TitleViewHolder) {
                    val title = getItem(position) as TransactionsDataSource.Response.Title
                    binding.apply {
                        tvTitle.text = title.title
                        chipCout.text = title.content
                    }
                }
            }

            R.layout.item_explorer_address -> {
                if (binding is AddressViewHolder) {
                    binding.apply {
                        val address = getItem(position) as AddressResponse
                        tvAddressS.text = address.addrStr
                        tvAddress.text = address.addrStr
                        tvBtcReceived.text = number.format(address.totalReceived).plus(" BTC")
                        tvBtcSent.text = number.format(address.totalSent).plus(" BTC")
                        tvBtcBalance.text = number.format(address.balance).plus(" BTC")
                        btnCopy.setOnClickListener {
                            address.addrStr?.let {
                                WalletAddressBottomDialog.copyTextToClipboard(this.itemView.context, it)
                                Toast.makeText(this.itemView.context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        }
                        btnQrCode.setOnClickListener {
                            showQrCode(address.addrStr)
                        }
                    }
                }
            }

            R.layout.item_transaction_extra_top -> {
                if (binding is BlocksViewHolder) {
                    binding.apply {
                        val summary = getItem(position) as BlockDetailResponse
                        tvHashBlock.text = summary.hash
                        tvBlockH.text = summary.height?.toString()
                        tvPreBlock.text = if (summary.previousblockhash != null) (summary.height?.minus(1)).toString() else ""
                        txMining.text = if (summary.nextblockhash != null) (summary.height?.plus(1)).toString() else "Mining"
                        imageView5.setImageResource(if (summary.nextblockhash != null) R.drawable.ic_block_selected else R.drawable.ic_block_mining)
                        tvAgeS.time = summary.time?.times(1000) ?: 0
                        tvTimestamp.text = summary.time?.toString()
                        tvDifficulty.text = number.also {
                            it.minimumFractionDigits = 2
                            it.maximumFractionDigits = 2
                        }.format(summary.difficulty)
                        tvBits.text = summary.bits
                        tvNonceS.text = summary.nonce?.toString()
                        tvHeightS.text = summary.height?.toString()
                        tvConfirmS.text = summary.confirmations?.toString()
                        tvSizeS.text = summary.size?.toString().plus(" kB")
                        tvBlockReward.text = number.format(summary.reward).plus(" BTC")
                        tvMerkle.text = summary.merkleroot
                        tvChainwork.text = summary.chainwork
                        btnCopyHash.setOnClickListener {
                            summary.hash?.let {
                                WalletAddressBottomDialog.copyTextToClipboard(this.itemView.context, it)
                                Toast.makeText(this.itemView.context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.item_network_state
        } else if (getItem(position) is TxsItem) {
            R.layout.item_explorer_transactions
        } else if (getItem(position) is TransactionsDataSource.Response.Title) {
            R.layout.item_explorer_title
        } else if (getItem(position) is AddressResponse) {
            R.layout.item_explorer_address
        } else {
            R.layout.item_transaction_extra_top
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }

    class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class BlocksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }

    class TransactionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View?
            get() = itemView
    }
}