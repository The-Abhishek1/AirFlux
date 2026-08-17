package com.xcloak.airflux.core.billing

import java.util.Locale

data class ProPricing(val display: String, val currencyNote: String)

object PricingUtils {
    /** Best-effort regional price display based on device locale.
     *  TODO: once real Play Billing is wired, replace this with the actual
     *  localized price string Play returns for the product — Play handles
     *  currency conversion and local tax display far more accurately than
     *  a locale guess ever can. This is a placeholder for pre-launch UI only. */
    fun getDisplayPricing(): ProPricing {
        val country = Locale.getDefault().country
        return when (country) {
            "IN" -> ProPricing("₹199", "one-time, lifetime")
            "US", "CA", "AU", "GB" -> ProPricing("$5", "one-time, lifetime")
            else -> {
                val euCountries = setOf("DE", "FR", "IT", "ES", "NL", "BE", "IE", "PT", "AT", "FI")
                if (country in euCountries) ProPricing("€5", "one-time, lifetime")
                else ProPricing("$5", "one-time, lifetime")
            }
        }
    }
}