package com.maxinesworld.feature.childhome

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

private val Ink = Color(0xFF123E45)
private val English = Color(0xFF7653B5)
private val Filipino = Color(0xFFD95D57)
private val Math = Color(0xFF2189C5)
private val Science = Color(0xFF4C9638)
private val History = Color(0xFFB66D0A)
private val Gmrc = Color(0xFF087F83)

data class VillageDestinationV16(
    val id: String,
    val destination: String,
    val subject: String,
    val progressText: String,
    val accent: Color,
    val centerX: Float,
    val centerY: Float,
    val enabled: Boolean = true,
    val today: Boolean = false,
)

private val DefaultDestinationsV16 = listOf(
    VillageDestinationV16("english", "Story Tree", "Reading", "42%", English, .17f, .43f),
    VillageDestinationV16("filipino", "Bahay ng Kuwento", "Filipino", "21%", Filipino, .54f, .45f),
    VillageDestinationV16("mathematics", "Number Market", "Math", "67%", Math, .86f, .43f, today = true),
    VillageDestinationV16("science", "Discovery Lab", "Science", "17%", Science, .17f, .68f),
    VillageDestinationV16("history", "Heritage Harbor", "Araling Panlipunan", "14%", History, .50f, .75f),
    VillageDestinationV16("gmrc", "Kindness Corner", "Values", "18%", Gmrc, .83f, .75f),
)

@Composable
fun VillageChromeV16(
    modifier: Modifier = Modifier,
    destinations: List<VillageDestinationV16> = DefaultDestinationsV16,
    onDestinationClick: (String) -> Unit,
    profile: @Composable BoxScope.() -> Unit,
    quest: @Composable BoxScope.() -> Unit,
    rewards: @Composable RowScope.() -> Unit,
    bottomNavigation: @Composable RowScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val expanded = maxWidth >= 840.dp
        if (!expanded) {
            CompactVillageChromeV16(destinations, onDestinationClick, profile, quest, rewards, bottomNavigation)
            return@BoxWithConstraints
        }
        Box(Modifier.fillMaxSize()) {
            destinations.forEach { item ->
                SubjectPlaqueV16(
                    item = item,
                    onClick = { onDestinationClick(item.id) },
                    modifier = Modifier.sceneAnchor(item.centerX, item.centerY, 196.dp, 84.dp),
                )
            }
            ArtPanelV16(R.drawable.mw_profile_panel, Modifier.offset(20.dp, 20.dp).size(280.dp,104.dp), profile)
            ArtPanelV16(R.drawable.mw_quest_panel, Modifier.offset(20.dp,132.dp).size(340.dp,104.dp), quest)
            Row(Modifier.align(Alignment.TopEnd).padding(top=20.dp,end=20.dp), horizontalArrangement=Arrangement.spacedBy(8.dp), content=rewards)
            ArtPanelV16(R.drawable.mw_bottom_nav, Modifier.align(Alignment.BottomCenter).padding(horizontal=40.dp,bottom=10.dp).fillMaxWidth().height(84.dp)) {
                Row(Modifier.fillMaxSize().padding(horizontal=26.dp,vertical=16.dp), horizontalArrangement=Arrangement.SpaceEvenly, verticalAlignment=Alignment.CenterVertically, content=bottomNavigation)
            }
        }
    }
}

@Composable
private fun SubjectPlaqueV16(item: VillageDestinationV16, onClick:()->Unit, modifier:Modifier=Modifier) {
    Box(modifier.semantics(mergeDescendants=true) {
        contentDescription = "${item.destination}, ${item.subject}, ${item.progressText}${if(item.today) ", recommended today" else ""}"
        role = androidx.compose.ui.semantics.Role.Button
        if(!item.enabled) disabled()
    }.clickable(enabled=item.enabled,onClick=onClick)) {
        Image(painterResource(R.drawable.mw_subject_plaque),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
        Row(Modifier.fillMaxSize().padding(start=30.dp,end=22.dp,top=22.dp,bottom=20.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(42.dp),contentAlignment=Alignment.Center) { androidx.compose.foundation.Canvas(Modifier.fillMaxSize()){ drawRoundRect(item.accent,cornerRadius=androidx.compose.ui.geometry.CornerRadius(5f,5f)) } }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.Center) {
                androidx.compose.material3.Text(item.destination,color=Ink,fontSize=16.sp,fontWeight=FontWeight.ExtraBold,maxLines=2,lineHeight=17.sp)
                androidx.compose.material3.Text("${item.subject} · ${item.progressText}",color=item.accent,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)
            }
            if(item.today) androidx.compose.material3.Text("TODAY",color=Color(0xFF7A4B00),fontSize=8.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
        }
    }
}

@Composable
private fun ArtPanelV16(@DrawableRes res:Int, modifier:Modifier, content:@Composable BoxScope.()->Unit) = Box(modifier) {
    Image(painterResource(res),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
    Box(Modifier.fillMaxSize().padding(22.dp),content=content)
}

private fun Modifier.sceneAnchor(cx:Float,cy:Float,w:Dp,h:Dp)=this.then(Modifier.layout { measurable,constraints ->
    val p=measurable.measure(Constraints.fixed(w.roundToPx(),h.roundToPx()))
    val x=(constraints.maxWidth*cx-p.width/2).roundToInt().coerceIn(8,constraints.maxWidth-p.width-8)
    val y=(constraints.maxHeight*cy-p.height/2).roundToInt().coerceIn(8,constraints.maxHeight-p.height-96)
    layout(constraints.maxWidth,constraints.maxHeight){ p.placeRelative(x,y) }
})

@Composable
private fun CompactVillageChromeV16(destinations:List<VillageDestinationV16>,onClick:(String)->Unit,profile:@Composable BoxScope.()->Unit,quest:@Composable BoxScope.()->Unit,rewards:@Composable RowScope.()->Unit,bottom:@Composable RowScope.()->Unit) {
    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { ArtPanelV16(R.drawable.mw_profile_panel,Modifier.fillMaxWidth().aspectRatio(2.69f),profile) }
        item { ArtPanelV16(R.drawable.mw_quest_panel,Modifier.fillMaxWidth().aspectRatio(3.27f),quest) }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),content=rewards) }
        items(destinations.size){ i -> SubjectPlaqueV16(destinations[i],{onClick(destinations[i].id)},Modifier.fillMaxWidth().heightIn(min=84.dp)) }
        item { ArtPanelV16(R.drawable.mw_bottom_nav,Modifier.fillMaxWidth().height(84.dp)){ Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically,content=bottom) } }
    }
}
