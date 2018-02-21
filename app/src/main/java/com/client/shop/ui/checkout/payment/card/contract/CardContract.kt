package com.client.shop.ui.checkout.payment.card.contract

import android.support.annotation.StringRes
import com.client.shop.R
import com.client.shop.ui.base.contract.BaseLcePresenter
import com.client.shop.ui.base.contract.BaseLceView
import com.client.shop.gateway.entity.Card
import com.client.shop.gateway.entity.CardType
import com.domain.interactor.checkout.CheckCreditCardUseCase
import com.domain.interactor.checkout.GetAcceptedCardTypesUseCase
import com.domain.validator.CardValidator
import javax.inject.Inject

interface CardView : BaseLceView<List<CardType>> {

    fun cardTokenReceived(data: Pair<Card, String>)

    fun cardPassValidation(card: Card)

    fun cardValidationError(@StringRes error: Int)
}

class CardPresenter @Inject constructor(
    private val checkCreditCardUseCase: CheckCreditCardUseCase,
    private val getAcceptedCardTypesUseCase: GetAcceptedCardTypesUseCase
) :
    BaseLcePresenter<List<CardType>, CardView>(
        checkCreditCardUseCase,
        getAcceptedCardTypesUseCase
    ) {

    private val cardValidator: CardValidator = CardValidator()

    fun getAcceptedCardTypes() {
        getAcceptedCardTypesUseCase.execute(
            { view?.showContent(it) },
            { resolveError(it) },
            Unit
        )
    }

    fun processCardData(holderName: String, cardNumber: String, cardMonth: String,
                        cardYear: String, cardCvv: String) {
        val cardHolderNamePair = cardValidator.splitHolderName(holderName)
        if (cardHolderNamePair != null) {
            val card = Card(
                cardHolderNamePair.first,
                cardHolderNamePair.second,
                cardNumber,
                cardMonth,
                cardYear,
                cardCvv
            )
            when (cardValidator.isCardValid(card)) {
                CardValidator.CardValidationResult.VALID -> view?.cardPassValidation(card)
                CardValidator.CardValidationResult.INVALID_NAME -> view?.cardValidationError(R.string.card_name_error)
                CardValidator.CardValidationResult.INVALID_DATE -> view?.cardValidationError(R.string.card_date_error)
                CardValidator.CardValidationResult.INVALID_CVV -> view?.cardValidationError(R.string.card_cvv_error)
                CardValidator.CardValidationResult.INVALID_NUMBER -> view?.cardValidationError(R.string.card_number_error)
            }
        } else {
            view?.cardValidationError(R.string.card_name_error)
        }
    }

    fun getToken(card: Card) {
        checkCreditCardUseCase.execute(
            { view?.cardTokenReceived(Pair(card, it)) },
            { resolveError(it) },
            card
        )
    }
}