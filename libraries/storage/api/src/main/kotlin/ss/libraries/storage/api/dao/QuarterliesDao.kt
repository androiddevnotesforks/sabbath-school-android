/*
 * Copyright (c) 2025. Adventech <info@adventech.io>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ss.libraries.storage.api.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import app.ss.models.OfflineState
import ss.libraries.storage.api.entity.QuarterlyEntity
import ss.libraries.storage.api.entity.QuarterlyInfoEntity

@Dao
interface QuarterliesDao : BaseDao<QuarterlyEntity> {

    @Transaction
    @Query("SELECT * FROM quarterlies WHERE quarterlies.`index` = :quarterlyIndex")
    fun getInfo(quarterlyIndex: String): QuarterlyInfoEntity?

    @Query("SELECT offlineState FROM quarterlies WHERE quarterlies.`index` = :index")
    suspend fun getOfflineState(index: String): OfflineState?

    @Query("UPDATE quarterlies SET offlineState = :state WHERE quarterlies.`index` = :index")
    suspend fun setOfflineState(index: String, state: OfflineState)
}
