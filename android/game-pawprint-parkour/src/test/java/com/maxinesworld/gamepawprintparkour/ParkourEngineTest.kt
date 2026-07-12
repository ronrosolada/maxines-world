package com.maxinesworld.gamepawprintparkour
import org.junit.Assert.*
import org.junit.Test
class ParkourEngineTest{
 private val e=ParkourEngine(listOf(ParkourCourse("t","Test",10f,listOf(CourseObstacle("o",2f,ObstacleKind.LOG)),listOf(CourseToken("p",1f,.65f)))))
 @Test fun `same seed selects same course`(){assertEquals(e.initial(1).course.id,e.initial(1).course.id)}
 @Test fun `jump only begins on ground while running`(){val s=e.start(e.initial(0));assertTrue(e.jump(s,JumpKind.SHORT).velocityY>0);assertEquals(e.jump(s.copy(y=1f),JumpKind.SHORT),s.copy(y=1f))}
 @Test fun `token is collected once`(){var s=e.start(e.initial(0)).copy(x=.8f,y=.65f);s=e.tick(s,.1f);val count=s.tokens;s=e.tick(s,.01f);assertEquals(count,s.tokens)}
 @Test fun `collision is recorded once without game over`(){var s=e.start(e.initial(0)).copy(x=1.8f);s=e.tick(s,.1f);val bumps=s.bumps;s=e.tick(s,.1f);assertEquals(bumps,s.bumps);assertEquals(ParkourPhase.RUNNING,s.phase)}
 @Test fun `assisted mode jumps before obstacle`(){var s=e.start(e.setAssisted(e.initial(0),true)).copy(x=.1f);s=e.tick(s,.01f);assertTrue(s.velocityY>0f)}
 @Test fun `course completes exactly once`(){var s=e.start(e.initial(0)).copy(x=9.9f);s=e.tick(s,.1f);assertEquals(1,s.roundsCompleted);val again=e.tick(s,.1f);assertEquals(1,again.roundsCompleted)}
}
