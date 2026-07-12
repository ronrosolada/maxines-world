package com.maxinesworld.gamepawprintparkour

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxinesworld.engineminigame.RewardBreakClock
import kotlinx.coroutines.delay

private val Teal=Color(0xFF087F83);private val Coral=Color(0xFFF47C6B);private val Gold=Color(0xFFF5B82E);private val Cream=Color(0xFFFFF7E8);private val Ink=Color(0xFF183B4A)
@Composable fun PawprintParkourScreen(childId:String,rewardBreakId:String,modifier:Modifier=Modifier,durationMillis:Long=RewardBreakClock.DEFAULT_DURATION_MILLIS,onExit:(ParkourResult)->Unit,viewModel:ParkourViewModel=viewModel(factory=ParkourViewModelFactory(childId,rewardBreakId,durationMillis))){
 val ui by viewModel.state.collectAsState(); val lifecycle=LocalLifecycleOwner.current.lifecycle; var exit by remember{mutableStateOf(false)};val ctx=LocalContext.current;val sounds=remember{ParkourSoundPlayer(ctx.applicationContext)};DisposableEffect(sounds){onDispose{sounds.close()}}
 DisposableEffect(lifecycle){val o=LifecycleEventObserver{_,e->when(e){Lifecycle.Event.ON_RESUME->viewModel.resume();Lifecycle.Event.ON_PAUSE->viewModel.pause();else->Unit}};lifecycle.addObserver(o);onDispose{lifecycle.removeObserver(o)}};BackHandler{exit=true}
 var lastTokens by remember{mutableIntStateOf(0)};var lastBumps by remember{mutableIntStateOf(0)};var lastPhase by remember{mutableStateOf(ui.game.phase)}
 LaunchedEffect(ui.game.tokens,ui.game.bumps,ui.game.phase){if(ui.soundEnabled){if(ui.game.tokens>lastTokens)sounds.token();if(ui.game.bumps>lastBumps)sounds.bump();if(lastPhase!=ParkourPhase.ROUND_COMPLETE&&ui.game.phase==ParkourPhase.ROUND_COMPLETE)sounds.finish()};lastTokens=ui.game.tokens;lastBumps=ui.game.bumps;lastPhase=ui.game.phase}
 LaunchedEffect(ui.breakExpired,ui.game.phase){if(ui.breakExpired&&ui.game.phase!=ParkourPhase.RUNNING){delay(1200);onExit(viewModel.result())}}
 Box(modifier.fillMaxSize()){Image(painterResource(R.drawable.parkour_background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.08f)))
  val wide=LocalConfiguration.current.screenWidthDp>=840
  if(wide)Row(Modifier.fillMaxSize().padding(24.dp),horizontalArrangement=Arrangement.spacedBy(20.dp)){Stage(ui,viewModel,{if(ui.soundEnabled)sounds.jump()},Modifier.weight(3f).fillMaxHeight());Side(ui,viewModel,{exit=true},Modifier.widthIn(300.dp,390.dp).fillMaxHeight())}
  else Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Header(ui,{exit=true},viewModel);Stage(ui,viewModel,{if(ui.soundEnabled)sounds.jump()},Modifier.weight(1f).fillMaxWidth())}
  if(ui.paused)PauseOverlay(viewModel::resume);if(exit)ExitDialog({exit=false}){onExit(viewModel.result())}
 }
}
@Composable private fun Stage(ui:ParkourUiState,vm:ParkourViewModel,onJumpSound:()->Unit,modifier:Modifier){Card(modifier,shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(Cream.copy(alpha=.94f))){Column(Modifier.fillMaxSize().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
 Text(ui.game.course.title,color=Ink,fontSize=26.sp,fontWeight=FontWeight.ExtraBold);LinearProgressIndicator({ui.game.progress},Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),color=Gold,trackColor=Color.White)
 Text(ui.game.feedback,color=Ink,fontSize=18.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center,modifier=Modifier.padding(8.dp).semantics{contentDescription=ui.game.feedback})
 CourseView(ui.game,Modifier.weight(1f).fillMaxWidth())
 when(ui.game.phase){ParkourPhase.READY->Button({vm.start()},Modifier.heightIn(min=64.dp).fillMaxWidth(.65f),colors=ButtonDefaults.buttonColors(Teal)){Icon(Icons.Default.PlayArrow,null);Spacer(Modifier.width(8.dp));Text("Start trail",fontSize=21.sp)}
  ParkourPhase.RUNNING->Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){Button({vm.shortJump();onJumpSound()},Modifier.heightIn(min=64.dp).weight(1f),colors=ButtonDefaults.buttonColors(Coral)){Icon(Icons.Default.KeyboardArrowUp,null);Text(" Jump",fontSize=21.sp)};OutlinedButton({vm.longJump();onJumpSound()},Modifier.heightIn(min=64.dp).weight(1f)){Icon(Icons.Default.North,null);Text(" Big jump",fontSize=20.sp)}}
  ParkourPhase.ROUND_COMPLETE->Button({vm.nextCourse()},enabled=!ui.breakExpired,modifier=Modifier.heightIn(min=64.dp).fillMaxWidth(.65f),colors=ButtonDefaults.buttonColors(Teal)){Text(if(ui.breakExpired)"Break complete" else "Next trail",fontSize=21.sp)}}
 }}}
