package com.maxinesworld.gametarsiercanopy
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
class TarsierSoundPlayer(context:Context){private val tone=ToneGenerator(AudioManager.STREAM_MUSIC,35);fun leap()=tone.startTone(ToneGenerator.TONE_PROP_BEEP,80);fun firefly()=tone.startTone(ToneGenerator.TONE_PROP_ACK,100);fun rustle()=tone.startTone(ToneGenerator.TONE_PROP_BEEP2,60);fun close()=tone.release()}
@Composable fun TarsierCanopyScreen(childId:String,rewardBreakId:String,onExit:(com.maxinesworld.engineminigame.MiniGameResult)->Unit){val vm:TarsierCanopyViewModel=viewModel(factory=object:androidx.lifecycle.ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST")override fun<T:androidx.lifecycle.ViewModel>create(c:Class<T>)=TarsierCanopyViewModel(childId,rewardBreakId) as T});val s by vm.state.collectAsState();val haptic=LocalHapticFeedback.current;val context=androidx.compose.ui.platform.LocalContext.current;val sound=remember(context){TarsierSoundPlayer(context)};DisposableEffect(Unit){onDispose{sound.close()}};LaunchedEffect(s.phase){while(s.phase==CanopyPhase.AIRBORNE){delay(16);vm.tick(.016f)}}
 Column(Modifier.fillMaxSize()){Text("Tarsier Canopy Jump",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(12.dp));Text("Fireflies ${s.fireflies}  •  Figs ${s.figs}  •  ${s.message}",Modifier.padding(horizontal=12.dp));Canvas(Modifier.weight(1f).fillMaxWidth().semantics{contentDescription="Starry Philippine canopy. Hold then release to leap."}.pointerInput(Unit){detectTapGestures(onPress={vm.charge(.65f);tryAwaitRelease();vm.leap();sound.leap();haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)})}){drawRect(Color(0xFF10183D));repeat(30){i->drawCircle(Color(0xFFFFEE88),2f,Offset((i*73%size.width.toInt()).toFloat(),(i*47%(size.height*.7f).toInt()).toFloat()))};drawLine(Color(0xFF57945D),Offset(size.width*.08f,size.height*.78f),Offset(size.width*.35f,size.height*.25f),20f);drawLine(Color(0xFF6A8F55),Offset(size.width*.62f,size.height*.7f),Offset(size.width*.93f,size.height*.18f),22f);drawCircle(Color(0xFFD9B07A),28f,Offset(size.width*s.x,size.height*s.y));drawCircle(Color(0xFF8D57B5),42f,Offset(size.width*.5f,size.height*.94f));drawCircle(Color(0xFFFFFF72),8f,Offset(size.width*.72f,size.height*.42f))};Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceEvenly){Button({vm.collectFirefly();sound.firefly()}){Text("Catch Firefly")};if(s.phase==CanopyPhase.BOUNCING)Button(vm::reset){Text("Try again")};Button({onExit(vm.result())}){Text("Finish")}} }
}
