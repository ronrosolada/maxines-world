package com.maxinesworld.gametarsiercanopy
import androidx.lifecycle.ViewModel
import com.maxinesworld.engineminigame.MiniGameResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
class TarsierCanopyViewModel(private val childId:String="child",private val rewardBreakId:String="break",private val now:()->Long=System::currentTimeMillis):ViewModel(){private val engine=TarsierCanopyEngine();private val started=now();private val _state=MutableStateFlow(CanopyState());val state=_state.asStateFlow();fun charge(dt:Float)=_state.update{engine.charge(it,dt)};fun leap()=_state.update(engine::leap);fun tick(dt:Float)=_state.update{engine.tick(it,dt)};fun reset()=_state.update(engine::resetAfterBounce);fun collectFirefly()=_state.update(engine::collectFirefly);fun collectFig()=_state.update(engine::collectFig);fun hitThorn()=_state.update(engine::thorn);fun result()=MiniGameResult(rewardBreakId=rewardBreakId,gameId="tarsier-canopy",childId=childId,startedAtEpochMillis=started,endedAtEpochMillis=now(),roundsCompleted=if(_state.value.phase==CanopyPhase.COMPLETE)1 else 0,correctOrders=_state.value.fireflies,pawTokensEarned=(_state.value.fireflies*2+_state.value.figs).coerceAtMost(10))}
