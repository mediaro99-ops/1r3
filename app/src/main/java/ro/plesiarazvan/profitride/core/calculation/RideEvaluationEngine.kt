package ro.plesiarazvan.profitride.core.calculation

import ro.plesiarazvan.profitride.core.model.Evaluation
import ro.plesiarazvan.profitride.core.model.ProfitResult
import ro.plesiarazvan.profitride.core.model.RideOffer
import ro.plesiarazvan.profitride.core.model.UserThresholds

object RideEvaluationEngine {
    fun evaluate(offer: RideOffer, result: ProfitResult, thresholds: UserThresholds): Evaluation {
        val baseGood =
            result.ronPerKm >= thresholds.minRonKm &&
            result.ronPerHour >= thresholds.minRonHour &&
            result.profit >= thresholds.minProfit

        val scoreGood = if (thresholds.rideScoreEnabled && offer.rating != null) {
            offer.rating >= thresholds.minRideScore
        } else true

        return if (baseGood && scoreGood) Evaluation.GOOD else Evaluation.BAD
    }
}
