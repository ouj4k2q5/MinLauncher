package app.minlauncher.ui

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Filter
import android.widget.Filterable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.minlauncher.R
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.databinding.AdapterAppDrawerBinding
import app.minlauncher.databinding.AdapterPrivateSpaceHeaderBinding
import app.minlauncher.helper.hideKeyboard
import app.minlauncher.helper.isSystemApp
import app.minlauncher.helper.showKeyboard
import java.text.Normalizer

private const val TAG = "AppDrawerAdapter"

class AppDrawerAdapter(
    private var flag: Int,
    private val appLabelGravity: Int,
    private val appClickListener: (AppModel) -> Unit,
    private val appInfoListener: (AppModel) -> Unit,
    private val appDeleteListener: (AppModel) -> Unit,
    private val appHideListener: (AppModel) -> Unit,
    private val appRenameListener: (AppModel, String) -> Unit,
    private val privateSpaceToggleListener: () -> Unit = {},
    private val privateSpaceSettingsListener: () -> Unit = {},
) : ListAdapter<AppModel, RecyclerView.ViewHolder>(DIFF_CALLBACK),
    Filterable {
    companion object {
        const val VIEW_TYPE_APP = 0
        const val VIEW_TYPE_PRIVATE_HEADER = 1

        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<AppModel>() {
                override fun areItemsTheSame(
                    oldItem: AppModel,
                    newItem: AppModel,
                ): Boolean =
                    when {
                        oldItem is AppModel.App && newItem is AppModel.App ->
                            oldItem.appPackage == newItem.appPackage && oldItem.user == newItem.user

                        oldItem is AppModel.PinnedShortcut && newItem is AppModel.PinnedShortcut ->
                            oldItem.identity == newItem.identity

                        oldItem is AppModel.PrivateSpaceHeader && newItem is AppModel.PrivateSpaceHeader -> true

                        else -> false
                    }

                override fun areContentsTheSame(
                    oldItem: AppModel,
                    newItem: AppModel,
                ): Boolean = oldItem == newItem
            }
    }

    private var autoLaunch = true
    private var isBangSearch = false
    var allowAutoLaunch = true
    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val separatorsRegex = Regex("[-_+,.`'\\s\\p{Z}]")
    private val appFilter = createAppFilter()
    private val myUserHandle = android.os.Process.myUserHandle()

    private var appsList: List<AppModel> = mutableListOf()

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is AppModel.PrivateSpaceHeader -> VIEW_TYPE_PRIVATE_HEADER
            else -> VIEW_TYPE_APP
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_PRIVATE_HEADER ->
                PrivateSpaceHeaderViewHolder(
                    AdapterPrivateSpaceHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )

            else ->
                ViewHolder(
                    AdapterAppDrawerBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        try {
            if (position == RecyclerView.NO_POSITION) return
            val appModel = getItem(position)
            when (holder) {
                is PrivateSpaceHeaderViewHolder -> {
                    holder.bind(
                        appLabelGravity,
                        privateSpaceToggleListener,
                        privateSpaceSettingsListener,
                    )
                }

                is ViewHolder ->
                    holder.bind(
                        flag,
                        appLabelGravity,
                        myUserHandle,
                        appModel,
                        appClickListener,
                        appDeleteListener,
                        appInfoListener,
                        appHideListener,
                        appRenameListener,
                    )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind app drawer item", e)
        }
    }

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSearch: CharSequence?): FilterResults {
                isBangSearch = charSearch?.startsWith("!") ?: false
                autoLaunch = allowAutoLaunch && (charSearch?.startsWith(" ")?.not() ?: true)

                val source = appsList.toList()
                val appFilteredList = (
                    if (charSearch.isNullOrBlank()) {
                        source
                    } else {
                        source.filter { app ->
                            app !is AppModel.PrivateSpaceHeader && appLabelMatches(app.appLabel, charSearch)
                        }
                    }
                )

                val filterResults = FilterResults()
                filterResults.values = appFilteredList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?,
            ) {
                results?.values?.let {
                    val items = it as List<AppModel>
                    submitList(items) {
                        autoLaunch()
                    }
                }
            }
        }
    }

    private fun autoLaunch() {
        try {
            if (itemCount == 1 &&
                autoLaunch &&
                isBangSearch.not() &&
                flag == Constants.FLAG_LAUNCH_APP &&
                currentList.isNotEmpty() &&
                currentList[0] !is AppModel.PrivateSpaceHeader
            ) {
                appClickListener(currentList[0])
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto launch app", e)
        }
    }

    private fun appLabelMatches(
        appLabel: String,
        charSearch: CharSequence,
    ): Boolean {
        fun normalize(value: CharSequence): String =
            Normalizer
                .normalize(value, Normalizer.Form.NFD)
                .replace(diacriticsRegex, "")
                .replace(separatorsRegex, "")

        if (appLabel.contains(charSearch.trim(), true)) return true
        val query = normalize(charSearch)
        return query.isNotEmpty() && normalize(appLabel).contains(query, true)
    }

    fun setAppList(appsList: List<AppModel>) {
        // Add empty app for bottom padding in recyclerview and assign to list
        val list = appsList.toMutableList()
        list.add(
            AppModel.App(
                appLabel = "",
                key = null,
                appPackage = "",
                activityClassName = "",
                isNew = false,
                user = android.os.Process.myUserHandle(),
            ),
        )
        this.appsList = list
        submitList(list.toList())
    }

    fun removeApp(appModel: AppModel) {
        appsList = appsList.filterNot { it == appModel }
        submitList(currentList.filterNot { it == appModel })
    }

    fun launchFirstInList() {
        val first = currentList.firstOrNull { it !is AppModel.PrivateSpaceHeader }
        if (first != null) appClickListener(first)
    }

    class PrivateSpaceHeaderViewHolder(
        private val binding: AdapterPrivateSpaceHeaderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            appLabelGravity: Int,
            toggleListener: () -> Unit,
            settingsListener: () -> Unit,
        ) = with(binding) {
            privateSpaceTitle.gravity = appLabelGravity
            privateSpaceTitle.setOnClickListener { toggleListener() }
            privateSpaceTitle.setOnLongClickListener {
                settingsListener()
                true
            }
        }
    }

    class ViewHolder(
        private val binding: AdapterAppDrawerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            flag: Int,
            appLabelGravity: Int,
            myUserHandle: UserHandle,
            appModel: AppModel,
            clickListener: (AppModel) -> Unit,
            appDeleteListener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit,
            appHideListener: (AppModel) -> Unit,
            appRenameListener: (AppModel, String) -> Unit,
        ) = with(binding) {
            appHideLayout.visibility = View.GONE
            renameLayout.visibility = View.GONE
            appTitle.visibility = View.VISIBLE

            // Show indicators in title based on app type and state
            appTitle.text =
                buildString {
                    append(appModel.appLabel)
                    if (appModel.isNew) append(" ✦")
                }
            appTitle.gravity = appLabelGravity
            otherProfileIndicator.isVisible = appModel.user != myUserHandle

            appTitle.setOnClickListener { clickListener(appModel) }

            appTitle.setOnLongClickListener {
                if (appModel.appPackage.isNotEmpty()) {
                    appDelete.alpha =
                        when (
                            appModel is AppModel.PinnedShortcut ||
                                !root.context.isSystemApp(appModel.appPackage, appModel.user)
                        ) {
                            true -> 1.0f
                            false -> 0.5f
                        }
                    appHide.text =
                        if (flag == Constants.FLAG_HIDDEN_APPS) {
                            root.context.getString(R.string.adapter_show)
                        } else {
                            root.context.getString(R.string.adapter_hide)
                        }
                    appTitle.visibility = View.INVISIBLE
                    appHide.alpha =
                        when (appModel is AppModel.PinnedShortcut) {
                            true -> 0.5f
                            false -> 1.0f
                        }
                    appHideLayout.visibility = View.VISIBLE
                    // Only allow renaming non hidden apps
                    appRename.isVisible = flag != Constants.FLAG_HIDDEN_APPS
                }
                true
            }

            // Configure rename behavior
            appRename.setOnClickListener {
                if (appModel.appPackage.isNotEmpty()) {
                    etAppRename.hint = getAppName(etAppRename.context, appModel.appPackage, appModel.user)
                    etAppRename.setText(appModel.appLabel)
                    etAppRename.setSelectAllOnFocus(true)
                    renameLayout.visibility = View.VISIBLE
                    appHideLayout.visibility = View.GONE
                    etAppRename.showKeyboard()
                    etAppRename.imeOptions = EditorInfo.IME_ACTION_DONE
                }
            }
            etAppRename.onFocusChangeListener =
                View.OnFocusChangeListener { _, hasFocus ->
                    appTitle.visibility = if (hasFocus) View.INVISIBLE else View.VISIBLE
                }
            etAppRename.addTextChangedListener(
                object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        etAppRename.hint = getAppName(etAppRename.context, appModel.appPackage, appModel.user)
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) {
                        etAppRename.hint = ""
                    }
                },
            )
            etAppRename.setOnEditorActionListener { _, actionCode, _ ->
                if (actionCode == EditorInfo.IME_ACTION_DONE) {
                    val renameLabel = etAppRename.text.toString().trim()
                    if (renameLabel.isNotBlank() && appModel.appPackage.isNotBlank()) {
                        appRenameListener(appModel, renameLabel)
                        renameLayout.visibility = View.GONE
                    }
                    true
                }
                false
            }
            tvSaveRename.setOnClickListener {
                etAppRename.hideKeyboard()
                val renameLabel = etAppRename.text.toString().trim()
                if (renameLabel.isNotBlank() && appModel.appPackage.isNotBlank()) {
                    appRenameListener(appModel, renameLabel)
                    renameLayout.visibility = View.GONE
                } else {
                    appRenameListener(
                        appModel,
                        getAppName(etAppRename.context, appModel.appPackage, appModel.user),
                    )
                    renameLayout.visibility = View.GONE
                }
            }
            appInfo.setOnClickListener { appInfoListener(appModel) }
            appDelete.setOnClickListener { appDeleteListener(appModel) }
            appMenuClose.setOnClickListener {
                appHideLayout.visibility = View.GONE
                appTitle.visibility = View.VISIBLE
            }
            appRenameClose.setOnClickListener {
                renameLayout.visibility = View.GONE
                appTitle.visibility = View.VISIBLE
            }
            appHide.setOnClickListener { appHideListener(appModel) }
        }

        private fun getAppName(
            context: Context,
            appPackage: String,
            user: UserHandle,
        ): String {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            return try {
                val activityList = launcherApps.getActivityList(appPackage, user)
                if (activityList.isNotEmpty()) {
                    activityList.first().label.toString()
                } else {
                    val packageManager = context.packageManager
                    packageManager
                        .getApplicationLabel(
                            packageManager.getApplicationInfo(appPackage, 0),
                        ).toString()
                }
            } catch (_: Exception) {
                "" // As a fallback, display an empty string.
            }
        }
    }
}
