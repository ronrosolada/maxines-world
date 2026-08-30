package com.maxinesworld.gametarsiercanopy
import org.junit.Assert.*
import org.junit.Test
class TarsierCanopyViewModelTest{
 @Test fun chargeLeapAndGravityTransition(){val e=TarsierCanopyEngine();val charged=e.charge(CanopyState(),.5f);assertEquals(CanopyPhase.CHARGING,charged.phase);val airborne=e.leap(charged);assertEquals(CanopyPhase.AIRBORNE,airborne.phase);assertTrue(e.tick(airborne,.1f).y<airborne.y)}
 @Test fun fallingIsNonPunitiveBounce(){val e=TarsierCanopyEngine();val bounced=e.tick(CanopyState(phase=CanopyPhase.AIRBORNE,y=.89f,vy=1f),.1f);assertEquals(CanopyPhase.BOUNCING,bounced.phase);assertTrue(bounced.message.contains("Try again"))}
 @Test fun scoringAndCompletionResult(){var time=10L;val vm=TarsierCanopyViewModel("c","r"){time++};vm.collectFirefly();vm.collectFig();val result=vm.result();assertEquals(3,result.pawTokensEarned);assertEquals("tarsier-canopy",result.gameId)}
}
