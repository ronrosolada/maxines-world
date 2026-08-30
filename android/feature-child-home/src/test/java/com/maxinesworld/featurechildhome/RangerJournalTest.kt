package com.maxinesworld.featurechildhome
import org.junit.Assert.*
import org.junit.Test
class RangerJournalTest {
 private class MemoryStore:RangerJournalStore{val data=mutableMapOf<String,String>();override fun read(childId:String)=data[childId];override fun write(childId:String,value:String){data[childId]=value}}
 @Test fun snapshotSavingAddsPawprintAndCanBeRetrieved(){val repo=RangerJournalRepository(MemoryStore());repo.saveSnapshot(RangerSnapshot("1","maxine","My sanctuary",100,"Dusk",listOf("philippine_tarsier"),listOf("⭐"),"Curious"));val j=repo.journal("maxine");assertEquals(1,j.snapshots.size);assertEquals("philippine_tarsier",j.stamps.single().speciesId);assertEquals("night-owl",j.badges.single().id)}
 @Test fun pawprintStampIsIdempotent(){val repo=RangerJournalRepository(MemoryStore());val s=PawprintStamp("eagle","Philippine Eagle",5);repo.addStamp("c",s);repo.addStamp("c",s);assertEquals(1,repo.journal("c").stamps.size)}
}
