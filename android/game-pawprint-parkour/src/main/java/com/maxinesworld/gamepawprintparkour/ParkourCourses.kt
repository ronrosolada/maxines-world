package com.maxinesworld.gamepawprintparkour

object ParkourCourses {
    val rooftops = course("rooftops", "Rooftop Pawprints", 48f, listOf(8f to ObstacleKind.PUDDLE, 17f to ObstacleKind.HAY, 27f to ObstacleKind.LOG, 38f to ObstacleKind.HAY))
    val garden = course("garden", "Garden Bridges", 54f, listOf(7f to ObstacleKind.HAY, 15f to ObstacleKind.PUDDLE, 24f to ObstacleKind.LOG, 35f to ObstacleKind.PUDDLE, 44f to ObstacleKind.LOG))
    val canopy = course("canopy", "Tree Canopy Trail", 60f, listOf(9f to ObstacleKind.LOG, 18f to ObstacleKind.HAY, 29f to ObstacleKind.LOG, 40f to ObstacleKind.PUDDLE, 50f to ObstacleKind.HAY))
    val all = listOf(rooftops, garden, canopy)
    private fun course(id:String,title:String,length:Float, obstacles:List<Pair<Float,ObstacleKind>>):ParkourCourse {
        val os=obstacles.mapIndexed { i,p -> CourseObstacle("$id-o$i",p.first,p.second) }
        val ts=(4 until length.toInt() step 4).mapIndexed { i,x -> CourseToken("$id-t$i",x.toFloat(), if(i%3==2) 1.4f else .65f) }
        return ParkourCourse(id,title,length,os,ts)
    }
}
