package app.noiseflow.di

import app.noiseflow.billing.BillingGateway
import app.noiseflow.billing.play.PlayBillingGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Play flavor only. The rustore source set has its own copy of this file. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun gateway(impl: PlayBillingGateway): BillingGateway
}
