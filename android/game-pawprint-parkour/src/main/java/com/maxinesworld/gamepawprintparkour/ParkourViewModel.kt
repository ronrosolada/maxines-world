package com.maxinesworld.gamepawprintparkour

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maxinesworld.engineminigame.RewardBreakClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ParkourUiState(val game:ParkourState,val remainingMillis:Long,val durationMillis:Long,val breakExpired:Boolean=false,val paused:Boolean=false,val soundEnabled:Boolean=true)
class ParkourViewModel(
    private val childId:String, private val rewardBreakId:String, private val durationMillis:Long,
    private val wallTime:()->Long=System::currentTimeMillis, monotonic:()->Long=SystemClock::elapsedRealtime
):ViewModel(){
    private val startedAt=wallTime(); private val engine=ParkourEngine(); private val clock=RewardBreakClock(durationMillis,monotonic)
    private val _state=MutableStateFlow(ParkourUiState(engine.initial(rewardBreakId.hashCode()),durationMillis,durationMillis)); val state=_state.asStateFlow()
    init { clock.resume(); viewModelScope.launch { var previous=monotonic(); while(isActive){ delay(16); val now=monotonic(); val dt=(now-previous)/1000f; previous=now
        _state.update { u -> val rem=clock.remainingMillis(); val active=!u.paused && u.game.phase==ParkourPhase.RUNNING; u.copy(game=if(active)engine.tick(u.game,dt) else u.game,remainingMillis=rem,breakExpired=rem==0L) }
    } } }
    fun start()=_state.update{it.copy(game=engine.start(it.game))}; fun shortJump()=_state.update{it.copy(game=engine.jump(it.game,JumpKind.SHORT))}; fun longJump()=_state.update{it.copy(game=engine.jump(it.game,JumpKind.LONG))}
    fun nextCourse()=_state.update{if(it.breakExpired)it else it.copy(game=engine.nextCourse(it.game))}
    fun toggleAssist()=_state.update{it.copy(game=engine.setAssisted(it.game,!it.game.assistedMode))}; fun toggleReducedMotion()=_state.update{it.copy(game=engine.setReducedMotion(it.game,!it.game.reducedMotion))}
    fun toggleSound()=_state.update{it.copy(soundEnabled=!it.soundEnabled)}
    fun pause(){clock.pause();_state.update{it.copy(paused=true)}}; fun resume(){clock.resume();_state.update{it.copy(paused=false)}}
    fun result():ParkourResult { val g=state.value.game; return ParkourResult(rewardBreakId,childId,startedAt,wallTime(),g.roundsCompleted,g.tokens,g.bumps,(g.tokens/3+g.roundsCompleted).coerceAtMost(10),if(g.roundsCompleted>=2)"milo-teal-trail" else null) }
}
class ParkourViewModelFactory(private val childId:String,private val rewardBreakId:String,private val durationMillis:Long):ViewModelProvider.Factory{
    @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=ParkourViewModel(childId,rewardBreakId,durationMillis) as T
}
