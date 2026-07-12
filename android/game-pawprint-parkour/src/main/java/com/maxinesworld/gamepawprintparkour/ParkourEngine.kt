package com.maxinesworld.gamepawprintparkour

import kotlin.math.abs

class ParkourEngine(private val courses: List<ParkourCourse> = ParkourCourses.all) {
    fun initial(seed:Int, assisted:Boolean=false, reducedMotion:Boolean=false):ParkourState =
        ParkourState(courses[Math.floorMod(seed,courses.size)], assistedMode=assisted, reducedMotion=reducedMotion)

    fun start(s:ParkourState)=if(s.phase==ParkourPhase.READY) s.copy(phase=ParkourPhase.RUNNING,feedback="Collect pawprints and hop over soft obstacles!") else s
    fun jump(s:ParkourState, kind:JumpKind):ParkourState = if(s.phase==ParkourPhase.RUNNING && s.onGround)
        s.copy(velocityY=kind.velocity,feedback=if(kind==JumpKind.LONG) "Big jump!" else "Nice hop!") else s

    fun tick(s0:ParkourState, deltaSeconds:Float):ParkourState {
        if(s0.phase!=ParkourPhase.RUNNING || deltaSeconds<=0f) return s0
        val dt=deltaSeconds.coerceAtMost(.05f)
        var s=s0
        val next=s.course.obstacles.firstOrNull { it.id !in s.passedObstacleIds && it.x>=s.x }
        if(s.assistedMode && s.onGround && next!=null && next.x-s.x in 0f..2.35f) s=jump(s, if(next.kind==ObstacleKind.LOG) JumpKind.LONG else JumpKind.SHORT)
        val speed=if(s.reducedMotion) 3.25f else 4.35f
        val nx=(s.x+speed*dt).coerceAtMost(s.course.length)
        val vy=s.velocityY-18.5f*dt
        val ny=(s.y+s.velocityY*dt-.5f*18.5f*dt*dt).coerceAtLeast(0f)
        var tokens=s.tokens; var bumps=s.bumps; var collected=s.collectedTokenIds; var passed=s.passedObstacleIds; var feedback=s.feedback
        s.course.tokens.filter { it.id !in collected && it.x in s.x..(nx+.28f) && abs(ny-it.height)<1.2f }.forEach { tokens++; collected=collected+it.id; feedback="Pawprint collected!" }
        s.course.obstacles.filter { it.id !in passed && it.x in s.x..(nx+.32f) }.forEach {
            passed=passed+it.id
            if(ny<it.kind.clearance) { bumps++; feedback="Soft landing! Try jumping a little earlier." } else feedback="Great jump!"
        }
        val finished=nx>=s.course.length
        return s.copy(x=nx,y=ny,velocityY=if(ny==0f)0f else vy,tokens=tokens,bumps=bumps,collectedTokenIds=collected,passedObstacleIds=passed,
            phase=if(finished)ParkourPhase.ROUND_COMPLETE else ParkourPhase.RUNNING,
            roundsCompleted=if(finished)s.roundsCompleted+1 else s.roundsCompleted,
            feedback=if(finished)"Trail complete! Milo reached the finish flag." else feedback)
    }

    fun nextCourse(s:ParkourState):ParkourState {
        if(s.phase!=ParkourPhase.ROUND_COMPLETE) return s
        val index=Math.floorMod(courses.indexOfFirst { it.id==s.course.id }+1,courses.size)
        return ParkourState(courses[index],phase=ParkourPhase.READY,tokens=s.tokens,bumps=s.bumps,roundsCompleted=s.roundsCompleted,
            feedback="${courses[index].title} is ready!",assistedMode=s.assistedMode,reducedMotion=s.reducedMotion)
    }
    fun setAssisted(s:ParkourState,on:Boolean)=s.copy(assistedMode=on,feedback=if(on)"Assisted jumps are on." else "You control every jump.")
    fun setReducedMotion(s:ParkourState,on:Boolean)=s.copy(reducedMotion=on,feedback=if(on)"Calm motion is on." else s.feedback)
}
