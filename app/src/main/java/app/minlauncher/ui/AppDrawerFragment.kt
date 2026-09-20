package app.minlauncher.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.text.Spannable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import app.minlauncher.MainViewModel
import app.minlauncher.R
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.databinding.FragmentAppDrawerBinding
import app.minlauncher.helper.deletePinnedShortcut
import app.minlauncher.helper.hideKeyboard
import app.minlauncher.helper.isEinkDisplay
import app.minlauncher.helper.isSystemAnimationsDisabled
import app.minlauncher.helper.isSystemApp
import app.minlauncher.helper.openAppInfo
import app.minlauncher.helper.openSearch
import app.minlauncher.helper.openUrl
import app.minlauncher.helper.showKeyboard
import app.minlauncher.helper.showToast
import app.minlauncher.helper.uninstall

private const val TAG = "AppDrawerFragment"

class AppDrawerFragment : BaseFragment() {
    private lateinit var prefs: Prefs
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var searchTextView: TextView? = null
    private var cachedIsCjkKeyboard: Boolean? = null

    private var flag = Constants.FLAG_LAUNCH_APP
    private var canRename = false
    private var currentAppList: List<AppModel>? = null
    private var currentPrivateSpaceApps: List<AppModel>? = null
    private var currentPrivateSpaceLocked: Boolean = true
    private var currentPrivateSpaceAvailable: Boolean = false

    private val viewModel: MainViewModel by activityViewModels()
    private var viewBinding: FragmentAppDrawerBinding? = null
    private val binding get() = viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewBinding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        arguments?.let {
            flag = it.getInt(Constants.Key.FLAG, Constants.FLAG_LAUNCH_APP)
            canRename = it.getBoolean(Constants.Key.RENAME, false)
        }

