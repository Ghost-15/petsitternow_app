package www.com.petsitternow_app.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun saveOnboardingData(
        userId: String,
        firstName: String,
        lastName: String,
        phone: String,
        gender: String,
        dateOfBirth: String,
        userType: String,
        address: String,
        city: String,
        codePostal: String
    ): Flow<Result<Unit>>
}

