package www.com.petsitternow_app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import www.com.petsitternow_app.data.repository.AuthRepositoryImpl
import www.com.petsitternow_app.data.repository.PetRepositoryImpl
import www.com.petsitternow_app.data.repository.UserRepositoryImpl
import www.com.petsitternow_app.domain.repository.AuthRepository
import www.com.petsitternow_app.domain.repository.PetRepository
import www.com.petsitternow_app.domain.repository.UserRepository
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
}
