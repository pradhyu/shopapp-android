package com.shopify.ui.checkout

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.domain.entity.CartProduct
import com.domain.entity.Customer
import com.shopify.ShopifyWrapper
import com.shopify.api.R
import com.shopify.entity.Checkout
import com.shopify.entity.ShippingRate
import com.shopify.ui.address.AddressActivity
import com.shopify.ui.address.AddressListActivity
import com.shopify.ui.checkout.contract.CheckoutPresenter
import com.shopify.ui.checkout.contract.CheckoutView
import com.shopify.ui.checkout.di.CheckoutModule
import com.shopify.ui.checkout.view.ShippingOptionsView
import com.shopify.ui.payment.PaymentActivity
import com.ui.base.lce.BaseActivity
import com.ui.base.lce.view.LceLayout
import com.ui.const.RequestCode
import kotlinx.android.synthetic.main.activity_checkout.*
import java.math.BigDecimal
import javax.inject.Inject

class CheckoutActivity :
    BaseActivity<Checkout, CheckoutView, CheckoutPresenter>(),
    CheckoutView,
    ShippingOptionsView.OnOptionSelectedListener {

    companion object {
        fun getStartIntent(context: Context) = Intent(context, CheckoutActivity::class.java)
    }

    @Inject
    lateinit var checkoutPresenter: CheckoutPresenter
    private var checkout: Checkout? = null
    private var customer: Customer? = null

    //ANDROID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(getString(R.string.checkout))

        setupListeners()
        loadData()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCode.ADD_ADDRESS || requestCode == RequestCode.EDIT_ADDRESS) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == RequestCode.PAYMENT && resultCode == Activity.RESULT_OK) {
            paymentView.setData(data)
        }
    }

    //INIT

    override fun inject() {
        ShopifyWrapper.component
            .attachCheckoutComponent(CheckoutModule())
            .inject(this)
    }

    override fun getContentView() = R.layout.activity_checkout

    override fun createPresenter() = checkoutPresenter

    override fun useModalStyle() = true

    //SETUP

    private fun setupListeners() {
        shippingAddressView.setClickListeners(
            editClickListener = View.OnClickListener {
                checkout?.let {
                    val address = it.address
                    if (customer != null) {
                        startActivityForResult(
                            AddressListActivity.getStartIntent(
                                this,
                                it.checkoutId,
                                address
                            ),
                            RequestCode.EDIT_ADDRESS
                        )
                    } else if (address != null) {
                        startActivityForResult(
                            AddressActivity.getStartIntent(this, it.checkoutId, address),
                            RequestCode.EDIT_ADDRESS
                        )
                    }
                }
            },
            addAddressClickListener = View.OnClickListener {
                checkout?.let {
                    startActivityForResult(
                        AddressActivity.getStartIntent(this, it.checkoutId),
                        RequestCode.ADD_ADDRESS
                    )
                }
            }
        )
        paymentView.setClickListeners(
            addAddressClickListener = View.OnClickListener {
                startActivityForResult(PaymentActivity.getStartIntent(this), RequestCode.PAYMENT)
            }
        )
        shippingOptionsView.onOptionSelectedListener = this
    }

    //LCE

    override fun loadData(pullToRefresh: Boolean) {
        super.loadData(pullToRefresh)
        val checkout = this.checkout
        if (checkout != null) {
            presenter.getCheckout(checkout.checkoutId)
        } else {
            presenter.getCartProductList()
        }
    }

    override fun showContent(data: Checkout) {
        super.showContent(data)
        checkout = data
        shippingAddressView.setAddress(data.address)
        priceView.setData(
            data.subtotalPrice, BigDecimal.ZERO,
            data.shippingRate?.price ?: BigDecimal.ZERO, data.totalPrice, data.currency
        )
        if (data.address != null) {
            presenter.getShippingRates(data.checkoutId)
        } else {
            shippingOptionsView.unSelectAddress()
        }
    }

    override fun customerReceived(customer: Customer?) {
        this.customer = customer
    }

    override fun cartProductListReceived(cartProductList: List<CartProduct>) {
        myCartView.setData(cartProductList)
    }

    override fun shippingRatesReceived(shippingRates: List<ShippingRate>) {
        checkout?.let {
            shippingOptionsView.setData(it, shippingRates)
        }
    }

    //CALLBACK

    override fun onOptionSelected(shippingRate: ShippingRate) {
        shippingOptionsView.selectShippingRate(shippingRate)
        changeState(LceLayout.LceState.LoadingState)
        checkout?.let { presenter.setShippingRates(it, shippingRate) }
    }
}