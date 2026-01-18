package www.com.petsitternow_app.ui.walk

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import www.com.petsitternow_app.domain.model.ActiveWalk
import www.com.petsitternow_app.domain.model.PetsitterInfo
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.ui.walk.components.WalkStatusCard

/**
 * UI tests for WalkStatusCard composable.
 */
class WalkStatusCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===== MATCHING status tests =====

    @Test
    fun walkStatusCard_matching_displaysSearchingText() {
        val walkRequest = createWalkRequest(WalkStatus.MATCHING)

        composeTestRule.setContent {
            WalkStatusCard(
                walkRequest = walkRequest,
                activeWalk = null,
                elapsedTime = "",
                onCancel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Recherche en cours...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nous cherchons un petsitter disponible").assertIsDisplayed()
        composeTestRule.onNodeWithText("Annuler la demande").assertIsDisplayed()
    }

    @Test
    fun walkStatusCard_matching_cancelButtonWorks() {
        var cancelClicked = false
        val walkRequest = createWalkRequest(WalkStatus.MATCHING)

        composeTestRule.setContent {
            WalkStatusCard(
                walkRequest = walkRequest,
                activeWalk = null,
                elapsedTime = "",
                onCancel = { cancelClicked = true },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Annuler la demande").performClick()
        assertTrue(cancelClicked)
    }

    // ===== ASSIGNED status tests =====

    @Test
    fun walkStatusCard_assigned_displaysPetsitterEnRoute() {
        val petsitter = PetsitterInfo(
            id = "petsitter1",
            firstName = "Jean",
            lastName = "Dupont"
        )
        val walkRequest = createWalkRequest(WalkStatus.ASSIGNED, petsitter)

        composeTestRule.setContent {
            WalkStatusCard(
                walkRequest = walkRequest,
                activeWalk = null,
                elapsedTime = "",
                onCancel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Petsitter en route").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jean Dupont arrive...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jean Dupont").assertIsDisplayed()
        composeTestRule.onNodeWithText("Annuler").assertIsDisplayed()
    }

    // ===== WALKING status tests =====

    @Test
    fun walkStatusCard_walking_displaysWalkInProgress() {
        val petsitter = PetsitterInfo(
            id = "petsitter1",
            firstName = "Jean",
            lastName = "Dupont"
        )
        val walkRequest = createWalkRequest(WalkStatus.WALKING, petsitter)

        composeTestRule.setContent {
            WalkStatusCard(
                walkRequest = walkRequest,
                activeWalk = null,
                elapsedTime = "5m 30s",
                onCancel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Promenade en cours").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jean promene votre chien").assertIsDisplayed()
        composeTestRule.onNodeWithText("5m 30s").assertIsDisplayed()
    }

    // ===== RETURNING status tests =====

    @Test
    fun walkStatusCard_returning_displaysReturnInProgress() {
        val petsitter = PetsitterInfo(
            id = "petsitter1",
            firstName = "Jean",
            lastName = "Dupont"
        )
        val walkRequest = createWalkRequest(WalkStatus.RETURNING, petsitter)

        composeTestRule.setContent {
            WalkStatusCard(
                walkRequest = walkRequest,
                activeWalk = null,
                elapsedTime = "25m 0s",
                onCancel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Retour en cours").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jean ramene votre chien").assertIsDisplayed()
        composeTestRule.onNodeWithText("25m 0s").assertIsDisplayed()
    }

    // ===== COMPLETED status tests =====

    @Test
    fun walkStatusCard_completed_displaysCompletedMessage() {
        val walkRequest = createWalkRequest(WalkStatus.COMPLETED)

        composeTestRule.setContent {
            WalkStatusCard(
                walkRequest = walkRequest,
                activeWalk = null,
                elapsedTime = "30m 15s",
                onCancel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Promenade terminee !").assertIsDisplayed()
        composeTestRule.onNodeWithText("Duree totale: 30m 15s").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fermer").assertIsDisplayed()
    }

    @Test
    fun walkStatusCard_completed_dismissButtonWorks() {
        var dismissClicked = false
        val walkRequest = createWalkRequest(WalkStatus.COMPLETED)

        composeTestRule.setContent {
            WalkStatusCard(
                walkRequest = walkRequest,
                activeWalk = null,
                elapsedTime = "30m 15s",
                onCancel = {},
                onDismiss = { dismissClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Fermer").performClick()
        assertTrue(dismissClicked)
    }

    // ===== FAILED status tests =====

    @Test
    fun walkStatusCard_failed_displaysNoPetsitterAvailable() {
        val walkRequest = createWalkRequest(WalkStatus.FAILED)

        composeTestRule.setContent {
            WalkStatusCard(
                walkRequest = walkRequest,
                activeWalk = null,
                elapsedTime = "",
                onCancel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Aucun petsitter disponible").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reessayez plus tard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fermer").assertIsDisplayed()
    }

    // ===== CANCELLED status tests =====

    @Test
    fun walkStatusCard_cancelled_displaysCancelledMessage() {
        val walkRequest = createWalkRequest(WalkStatus.CANCELLED)

        composeTestRule.setContent {
            WalkStatusCard(
                walkRequest = walkRequest,
                activeWalk = null,
                elapsedTime = "",
                onCancel = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Promenade annulee").assertIsDisplayed()
        composeTestRule.onNodeWithText("La demande a ete annulee").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fermer").assertIsDisplayed()
    }

    // ===== Helper functions =====

    private fun createWalkRequest(
        status: WalkStatus,
        petsitter: PetsitterInfo? = null
    ): WalkRequest {
        return WalkRequest(
            id = "walk_test",
            ownerId = "owner1",
            petIds = listOf("pet1", "pet2"),
            location = WalkLocation(lat = 48.8566, lng = 2.3522, address = "Paris"),
            duration = "30",
            status = status,
            petsitter = petsitter,
            createdAt = System.currentTimeMillis()
        )
    }
}
