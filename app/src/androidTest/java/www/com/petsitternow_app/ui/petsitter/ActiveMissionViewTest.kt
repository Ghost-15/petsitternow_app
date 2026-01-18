package www.com.petsitternow_app.ui.petsitter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import www.com.petsitternow_app.domain.model.OwnerInfo
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.ui.petsitter.components.ActiveMissionView

/**
 * UI tests for ActiveMissionView composable.
 */
class ActiveMissionViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===== ASSIGNED status tests =====

    @Test
    fun activeMissionView_assigned_displaysEnRouteStatus() {
        val mission = createMission(WalkStatus.ASSIGNED)

        composeTestRule.setContent {
            ActiveMissionView(
                mission = mission,
                elapsedTime = "",
                distanceToOwner = null,
                isWithinCompletionRange = false,
                onStartWalk = {},
                onMarkReturning = {},
                onCompleteMission = {},
                onOpenInMaps = {}
            )
        }

        composeTestRule.onNodeWithText("En route vers le client").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dirigez-vous vers l'adresse indiquee").assertIsDisplayed()
        composeTestRule.onNodeWithText("J'ai recupere le chien").assertIsDisplayed()
    }

    @Test
    fun activeMissionView_assigned_startWalkButtonWorks() {
        var startWalkClicked = false
        val mission = createMission(WalkStatus.ASSIGNED)

        composeTestRule.setContent {
            ActiveMissionView(
                mission = mission,
                elapsedTime = "",
                distanceToOwner = null,
                isWithinCompletionRange = false,
                onStartWalk = { startWalkClicked = true },
                onMarkReturning = {},
                onCompleteMission = {},
                onOpenInMaps = {}
            )
        }

        composeTestRule.onNodeWithText("J'ai recupere le chien").performClick()
        assertTrue(startWalkClicked)
    }

    @Test
    fun activeMissionView_assigned_showsOpenInMapsButton() {
        val mission = createMission(WalkStatus.ASSIGNED)

        composeTestRule.setContent {
            ActiveMissionView(
                mission = mission,
                elapsedTime = "",
                distanceToOwner = null,
                isWithinCompletionRange = false,
                onStartWalk = {},
                onMarkReturning = {},
                onCompleteMission = {},
                onOpenInMaps = {}
            )
        }

        composeTestRule.onNodeWithText("Ouvrir dans Maps").assertIsDisplayed()
    }

    // ===== WALKING status tests =====

    @Test
    fun activeMissionView_walking_displaysWalkingStatus() {
        val mission = createMission(WalkStatus.WALKING)

        composeTestRule.setContent {
            ActiveMissionView(
                mission = mission,
                elapsedTime = "10m 30s",
                distanceToOwner = null,
                isWithinCompletionRange = false,
                onStartWalk = {},
                onMarkReturning = {},
                onCompleteMission = {},
                onOpenInMaps = {}
            )
        }

        composeTestRule.onNodeWithText("Promenade en cours").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profitez de la balade !").assertIsDisplayed()
        composeTestRule.onNodeWithText("10m 30s").assertIsDisplayed()
        composeTestRule.onNodeWithText("Je ramene le chien").assertIsDisplayed()
    }

    @Test
    fun activeMissionView_walking_markReturningButtonWorks() {
        var markReturningClicked = false
        val mission = createMission(WalkStatus.WALKING)

        composeTestRule.setContent {
            ActiveMissionView(
                mission = mission,
                elapsedTime = "10m 30s",
                distanceToOwner = null,
                isWithinCompletionRange = false,
                onStartWalk = {},
                onMarkReturning = { markReturningClicked = true },
                onCompleteMission = {},
                onOpenInMaps = {}
            )
        }

        composeTestRule.onNodeWithText("Je ramene le chien").performClick()
        assertTrue(markReturningClicked)
    }

    // ===== RETURNING status tests =====

    @Test
    fun activeMissionView_returning_displaysReturningStatus() {
        val mission = createMission(WalkStatus.RETURNING)

        composeTestRule.setContent {
            ActiveMissionView(
                mission = mission,
                elapsedTime = "25m 0s",
                distanceToOwner = 500f,
                isWithinCompletionRange = false,
                onStartWalk = {},
                onMarkReturning = {},
                onCompleteMission = {},
                onOpenInMaps = {}
            )
        }

        composeTestRule.onNodeWithText("Retour en cours").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ramenez le chien chez son proprietaire").assertIsDisplayed()
        composeTestRule.onNodeWithText("25m 0s").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mission terminee").assertIsDisplayed()
    }

    @Test
    fun activeMissionView_returning_displaysDistanceInfo() {
        val mission = createMission(WalkStatus.RETURNING)

        composeTestRule.setContent {
            ActiveMissionView(
                mission = mission,
                elapsedTime = "25m 0s",
                distanceToOwner = 500f,
                isWithinCompletionRange = false,
                onStartWalk = {},
                onMarkReturning = {},
                onCompleteMission = {},
                onOpenInMaps = {}
            )
        }

        composeTestRule.onNodeWithText("Distance du point de depart").assertIsDisplayed()
        composeTestRule.onNodeWithText("500m").assertIsDisplayed()
        composeTestRule.onNodeWithText("< 100m requis").assertIsDisplayed()
    }

    @Test
    fun activeMissionView_returning_withinRange_showsOK() {
        val mission = createMission(WalkStatus.RETURNING)

        composeTestRule.setContent {
            ActiveMissionView(
                mission = mission,
                elapsedTime = "30m 0s",
                distanceToOwner = 50f,
                isWithinCompletionRange = true,
                onStartWalk = {},
                onMarkReturning = {},
                onCompleteMission = {},
                onOpenInMaps = {}
            )
        }

        composeTestRule.onNodeWithText("50m").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun activeMissionView_returning_withinRange_completeMissionEnabled() {
        var completeMissionClicked = false
        val mission = createMission(WalkStatus.RETURNING)

        composeTestRule.setContent {
            ActiveMissionView(
                mission = mission,
                elapsedTime = "30m 0s",
                distanceToOwner = 50f,
                isWithinCompletionRange = true,
                onStartWalk = {},
                onMarkReturning = {},
                onCompleteMission = { completeMissionClicked = true },
                onOpenInMaps = {}
            )
        }

        composeTestRule.onNodeWithText("Mission terminee").assertIsEnabled()
        composeTestRule.onNodeWithText("Mission terminee").performClick()
        assertTrue(completeMissionClicked)
    }

    @Test
    fun activeMissionView_returning_outsideRange_completeMissionDisabled() {
        val mission = createMission(WalkStatus.RETURNING)

        composeTestRule.setContent {
            ActiveMissionView(
                mission = mission,
                elapsedTime = "30m 0s",
                distanceToOwner = 500f,
                isWithinCompletionRange = false,
                onStartWalk = {},
                onMarkReturning = {},
                onCompleteMission = {},
                onOpenInMaps = {}
            )
        }

        composeTestRule.onNodeWithText("Mission terminee").assertIsNotEnabled()
    }

    // ===== Owner info tests =====

    @Test
    fun activeMissionView_displaysOwnerInfo() {
        val mission = createMission(WalkStatus.ASSIGNED)

        composeTestRule.setContent {
            ActiveMissionView(
                mission = mission,
                elapsedTime = "",
                distanceToOwner = null,
                isWithinCompletionRange = false,
                onStartWalk = {},
                onMarkReturning = {},
                onCompleteMission = {},
                onOpenInMaps = {}
            )
        }

        composeTestRule.onNodeWithText("Marie Martin").assertIsDisplayed()
        composeTestRule.onNodeWithText("123 Rue de Paris").assertIsDisplayed()
    }

    // ===== Helper functions =====

    private fun createMission(status: WalkStatus): WalkRequest {
        return WalkRequest(
            id = "mission_test",
            ownerId = "owner1",
            petIds = listOf("pet1", "pet2"),
            location = WalkLocation(
                lat = 48.8566,
                lng = 2.3522,
                address = "123 Rue de Paris"
            ),
            duration = "30",
            status = status,
            assignedPetsitterId = "petsitter1",
            owner = OwnerInfo(
                firstName = "Marie",
                lastName = "Martin"
            ),
            createdAt = System.currentTimeMillis()
        )
    }
}
