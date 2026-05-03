package nep.timeline.cirno.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nep.timeline.cirno.entity.AppItem
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.utils.PackageUtils
import nep.timeline.cirno.ui.utils.WindowUtils

class AppListViewModel : ViewModel() {
    private val _filterApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _cacheFilterApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _search = MutableStateFlow("")
    private val _type = MutableStateFlow(0)
    private val _updatedApps = MutableStateFlow(true)
    private val _hasLoadedMonitorOnce = MutableStateFlow(false)
    val search: StateFlow<String> = _search
    var cacheFilterApps: StateFlow<List<AppItem>> = _cacheFilterApps
    val type: StateFlow<Int> = _type
    var filterApps: StateFlow<List<AppItem>> = _filterApps
    val updatedApps: StateFlow<Boolean> = _updatedApps
    val hasLoadedMonitorOnce: StateFlow<Boolean> = _hasLoadedMonitorOnce

    private fun autoUpdateCacheFilterApps() {
        viewModelScope.launch {
            combine(_filterApps, _search, _type) { apps, search, type ->
                apps.asSequence()
                    .filter {
                        when (type) {
                            0 -> !PackageUtils.isSystemUIChecker(AppContext.context, it.packageInfo)
                            1 -> PackageUtils.isSystemUIChecker(AppContext.context, it.packageInfo)
                            else -> true
                        }
                    }
                    .filter {
                        if (search.isNotEmpty())
                            it.appName.contains(search, ignoreCase = true) || it.packageName.contains(search, ignoreCase = true)
                        else true
                    }.toList()
            }.collect { filteredApps ->
                _cacheFilterApps.value = filteredApps
            }
        }
    }

    fun updateByQuery(appName: String = _search.value, type: Int) {
        _search.value = appName
        if (_type.value != type) {
            getFilterApps(type)
            _type.value = type
        }
    }

    fun getFilterApps(type: Int = _type.value) {
        _updatedApps.value = false
        viewModelScope.launch {
            WindowUtils.handler.post {
                _filterApps.value = when (type) {
                    0 -> PackageUtils.filter(3)
                    1 -> PackageUtils.filter(3)
                    else -> PackageUtils.getFrozenApplication(AppContext.context)
                }
                if (type == 2) {
                    _hasLoadedMonitorOnce.value = true
                }
                _updatedApps.value = true
            }
        }
    }

    fun update() {
        viewModelScope.launch {
            _updatedApps.value = false
            WindowUtils.handler.post {
                _filterApps.value = when (_type.value) {
                    0 -> PackageUtils.filter(3)
                    1 -> PackageUtils.filter(3)
                    else -> PackageUtils.getFrozenApplication(AppContext.context)
                }
                _updatedApps.value = true
            }
        }
    }

    init {
        autoUpdateCacheFilterApps()
    }
}
