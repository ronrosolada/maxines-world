package com.maxinesworld.gametarsiercanopy
import com.maxinesworld.engineminigame.MiniGameDefinition
import com.maxinesworld.engineminigame.MiniGameMode
val TarsierCanopyDefinition=MiniGameDefinition("tarsier-canopy","Tarsier Canopy Jump",MiniGameMode.PLAYFUL,45,"tarsier-canopy-v1")

enum class CanopyPhase{READY,CHARGING,AIRBORNE,BOUNCING,COMPLETE}
data class CanopyState(val phase:CanopyPhase=CanopyPhase.READY,val x:Float=.18f,val y:Float=.72f,val vx:Float=0f,val vy:Float=0f,val charge:Float=0f,val fireflies:Int=0,val figs:Int=0,val leaps:Int=0,val message:String="Hold to charge your leap!")
class TarsierCanopyEngine{
 fun charge(s:CanopyState,dt:Float)=if(s.phase in listOf(CanopyPhase.READY,CanopyPhase.CHARGING))s.copy(phase=CanopyPhase.CHARGING,charge=(s.charge+dt*1.2f).coerceAtMost(1f))else s
 fun leap(s:CanopyState)=if(s.phase==CanopyPhase.CHARGING)s.copy(phase=CanopyPhase.AIRBORNE,vx=.28f+s.charge*.22f,vy=-.75f-s.charge*.55f,leaps=s.leaps+1,message="Super leap!")else s
 fun tick(s:CanopyState,dt:Float):CanopyState{if(s.phase!=CanopyPhase.AIRBORNE)return s;val vy=s.vy+1.8f*dt;val x=s.x+s.vx*dt;val y=s.y+vy*dt;return if(y>=.9f)s.copy(phase=CanopyPhase.BOUNCING,x=x.coerceIn(.05f,.95f),y=.88f,vx=0f,vy=0f,charge=0f,message="Try again! Milo is cheering!")else if(x>=.86f)s.copy(phase=CanopyPhase.COMPLETE,x=.86f,y=y,fireflies=s.fireflies+1,message="Canopy crossed!")else s.copy(x=x,y=y,vy=vy)}
 fun resetAfterBounce(s:CanopyState)=if(s.phase==CanopyPhase.BOUNCING)s.copy(phase=CanopyPhase.READY,x=.18f,y=.72f,message="Ready for another gentle leap!")else s
 fun collectFirefly(s:CanopyState)=s.copy(fireflies=s.fireflies+1,message="Firefly glow!")
 fun collectFig(s:CanopyState)=s.copy(figs=s.figs+1,message="Wild fig found!")
 fun thorn(s:CanopyState)=if(s.phase==CanopyPhase.AIRBORNE)s.copy(vx=s.vx*.45f,message="A vine tickled—keep going!")else s
}
