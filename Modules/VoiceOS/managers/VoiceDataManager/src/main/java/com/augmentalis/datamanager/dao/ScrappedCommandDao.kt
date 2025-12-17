// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.ScrappedCommand

@Dao
interface ScrappedCommandDao : BaseDao<ScrappedCommand> {
    
    @Query("SELECT * FROM scrapped_command")
    suspend fun getAll(): List<ScrappedCommand>
    
    @Query("SELECT * FROM scrapped_command WHERE id = :id")
    suspend fun getById(id: Long): ScrappedCommand?
    
    @Query("DELETE FROM scrapped_command WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM scrapped_command")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM scrapped_command")
    suspend fun count(): Long

}