package com.example.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.data.local.ProjectEntity
import com.example.ui.components.ManageCategoriesDialog
import com.example.ui.theme.SurveyPrimaryAmber
import com.example.util.GpsData

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SurveyFormSheet(
    sheetState: SheetState,
    projects: List<ProjectEntity>,
    categories: List<CategoryEntity>,
    selectedProject: String,
    surveyNote: String,
    surveyVillage: String,
    surveyDistrict: String,
    surveyRegency: String,
    gpsData: GpsData,
    onProjectSelected: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onLocationOverridesChanged: (village: String, district: String, regency: String) -> Unit,
    onAddNewProject: (name: String, desc: String, category: String, colorHex: String) -> Unit,
    onAddCategory: (String) -> Unit,
    onEditCategory: (CategoryEntity, String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var noteInput by remember(surveyNote) { mutableStateOf(surveyNote) }
    var villageInput by remember(surveyVillage) { mutableStateOf(surveyVillage) }
    var districtInput by remember(surveyDistrict) { mutableStateOf(surveyDistrict) }
    var regencyInput by remember(surveyRegency) { mutableStateOf(surveyRegency) }

    var showManageCategoriesDialog by remember { mutableStateOf(false) }

    // Categories calculation from database entities
    val availableCategoryNames = remember(categories, projects) {
        val fromDb = categories.map { it.name }.filter { it.isNotBlank() }
        val fromProjects = projects.map { it.category }.filter { it.isNotBlank() }
        val combined = (fromDb + fromProjects).distinct()
        listOf("Semua") + if (combined.isEmpty()) listOf("Pajak & Retribusi", "Infrastruktur & PU", "Umum") else combined
    }

    // Auto-select category matching current selected project if possible
    val initialCategory = remember(projects, selectedProject) {
        val currentProj = projects.find { it.name == selectedProject }
        currentProj?.category ?: "Semua"
    }

    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }

    // Revert to "Semua" if selected category was deleted
    if (selectedCategory != "Semua" && !availableCategoryNames.contains(selectedCategory)) {
        selectedCategory = "Semua"
    }

    val filteredProjects = remember(projects, selectedCategory) {
        if (selectedCategory == "Semua") {
            projects
        } else {
            projects.filter { it.category == selectedCategory }
        }
    }

    var isAddingProject by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var newProjectDesc by remember { mutableStateOf("") }
    var newProjectCategory by remember { mutableStateOf(if (selectedCategory != "Semua") selectedCategory else "Pajak & Retribusi") }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.NoteAlt,
                    contentDescription = null,
                    tint = SurveyPrimaryAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Form Survei Lapangan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pilih kategori, proyek, & objek survei",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // STEP 1: PILIHAN KATEGORI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = SurveyPrimaryAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1. Pilih Kategori Kegiatan",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Kelola Kategori Button
                    TextButton(
                        onClick = { showManageCategoriesDialog = true },
                        modifier = Modifier.testTag("manage_categories_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kelola", fontSize = 12.sp, color = SurveyPrimaryAmber)
                    }

                    // Tambah Proyek Button
                    TextButton(
                        onClick = { isAddingProject = !isAddingProject },
                        modifier = Modifier.testTag("toggle_add_project_btn")
                    ) {
                        Icon(
                            imageVector = if (isAddingProject) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(if (isAddingProject) "Tutup" else "+ Proyek", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Chips FlowRow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCategoryNames.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            val icon = when (category) {
                                "Pajak & Retribusi" -> Icons.Default.AccountBalance
                                "Infrastruktur & PU" -> Icons.Default.Construction
                                "Pengawasan & Audit" -> Icons.Default.Security
                                else -> Icons.Default.Folder
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SurveyPrimaryAmber,
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        )
                    )
                }

                // Quick add category action chip
                FilterChip(
                    selected = false,
                    onClick = { showManageCategoriesDialog = true },
                    label = { Text("+ Kategori", fontSize = 12.sp, color = SurveyPrimaryAmber) },
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = null, tint = SurveyPrimaryAmber, modifier = Modifier.size(14.dp))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create New Project Form Expandable
            if (isAddingProject) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Tambah Proyek Baru",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SurveyPrimaryAmber
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Category picker for new project
                        Box {
                            OutlinedTextField(
                                value = newProjectCategory,
                                onValueChange = { newProjectCategory = it },
                                label = { Text("Kategori Proyek") },
                                placeholder = { Text("Pajak & Retribusi / Infrastruktur...") },
                                singleLine = true,
                                trailingIcon = {
                                    TextButton(onClick = { showCategoryDropdown = true }) {
                                        Text("Pilih", fontSize = 12.sp, color = SurveyPrimaryAmber)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_project_category_input")
                            )

                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                availableCategoryNames.filter { it != "Semua" }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            newProjectCategory = cat
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newProjectName,
                            onValueChange = { newProjectName = it },
                            label = { Text("Nama Proyek / Kegiatan") },
                            placeholder = { Text("Contoh: Pendataan Reklame") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_project_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SurveyPrimaryAmber
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newProjectDesc,
                            onValueChange = { newProjectDesc = it },
                            label = { Text("Deskripsi (Opsional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (newProjectName.isNotBlank()) {
                                    val cat = newProjectCategory.ifBlank { "Umum" }
                                    onAddNewProject(newProjectName, newProjectDesc, cat, "#F59E0B")
                                    selectedCategory = cat
                                    newProjectName = ""
                                    newProjectDesc = ""
                                    isAddingProject = false
                                }
                            },
                            enabled = newProjectName.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_new_project_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SurveyPrimaryAmber)
                        ) {
                            Text("Simpan Proyek", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // STEP 2: PILIHAN NAMA PROYEK / KEGIATAN
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = SurveyPrimaryAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "2. Pilih Nama Proyek / Kegiatan (${filteredProjects.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Project Chips
            if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada proyek di kategori '$selectedCategory'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredProjects.forEach { project ->
                        val isSelected = project.name == selectedProject
                        val chipColor = try {
                            Color(android.graphics.Color.parseColor(project.colorHex))
                        } catch (_: Exception) {
                            SurveyPrimaryAmber
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .testTag("project_chip_${project.name}")
                                .background(
                                    if (isSelected) chipColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) chipColor else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onProjectSelected(project.name) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = project.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // STEP 3: KETERANGAN OBJEK / NO. ASET
            Text(
                text = "Keterangan Objek / Nomor Aset",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = noteInput,
                onValueChange = {
                    noteInput = it
                    onNoteChanged(it)
                },
                placeholder = { Text("Contoh: Kios Blok B No. 04, Tiang 34, Jembatan P1") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("survey_note_input"),
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SurveyPrimaryAmber
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Location Administration (Desa, Kecamatan, Kabupaten)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lokasi Administrasi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (gpsData.village.isNotBlank() || gpsData.district.isNotBlank()) {
                    TextButton(
                        onClick = {
                            villageInput = gpsData.village
                            districtInput = gpsData.district
                            regencyInput = gpsData.regency
                            onLocationOverridesChanged(villageInput, districtInput, regencyInput)
                        }
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Isi dari GPS", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = villageInput,
                onValueChange = {
                    villageInput = it
                    onLocationOverridesChanged(villageInput, districtInput, regencyInput)
                },
                label = { Text("Desa / Kelurahan") },
                placeholder = { Text(gpsData.village.ifBlank { "Desa/Kelurahan" }) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("survey_village_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = districtInput,
                    onValueChange = {
                        districtInput = it
                        onLocationOverridesChanged(villageInput, districtInput, regencyInput)
                    },
                    label = { Text("Kecamatan") },
                    placeholder = { Text(gpsData.district.ifBlank { "Kecamatan" }) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("survey_district_input")
                )
                OutlinedTextField(
                    value = regencyInput,
                    onValueChange = {
                        regencyInput = it
                        onLocationOverridesChanged(villageInput, districtInput, regencyInput)
                    },
                    label = { Text("Kabupaten") },
                    placeholder = { Text(gpsData.regency.ifBlank { "Kabupaten" }) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("survey_regency_input")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_survey_form_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = SurveyPrimaryAmber),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Terapkan Pada Kamera",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }

    // Manage Categories Dialog (Tambah, Edit, Hapus)
    if (showManageCategoriesDialog) {
        ManageCategoriesDialog(
            categories = categories,
            projects = projects,
            onAddCategory = onAddCategory,
            onEditCategory = onEditCategory,
            onDeleteCategory = onDeleteCategory,
            onDismiss = { showManageCategoriesDialog = false }
        )
    }
}
