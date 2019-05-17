package com.bitcoin.wallet.btc.base

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.extension.setBottomSheetCallback
import com.github.zagum.expandicon.ExpandIconView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment(), HasSupportFragmentInjector {

    @Inject
    lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>

    private var disposal = CompositeDisposable()
    var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    @LayoutRes
    abstract fun layoutRes(): Int

    abstract fun onFragmentCreated(view: View, savedInstanceState: Bundle?)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val contextThemeWrapper = ContextThemeWrapper(context, context?.theme)
        val themeAwareInflater = inflater.cloneInContext(contextThemeWrapper)
        val view = themeAwareInflater.inflate(layoutRes(), container, false)
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                onGlobalLayoutChanged(view)
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onFragmentCreated(view, savedInstanceState)

        val toggleArrow = view.findViewById<View?>(R.id.toggleArrow)
        toggleArrow?.setOnClickListener {
            bottomSheetBehavior?.let { behaviour ->
                if (behaviour.state != BottomSheetBehavior.STATE_EXPANDED) {
                    behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    behaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }
    }

    override fun onDestroyView() {
        disposal.clear()
        super.onDestroyView()
    }

    fun dismissDialog() = dialog?.dismiss()

    protected fun addDisposal(disposable: Disposable) {
        disposal.add(disposable)
    }

    protected fun removeAndAddDisposal(disposable: Disposable) {
        disposal.remove(disposable)
        disposal.add(disposable)
    }

    protected fun setupToolbar(resId: Int, menuId: Int? = null) {
        view?.findViewById<Toolbar?>(R.id.toolbar)?.apply {
            val titleText = findViewById<TextView?>(R.id.toolbarTitle)
            if (titleText != null) {
                titleText.setText(resId)
            } else {
                setTitle(resId)
            }
            setNavigationOnClickListener { dismiss() }
            menuId?.let { inflateMenu(it) }
        }
    }

    private fun onGlobalLayoutChanged(view: View) {
        val parent = dialog?.findViewById<ViewGroup>(R.id.design_bottom_sheet)
        if (parent != null) {
            val toggleArrow = view.findViewById<ExpandIconView?>(R.id.toggleArrow)
            toggleArrow?.setState(ExpandIconView.LESS, true)
            parent.setBackgroundColor(Color.TRANSPARENT)
            bottomSheetBehavior = BottomSheetBehavior.from(parent)
            bottomSheetBehavior?.setBottomSheetCallback({ newState ->
                if (newState == BottomSheetBehavior.STATE_HIDDEN) dialog?.cancel()
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> toggleArrow?.setState(ExpandIconView.MORE, true)
                    BottomSheetBehavior.STATE_COLLAPSED -> toggleArrow?.setState(ExpandIconView.LESS, true)
                    else -> toggleArrow?.setFraction(0.5f, false)
                }
            })
            bottomSheetBehavior?.let { behaviour ->
                toggleArrow?.setOnClickListener {
                    toggleArrow.setOnClickListener {
                        if (behaviour.state != BottomSheetBehavior.STATE_EXPANDED) {
                            behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                        } else {
                            behaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }
                }
            }
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior?.isFitToContents = true
        }
    }


    override fun supportFragmentInjector(): AndroidInjector<Fragment>? = this.childFragmentInjector
}