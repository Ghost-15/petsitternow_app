package www.com.petsitternow_app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import www.com.petsitternow_app.data.repository.AuthRepositoryImpl
import www.com.petsitternow_app.data.repository.FeatureFlagRepositoryImpl
import www.com.petsitternow_app.data.repository.PetRepositoryImpl
import www.com.petsitternow_app.data.repository.PetsitterRepositoryImpl
import www.com.petsitternow_app.data.repository.UserRepositoryImpl
import www.com.petsitternow_app.data.repository.WalkRepositoryImpl
import www.com.petsitternow_app.domain.navigation.RouteProtectionManager
import www.com.petsitternow_app.domain.navigation.RouteProtectionManagerImpl
import www.com.petsitternow_app.domain.repository.AuthRepository
import www.com.petsitternow_app.domain.repository.FeatureFlagRepository
import www.com.petsitternow_app.domain.repository.PetRepository
import www.com.petsitternow_app.domain.repository.PetsitterRepository
import www.com.petsitternow_app.domain.repository.UserRepository
import www.com.petsitternow_app.domain.repository.WalkRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindPetRepository(
        petRepositoryImpl: PetRepositoryImpl
    ): PetRepository

    @Binds
    @Singleton
    abstract fun bindWalkRepository(
        walkRepositoryImpl: WalkRepositoryImpl
    ): WalkRepository

    @Binds
    @Singleton
    abstract fun bindPetsitterRepository(
        petsitterRepositoryImpl: PetsitterRepositoryImpl
    ): PetsitterRepository

    @Binds
    @Singleton
    abstract fun bindFeatureFlagRepository(
        impl: FeatureFlagRepositoryImpl
    ): FeatureFlagRepository

    @Binds
    @Singleton
    abstract fun bindRouteProtectionManager(
        impl: RouteProtectionManagerImpl
    ): RouteProtectionManager
}

