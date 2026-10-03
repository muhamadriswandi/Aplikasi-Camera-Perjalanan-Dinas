package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.local.CategoryEntity
import com.example.data.local.PhotoDao
import com.example.data.local.PhotoEntity
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class SurveyRepository(
    private val photoDao: PhotoDao,
    private val projectDao: ProjectDao,
    private val categoryDao: CategoryDao
) {
    val allPhotos: Flow<List<PhotoEntity>> = photoDao.getAllPhotos()
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val projectNames: Flow<List<String>> = photoDao.getDistinctProjectNames()

    fun getPhotosByProject(projectName: String): Flow<List<PhotoEntity>> =
        photoDao.getPhotosByProject(projectName)

    fun getPhotosByDate(dateFormatted: String): Flow<List<PhotoEntity>> =
        photoDao.getPhotosByDate(dateFormatted)

    fun getPhotosByMonth(year: Int, month: Int): Flow<List<PhotoEntity>> =
        photoDao.getPhotosByMonth(year, month)

    fun getPhotosByYear(year: Int): Flow<List<PhotoEntity>> =
        photoDao.getPhotosByYear(year)

    fun searchPhotos(query: String): Flow<List<PhotoEntity>> =
        photoDao.searchPhotos(query)

    suspend fun getPhotoById(id: Long): PhotoEntity? =
        photoDao.getPhotoById(id)

    suspend fun insertPhoto(photo: PhotoEntity): Long =
        photoDao.insertPhoto(photo)

    suspend fun updatePhoto(photo: PhotoEntity) =
        photoDao.updatePhoto(photo)

    suspend fun deletePhoto(photo: PhotoEntity) {
        photoDao.deletePhoto(photo)
        try {
            val file = File(photo.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun deletePhotosByIds(photos: List<PhotoEntity>) {
        photoDao.deletePhotosByIds(photos.map { it.id })
        photos.forEach { photo ->
            try {
                val file = File(photo.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun movePhotosToProject(ids: List<Long>, newProjectName: String) =
        photoDao.movePhotosToProject(ids, newProjectName)

    suspend fun insertProject(project: ProjectEntity): Long =
        projectDao.insertProject(project)

    suspend fun deleteProject(project: ProjectEntity) =
        projectDao.deleteProject(project)

    suspend fun deleteProjectByName(projectName: String, deletePhotos: Boolean) {
        val photos = photoDao.getPhotosByProjectSync(projectName)
        if (deletePhotos) {
            deletePhotosByIds(photos)
        } else if (photos.isNotEmpty()) {
            photoDao.movePhotosToProject(photos.map { it.id }, "Umum")
        }
        projectDao.deleteProjectByName(projectName)
    }

    suspend fun updateProject(project: ProjectEntity) =
        projectDao.updateProject(project)

    // Category Operations (Tambah, Edit, Hapus)
    suspend fun insertCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity, oldName: String) {
        categoryDao.updateCategory(category)
        if (oldName.isNotBlank() && oldName != category.name) {
            categoryDao.updateProjectCategoryName(oldName, category.name)
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
        categoryDao.resetProjectsCategoryToUmum(category.name)
    }
}
