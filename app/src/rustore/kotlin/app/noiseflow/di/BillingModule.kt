package app.noiseflow.di

import app.noiseflow.billing.BillingGateway
import app.noiseflow.billing.rustore.RuStoreBillingGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * RuStore flavor only. Nothing here may reference Google Play Services --
 * devices in this channel often do not have them at all, and CI fails the
 * build if any appear in this flavor's dependency graph.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun gateway(impl: RuStoreBillingGateway): BillingGateway
}