        initViews()
        initSearch()
        initAdapter()
        initObservers()
        initClickListeners()
    }

    private fun initViews() {
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            binding.search.queryHint = getString(R.string.hidden_apps)
        } else if (flag in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_SWIPE_RIGHT_APP) {
            binding.search.queryHint = getString(R.string.select_app_hint)
        }
        try {
            searchTextView = binding.search.findViewById(androidx.appcompat.R.id.search_src_text)
            searchTextView?.gravity = prefs.appLabelAlignment
        } catch (e: Exception) {
            Log.e(TAG, "Failed to style search view", e)
        }
    }

    private fun initSearch() {
        binding.search.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (query?.startsWith("!") == true) {
                        requireContext().openUrl(Constants.URL_DUCK_SEARCH + Uri.encode(query))
                    } else if (adapter.itemCount == 0) {
                        requireContext().openSearch(query?.trim())
                    } else {
                        adapter.launchFirstInList()
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    try {
                        adapter.allowAutoLaunch = !isSearchComposing()
                        adapter.filter.filter(newText)
                        binding.appRename.visibility =
                            if (canRename && newText.isNotBlank()) View.VISIBLE else View.GONE
                        return true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to filter app list", e)
                    }
                    return false
                }
            },
        )
    }

    private fun isSearchComposing(): Boolean {
        val text = searchTextView?.text
        if (text !is Spannable) return false
        val start = BaseInputConnection.getComposingSpanStart(text)
        val end = BaseInputConnection.getComposingSpanEnd(text)
        if (start !in 0 until end) return false
        return isCjkKeyboard()
    }

    private fun isCjkKeyboard(): Boolean {
        cachedIsCjkKeyboard?.let { return it }

        val result =
            runCatching {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val subtype = imm?.currentInputMethodSubtype ?: return@runCatching false

                val language =
                    subtype.languageTag.ifEmpty {
                        @Suppress("DEPRECATION")
                        subtype.locale
                    }

                language.startsWith("zh") || language.startsWith("ja") || language.startsWith("ko")
            }.getOrElse { false }

        cachedIsCjkKeyboard = result
        return result
    }

    private fun initAdapter() {
        adapter =
            AppDrawerAdapter(
                flag,
                prefs.appLabelAlignment,
                appClickListener = { appModel ->
                    viewModel.selectedApp(appModel, flag)
                    if (flag == Constants.FLAG_LAUNCH_APP || flag == Constants.FLAG_HIDDEN_APPS) {
                        findNavController().popBackStack(R.id.mainFragment, false)
                    } else {
                        findNavController().popBackStack()
                    }
                },
                appInfoListener = {
                    openAppInfo(
                        requireContext(),
                        it.user,
                        it.appPackage,
                    )
                    findNavController().popBackStack(R.id.mainFragment, false)
                },
                appDeleteListener = { appModel ->
                    when (appModel) {
                        is AppModel.PrivateSpaceHeader -> {}
                        is AppModel.PinnedShortcut ->
                            requireContext().deletePinnedShortcut(
                                packageName = appModel.appPackage,
                                shortcutIdToDelete = appModel.shortcutId,
                                user = appModel.user,
                            )

                        is AppModel.App -> {
                            if (appModel.user != Process.myUserHandle()) {
                                openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                            } else if (requireContext().isSystemApp(appModel.appPackage, appModel.user)) {
                                requireContext().showToast(getString(R.string.system_app_cannot_delete))
                                openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                            } else {
                                requireContext().uninstall(appModel.appPackage)
                            }
                        }
                    }
                    viewModel.getAppList()
                },
                appHideListener = { appModel ->
                    if (appModel is AppModel.PinnedShortcut) {
                        requireContext().showToast(getString(R.string.hide_pinned_shortcut_not_supported))
                        return@AppDrawerAdapter
                    }
                    adapter.removeApp(appModel)

                    val newSet = mutableSetOf<String>()
                    newSet.addAll(prefs.hiddenApps)
                    if (flag == Constants.FLAG_HIDDEN_APPS) {
                        newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
                    } else {
                        newSet.add(appModel.appPackage + "|" + appModel.user.toString())
                    }

                    prefs.hiddenApps = newSet
                    if (newSet.isEmpty()) {
                        findNavController().popBackStack()
                    }
                    if (prefs.firstHide) {
                        binding.search.hideKeyboard()
                        prefs.firstHide = false
                        viewModel.showDialog.postValue(Constants.Dialog.HIDDEN)
                        findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
                    }
                    viewModel.getAppList()
                    viewModel.getHiddenApps()
                },
                appRenameListener = { appModel, renameLabel ->
                    val identifier =
                        when (appModel) {
                            is AppModel.PinnedShortcut -> appModel.identity
                            is AppModel.App -> appModel.appPackage
                            else -> return@AppDrawerAdapter
                        }
                    prefs.setAppRenameLabel(identifier, renameLabel)
                    viewModel.getAppList()
                },
                privateSpaceToggleListener = {
                    viewModel.togglePrivateSpaceLock()
                },
                privateSpaceSettingsListener = {
                    viewModel.openPrivateSpaceSettings()
                    findNavController().popBackStack(R.id.mainFragment, false)
                },
            )

        linearLayoutManager =
            object : LinearLayoutManager(requireContext()) {
                override fun scrollVerticallyBy(
                    dx: Int,
                    recycler: Recycler,
                    state: RecyclerView.State,
                ): Int {
                    val scrollRange = super.scrollVerticallyBy(dx, recycler, state)
                    val overScroll = dx - scrollRange
                    if (overScroll < -10 && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        exitDrawer()
                    }
                    return scrollRange
                }
            }

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())
        binding.recyclerView.itemAnimator = null
        if (requireContext().isEinkDisplay()) {
            binding.recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        } else if (requireContext().isSystemAnimationsDisabled().not()) {
            binding.recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
        }
    }

    private fun initObservers() {
        viewModel.firstOpen.observe(viewLifecycleOwner) {
        }
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            viewModel.hiddenApps.observe(viewLifecycleOwner) {
                it?.let {
                    adapter.setAppList(it)
                }
            }
        } else {
            viewModel.appList.observe(viewLifecycleOwner) {
                currentAppList = it
                updateCombinedAppList()
            }
            if (flag == Constants.FLAG_LAUNCH_APP) {
                viewModel.privateSpaceAvailable.observe(viewLifecycleOwner) {
                    currentPrivateSpaceAvailable = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceLocked.observe(viewLifecycleOwner) {
                    currentPrivateSpaceLocked = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceApps.observe(viewLifecycleOwner) {
                    currentPrivateSpaceApps = it
                    updateCombinedAppList()
                }
            }
        }
    }

    private fun updateCombinedAppList() {
        val apps = currentAppList ?: return
        val combined = apps.toMutableList()

        if (flag == Constants.FLAG_LAUNCH_APP && currentPrivateSpaceAvailable) {
            combined.add(AppModel.PrivateSpaceHeader(isLocked = currentPrivateSpaceLocked))
            if (!currentPrivateSpaceLocked) {
                currentPrivateSpaceApps?.let { combined.addAll(it) }
            }
        }

        adapter.setAppList(combined)
        adapter.filter.filter(binding.search.query)
    }

    private fun initClickListeners() {
        binding.appRename.setOnClickListener {
            val name =
                binding.search.query
                    .toString()
                    .trim()
            if (name.isEmpty()) {
                requireContext().showToast(getString(R.string.type_a_new_app_name_first))
                binding.search.showKeyboard()
                return@setOnClickListener
            }

            when (flag) {
                Constants.FLAG_SET_HOME_APP_1 -> prefs.appName1 = name
                Constants.FLAG_SET_HOME_APP_2 -> prefs.appName2 = name
                Constants.FLAG_SET_HOME_APP_3 -> prefs.appName3 = name
                Constants.FLAG_SET_HOME_APP_4 -> prefs.appName4 = name
                Constants.FLAG_SET_HOME_APP_5 -> prefs.appName5 = name
                Constants.FLAG_SET_HOME_APP_6 -> prefs.appName6 = name
                Constants.FLAG_SET_HOME_APP_7 -> prefs.appName7 = name
                Constants.FLAG_SET_HOME_APP_8 -> prefs.appName8 = name
            }
            findNavController().popBackStack()
        }
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            var onTop = false

            override fun onScrollStateChanged(
                recyclerView: RecyclerView,
                newState: Int,
            ) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop) {
                            binding.search.hideKeyboard()
                        }
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1)) {
                            binding.search.hideKeyboard()
                        } else if (!recyclerView.canScrollVertically(-1)) {
                            if (!onTop && isRemoving.not()) {
                                binding.search.showKeyboard(prefs.autoShowKeyboard)
                            }
                        }
                    }
                }
            }
        }

    private fun exitDrawer() {
        findNavController().popBackStack()
    }

    override fun onStart() {
        super.onStart()
        cachedIsCjkKeyboard = null
        binding.search.showKeyboard(prefs.autoShowKeyboard)
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchTextView = null
        viewBinding = null
    }
}
