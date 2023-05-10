package com.example.vktesttask

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.Settings
import android.view.Gravity
import android.view.Menu
import android.view.SubMenu
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.vktesttask.data.repository.FolderRepository
import com.example.vktesttask.databinding.ActivityMainBinding
import com.example.vktesttask.model.FileType
import com.example.vktesttask.model.SortingType
import com.example.vktesttask.receiver.StorageReceiver
import com.example.vktesttask.ui.adapter.FolderAdapter
import com.example.vktesttask.viewmodel.FolderViewModel
import com.example.vktesttask.work.FolderFileChangedWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerViewAdapter: FolderAdapter

    private val viewModel: FolderViewModel by viewModels()

    @Inject
    lateinit var repository: FolderRepository

    private lateinit var storageReceiver: StorageReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Добавлено, чтобы работал Share API
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        val fileChanged: WorkRequest = FolderFileChangedWorker.startUpFilesChangedWork()
        WorkManager.getInstance(this).enqueue(fileChanged)

        setupRecyclerView()
        setupToolbar()

        storageReceiver = StorageReceiver {
            viewModel.initializeStorages(this)
        }

        launchWhenStartedInScope {
            viewModel.uiState.collect {
                recyclerViewAdapter.updateData(it.data)
            }
        }

        requestStoragePermission()
    }

    private var dialogNotGranted: AlertDialog? = null
    private fun actionOnNotGranted() {
        if (dialogNotGranted == null) {
            AlertDialog.Builder(this).apply {
                setTitle(R.string.dialog_access_title)
                setMessage(R.string.dialog_access_subtitle)
                setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                    requestStoragePermission()
                }

                setNegativeButton(android.R.string.cancel) { _: DialogInterface, _: Int ->
                    exitProcess(0)
                }

                setOnCancelListener {
                    exitProcess(0)
                }

                dialogNotGranted = create()
            }
        }

        dialogNotGranted?.show()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                initialization()
            } else {
                actionOnNotGranted()
            }
        }

    private val manageStorageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            initialization()
        } else {
            actionOnNotGranted()
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                manageStorageLauncher.launch(intent)
            } else {
                initialization()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                initialization()
            }
        }
    }

    private fun initialization() {
        viewModel.initializeStorages(this)
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

    override fun onBackPressed() {
        if (viewModel.canNavigateUp()) {
            viewModel.navigateUp()
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        recyclerViewAdapter = FolderAdapter(this) {
            if (it.fileType == FileType.DIR) {
                viewModel.navigateTo(it.file)
            } else {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.fromFile(it.file))

                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.error_open, Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }

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

        binding.refreshLayout.setOnRefreshListener {
            launchWhenStartedInScope {
                viewModel.updateFolder()
            }

            binding.refreshLayout.isRefreshing = false
        }
    }

    private var lastSortingChecked: Int = SortingType.NAME_ASC.ordinal
    private var lastStoragesChecked = 0
    private fun showPopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(this, anchorView, Gravity.END)
        val menu = popupMenu.menu
        val sortingSubMenu: SubMenu
        val storagesSubMenu: SubMenu

        menu.apply {
            sortingSubMenu = addSubMenu(R.string.sorting).apply {
                add(SORTING_SUBMENU, SortingType.NAME_ASC.ordinal, Menu.NONE, R.string.sorting_name_asc)
                add(SORTING_SUBMENU, SortingType.NAME_DESC.ordinal, Menu.NONE, R.string.sorting_name_desc)
                add(SORTING_SUBMENU, SortingType.SIZE_ASC.ordinal, Menu.NONE, R.string.sorting_size_asc)
                add(SORTING_SUBMENU, SortingType.SIZE_DESC.ordinal, Menu.NONE, R.string.sorting_size_desc)
                add(
                    SORTING_SUBMENU,
                    SortingType.CREATION_DATE_ASC.ordinal,
                    Menu.NONE,
                    R.string.sorting_creation_date_asc
                )
                add(
                    SORTING_SUBMENU,
                    SortingType.CREATION_DATE_DESC.ordinal,
                    Menu.NONE,
                    R.string.sorting_creation_date_desc
                )
                add(SORTING_SUBMENU, SortingType.EXTENSION_ASC.ordinal, Menu.NONE, R.string.sorting_extension_asc)
                add(SORTING_SUBMENU, SortingType.EXTENSION_DESC.ordinal, Menu.NONE, R.string.sorting_extension_desc)
            }

            storagesSubMenu = addSubMenu(R.string.storages).apply {
                viewModel.uiState.value.storages.forEachIndexed { index, file ->
                    add(STORAGES_SUBMENU, index, Menu.NONE, file.absolutePath).isCheckable = true
                }

                getItem(lastStoragesChecked)?.isChecked = true
            }
        }

        sortingSubMenu.forEach {
            it.isCheckable = true
        }

        sortingSubMenu.getItem(lastSortingChecked).isChecked = true

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val id = menuItem.itemId

            when (menuItem.groupId) {
                SORTING_SUBMENU -> {
                    val type = SortingType.values()
                        .find { it.ordinal == id }
                        ?: return@setOnMenuItemClickListener false

                    sortingSubMenu.getItem(lastSortingChecked).isChecked = false
                    sortingSubMenu.getItem(id).isChecked = true

                    lastSortingChecked = id

                    viewModel.updateSortingType(type)
                }
                STORAGES_SUBMENU -> {
                    storagesSubMenu.getItem(lastStoragesChecked).isChecked = false
                    storagesSubMenu.getItem(id).isChecked = true

                    lastStoragesChecked = id

                    viewModel.updateStorage(id)
                }
                else -> return@setOnMenuItemClickListener false
            }

            true
        }

        popupMenu.show()
    }

    override fun onResume() {
        super.onResume()

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addDataScheme("file")
        }
        registerReceiver(storageReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()

        unregisterReceiver(storageReceiver)
    }
    companion object {
        private const val SORTING_SUBMENU = 1
        private const val STORAGES_SUBMENU = 2
    }
}
