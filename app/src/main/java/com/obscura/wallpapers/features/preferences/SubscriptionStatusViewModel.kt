package com.obscura.wallpapers.features.preferences

import androidx.lifecycle.ViewModel
import com.obscura.wallpapers.core.billing.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * ViewModel for subscription status
 */
@HiltViewModel
class SubscriptionStatusViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {
    
    val isPremiumUser: Flow<Boolean> = billingRepository.isPremiumUser
}