@Composable private fun CourseView(s:ParkourState,modifier:Modifier){BoxWithConstraints(modifier.clip(RoundedCornerShape(22.dp)).background(Color(0x553C9DDB))){val viewStart=(s.x-5f).coerceAtLeast(0f);val span=16f;val px={x:Float->maxWidth*((x-viewStart)/span)};val ground=maxHeight*.78f
 s.course.tokens.filter{it.id !in s.collectedTokenIds&&it.x in viewStart..(viewStart+span)}.forEach{t->Image(painterResource(R.drawable.token_paw),"Glowing pawprint",Modifier.offset(px(t.x)-24.dp,ground-((t.height*44).dp)-24.dp).size(48.dp))}
 s.course.obstacles.filter{it.id !in s.passedObstacleIds&&it.x in viewStart..(viewStart+span)}.forEach{o->val dr=when(o.kind){ObstacleKind.PUDDLE->R.drawable.obstacle_puddle;ObstacleKind.HAY->R.drawable.obstacle_hay;ObstacleKind.LOG->R.drawable.obstacle_log};Image(painterResource(dr),o.kind.name.lowercase()+" obstacle",Modifier.offset(px(o.x)-38.dp,ground-42.dp).size(76.dp),contentScale=ContentScale.Fit)}
 val char=when{ s.phase==ParkourPhase.ROUND_COMPLETE->R.drawable.milo_celebrate;s.y>.9f->R.drawable.milo_jump_long;s.y>.1f->R.drawable.milo_jump_short;else->R.drawable.milo_run_1};Image(painterResource(char),"Milo running; ${s.tokens} pawprints collected",Modifier.align(Alignment.TopStart).offset(maxWidth*.28f,ground-(s.y*52).dp-105.dp).size(112.dp),contentScale=ContentScale.Fit)
 }}
@Composable private fun Side(ui:ParkourUiState,vm:ParkourViewModel,onExit:()->Unit,modifier:Modifier){Card(modifier,shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(Teal.copy(alpha=.96f))){Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("Pawprint Parkour",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.ExtraBold);Text(time(ui.remainingMillis),color=Color.White,fontSize=34.sp,fontWeight=FontWeight.Bold);LinearProgressIndicator({(ui.remainingMillis.toFloat()/ui.durationMillis).coerceIn(0f,1f)},Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),color=Gold)
 Image(painterResource(if(ui.game.phase==ParkourPhase.ROUND_COMPLETE)R.drawable.milo_celebrate else R.drawable.milo_encourage),"Milo the parkour guide",Modifier.height(210.dp),contentScale=ContentScale.Fit);Text("${ui.game.tokens} pawprints",color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold);Text("${ui.game.roundsCompleted} trails complete",color=Color.White.copy(alpha=.9f),fontSize=17.sp);Spacer(Modifier.weight(1f))
 Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){ToggleIcon(ui.game.assistedMode,Icons.Default.AccessibilityNew,"Assisted jumps",vm::toggleAssist);ToggleIcon(ui.game.reducedMotion,Icons.Default.MotionPhotosOff,"Calm motion",vm::toggleReducedMotion);ToggleIcon(ui.soundEnabled,if(ui.soundEnabled)Icons.Default.VolumeUp else Icons.Default.VolumeOff,"Sound",vm::toggleSound);ToggleIcon(false,Icons.Default.Pause,"Pause",vm::pause);ToggleIcon(false,Icons.Default.ExitToApp,"Leave",onExit)}
 if(ui.breakExpired)Text("Finish this trail, then Milo will return to the village.",color=Color.White,textAlign=TextAlign.Center,modifier=Modifier.padding(top=8.dp))}}}
@Composable private fun ToggleIcon(active:Boolean,icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,click:()->Unit){IconButton(click,Modifier.size(54.dp).background(if(active)Gold else Color.White.copy(alpha=.16f),CircleShape)){Icon(icon,label,tint=if(active)Ink else Color.White)}}
@Composable private fun Header(ui:ParkourUiState,onExit:()->Unit,vm:ParkourViewModel){Surface(color=Teal,shape=RoundedCornerShape(20.dp)){Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){Text("Pawprint Parkour",Modifier.weight(1f),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold);Text(time(ui.remainingMillis),color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold);IconButton(vm::toggleAssist){Icon(Icons.Default.AccessibilityNew,"Assisted jumps",tint=Color.White)};IconButton(vm::pause){Icon(Icons.Default.Pause,"Pause",tint=Color.White)};IconButton(onExit){Icon(Icons.Default.Close,"Leave",tint=Color.White)}}}}
@Composable private fun PauseOverlay(resume:()->Unit){Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.72f)),contentAlignment=Alignment.Center){Card(shape=RoundedCornerShape(28.dp)){Column(Modifier.padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Pause,null,Modifier.size(54.dp),tint=Teal);Text("Trail paused",fontSize=28.sp,fontWeight=FontWeight.Bold);Button(resume){Text("Continue",fontSize=19.sp)}}}}}
@Composable private fun ExitDialog(stay:()->Unit,leave:()->Unit){AlertDialog(stay,title={Text("Leave the trail?")},text={Text("Collected pawprints and completed trails will be saved.")},confirmButton={Button(leave){Text("Save and leave")}},dismissButton={OutlinedButton(stay){Text("Keep playing")}})}
private fun time(ms:Long):String{val s=(ms/1000).coerceAtLeast(0);return "%d:%02d".format(s/60,s%60)}
