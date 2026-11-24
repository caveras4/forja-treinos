package com.fitforge.treinos.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fitforge.treinos.data.local.AppDatabase
import com.fitforge.treinos.data.local.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migracao1Para2PreservaOsTreinos() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO workouts (id, name, focus, colorKey, orderIndex, updatedAt) " +
                    "VALUES (1, 'Treino A', 'Peito', 'emerald', 0, 0)"
            )
            close()
        }

        // validateDroppedTables = true: o schema resultante precisa bater com o 2.json.
        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)
        db.query("SELECT name FROM workouts").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Treino A", cursor.getString(0))
        }
        db.query("SELECT COUNT(*) FROM workout_exercises").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }
}
