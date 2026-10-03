package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [PhotoEntity::class, ProjectEntity::class, CategoryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class GeoCameraDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun projectDao(): ProjectDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: GeoCameraDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): GeoCameraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeoCameraDatabase::class.java,
                    "geocamera_survey_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            INSTANCE?.let { database ->
                                // Seed default categories
                                val defaultCategories = listOf(
                                    CategoryEntity(name = "Pajak & Retribusi", colorHex = "#F59E0B"),
                                    CategoryEntity(name = "Infrastruktur & PU", colorHex = "#0EA5E9"),
                                    CategoryEntity(name = "Pengawasan & Audit", colorHex = "#64748B"),
                                    CategoryEntity(name = "Pertanahan & Aset", colorHex = "#10B981"),
                                    CategoryEntity(name = "Umum", colorHex = "#8B5CF6")
                                )
                                defaultCategories.forEach { database.categoryDao().insertCategory(it) }

                                // Seed default projects with categories
                                val defaultProjects = listOf(
                                    ProjectEntity(
                                        name = "Pendataan Retribusi Pasar",
                                        description = "Dokumentasi kios, los, dan pedagang pasar daerah",
                                        category = "Pajak & Retribusi",
                                        colorHex = "#F59E0B"
                                    ),
                                    ProjectEntity(
                                        name = "Pendataan Bangunan (PBB)",
                                        description = "Survei PBB-P2, izin mendirikan bangunan, & tata ruang",
                                        category = "Pajak & Retribusi",
                                        colorHex = "#8B5CF6"
                                    ),
                                    ProjectEntity(
                                        name = "Pendataan Reklame & Iklan",
                                        description = "Pajak reklame, billboard, neon box, & spanduk",
                                        category = "Pajak & Retribusi",
                                        colorHex = "#D97706"
                                    ),
                                    ProjectEntity(
                                        name = "Inspeksi Jalan",
                                        description = "Pemantauan kondisi jalan, aspal, dan drainase",
                                        category = "Infrastruktur & PU",
                                        colorHex = "#0EA5E9"
                                    ),
                                    ProjectEntity(
                                        name = "Monitoring Jembatan",
                                        description = "Pemeriksaan struktur fisik dan bentang jembatan",
                                        category = "Infrastruktur & PU",
                                        colorHex = "#10B981"
                                    ),
                                    ProjectEntity(
                                        name = "Survey Irigasi & Bendung",
                                        description = "Inventarisasi saluran primer, sekunder, dan pintu air",
                                        category = "Infrastruktur & PU",
                                        colorHex = "#EC4899"
                                    ),
                                    ProjectEntity(
                                        name = "Audit Fisik Lapangan",
                                        description = "Inspektorat / audit kepatuhan fisik proyek",
                                        category = "Pengawasan & Audit",
                                        colorHex = "#64748B"
                                    )
                                )
                                defaultProjects.forEach { database.projectDao().insertProject(it) }
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
