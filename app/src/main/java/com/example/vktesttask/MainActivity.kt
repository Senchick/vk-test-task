package com.example.vktesttask

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vktesttask.data.repository.FolderRepository
import com.example.vktesttask.databinding.ActivityMainBinding
import com.example.vktesttask.model.Folder
import com.example.vktesttask.model.SortingType
import com.example.vktesttask.ui.adapter.FolderAdapter
import com.example.vktesttask.util.getAllFilesFlow
import com.example.vktesttask.util.toFolder
import com.example.vktesttask.viewmodel.FolderViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerViewAdapter: FolderAdapter

    private val viewModel: FolderViewModel by viewModels()

    @Inject
    lateinit var repository: FolderRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupToolbar()

        launchWhenStartedInScope {
            viewModel.data.collect {
                recyclerViewAdapter.updateData(it)
            }
        }

        requestStoragePermission()
    }

    private fun showToast() {
        Toast.makeText(this, "Дай права плиз", Toast.LENGTH_LONG).show()
    }

    private fun requestStoragePermission() {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    fillData()
                } else {
                    showToast()
                }
            }

        val manageStorageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                fillData()
            } else {
                showToast()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                manageStorageLauncher.launch(intent)
            } else {
                fillData()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                fillData()
            }
        }
    }

    private fun fillData() {
        launchWhenStartedInScope {
            val files = getExternalFilesDirs(null)
            val m = mutableListOf<Folder>()

            files.map { it.absolutePath.split("Android/data/")[0] }
                .map { File(it) }
                .forEach {
                    getAllFilesFlow(it).collect {
                        println(it.absolutePath)
                        m.add(it.toFolder())
                    }

                    viewModel.updateData(m)

                    println(it.absolutePath)
                }
        }
    }

    private fun setupToolbar() {
        // setSupportActionBar(binding.toolbar)

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.sorting_button -> {
                    showPopupMenu(binding.toolbar)

                    true
                }

                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerViewAdapter = FolderAdapter()

        binding.recyclerView.apply {
            val divider = ContextCompat.getDrawable(this@MainActivity, R.drawable.divider)
            val itemDecoration = DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL)

            if (divider != null) {
                itemDecoration.setDrawable(divider)
                addItemDecoration(itemDecoration)
            }

            adapter = this@MainActivity.recyclerViewAdapter
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
        }
    }

    private var lastChecked: Int = SortingType.NAME_ASC.ordinal
    private fun showPopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(this, anchorView, Gravity.END)
        val menu = popupMenu.menu

        menu.apply {
            add(Menu.NONE, SortingType.NAME_ASC.ordinal, Menu.NONE, R.string.sorting_name_asc)
            add(Menu.NONE, SortingType.NAME_DESC.ordinal, Menu.NONE, R.string.sorting_name_desc)
            add(Menu.NONE, SortingType.SIZE_ASC.ordinal, Menu.NONE, R.string.sorting_size_asc)
            add(Menu.NONE, SortingType.SIZE_DESC.ordinal, Menu.NONE, R.string.sorting_size_desc)
            add(Menu.NONE, SortingType.CREATION_DATE_ASC.ordinal, Menu.NONE, R.string.sorting_creation_date_asc)
            add(Menu.NONE, SortingType.CREATION_DATE_DESC.ordinal, Menu.NONE, R.string.sorting_creation_date_desc)
            add(Menu.NONE, SortingType.EXTENSION_ASC.ordinal, Menu.NONE, R.string.sorting_extension_asc)
            add(Menu.NONE, SortingType.EXTENSION_DESC.ordinal, Menu.NONE, R.string.sorting_extension_desc)
        }

        menu.forEach {
            it.isCheckable = true
        }

        menu.getItem(lastChecked).isChecked = true

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val type = SortingType.values()
                .find { it.ordinal == menuItem.itemId }
                ?: return@setOnMenuItemClickListener false

            menu.getItem(lastChecked).isChecked = false
            menu.getItem(type.ordinal).isChecked = true

            lastChecked = type.ordinal

            viewModel.updateSortingType(type)

            true
        }

        popupMenu.show()
    }
}
