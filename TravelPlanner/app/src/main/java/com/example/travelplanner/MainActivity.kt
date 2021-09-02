package com.example.travelplanner

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import com.example.travelplanner.detail_fragments.DestEditFragment
import com.example.travelplanner.detail_fragments.DestFragment
import com.example.travelplanner.detail_fragments.HomeFragment
import com.example.travelplanner.detail_fragments.StopFragment
import com.example.travelplanner.detail_fragments.TravelFragment
import com.example.travelplanner.detail_fragments.TripEditFragment
import com.example.travelplanner.detail_fragments.TripFragment
import com.example.travelplanner.utils.CardAdapter
import com.example.travelplanner.utils.ImportExportTripUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.microsoft.maps.MapServices

enum class TravelPlannerMode {
    HOME, TRIP, TRIP_EDIT, DEST, DEST_EDIT, STOP, TRAVEL
}

interface TravelPlannerInterface {
    fun tpLaunchMode(tpMode: TravelPlannerMode)
    var tpToolbar: MaterialToolbar
    var tpFlyoutAdapter: CardAdapter
    var tpMapFragment: MapFragment
    var tpHeaderNotesText: TextView
    var tpHeaderTitleText: TextView
    var tpTakeImageLauncher: ActivityResultLauncher<Uri>?
    var tpTakeImageCallback: ActivityResultCallback<Boolean>?
    var tpImportImageLauncher: ActivityResultLauncher<String>?
    var tpImportImageCallback: ActivityResultCallback<Uri>?
}

class MainActivity : AppCompatActivity(), TravelPlannerInterface, TabLayout.OnTabSelectedListener, ViewTreeObserver.OnGlobalLayoutListener {
    private var takeImageLauncher: ActivityResultLauncher<Uri>? = null
    private var takeImageCallback: ActivityResultCallback<Boolean>? = null
    private var importImageLauncher: ActivityResultLauncher<String>? = null
    private var importImageCallback: ActivityResultCallback<Uri>? = null

    private lateinit var tabTrick: View
    private lateinit var tabView: TabLayout

    private lateinit var headerNotesText: TextView
    private lateinit var headerTitleText: TextView

    private lateinit var flyoutView: View
    private lateinit var flyoutAdapter: CardAdapter
    private lateinit var detailView: FragmentContainerView

    private lateinit var mapFragment: MapFragment
    private lateinit var slidingPaneLayout: SlidingPaneLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tpViewModel: TravelPlannerViewModel

    private lateinit var dayList: ArrayList<Long>

    private lateinit var importExportTripUtil: ImportExportTripUtil

    private var tpMode: TravelPlannerMode = TravelPlannerMode.HOME
    private var tpToolbarButtonEditEnable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        tpViewModel = ViewModelProvider(this).get(TravelPlannerViewModel::class.java)

        importExportTripUtil = ImportExportTripUtil(this, tpViewModel)
        // Uncomment this line to import a test trip
        // importExportTripUtil.importTrip("mich_trip.json")

        takeImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            result ->
            takeImageCallback?.onActivityResult(result)
        }
        importImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
            result ->
            importImageCallback?.onActivityResult(result)
        }

        detailView = findViewById(R.id.detail)

        slidingPaneLayout = findViewById(R.id.sliding_pane)
        slidingPaneLayout.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED

        tabTrick = findViewById(R.id.tab_trick_tab_root)
        tabTrick.viewTreeObserver.addOnGlobalLayoutListener(this)

        tabView = tabTrick.findViewById(R.id.tab_trick_tab_dynamic)
        tabView.addOnTabSelectedListener(this)
        tabView.tabMode = TabLayout.MODE_SCROLLABLE

        headerNotesText = tabTrick.findViewById(R.id.header_notes)
        headerTitleText = tabTrick.findViewById(R.id.header_title)

        dayList = ArrayList<Long>()

        flyoutView = findViewById(R.id.flyout)
        flyoutAdapter = CardAdapter(flyoutView, this)

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as MapFragment
        toolbar = findViewById(R.id.toolbar)

        if (savedInstanceState?.getString("TP_MODE") != null) {
            tpMode = TravelPlannerMode.valueOf(savedInstanceState.getString("TP_MODE")!!)
            onModeChange()
        } else {
            tpLaunchMode(TravelPlannerMode.HOME)
        }

        if (BuildConfig.BUILD_KEY == "") {
            //TODO: You need to register for a Bing Maps key
            // https://docs.microsoft.com/bingmaps/sdk-native/getting-started-android/
            Log.d("MAP", "ext.credentialsKey in secrets.gradle is empty. Please register for a Bing Maps key.")
        } else {
            MapServices.setCredentialsKey(BuildConfig.BUILD_KEY)
        }
    }

    fun getDayFromIndex(dayIndex: Int): Long? {
        return if (dayIndex >= 0 && dayIndex < dayList.size) {
            dayList[dayIndex]
        } else {
            return null
        }
    }

    fun updateTabView(days: ArrayList<Long>) {
        dayList = days
        tabView.removeAllTabs()
        tabView.addTab(tabView.newTab().setText(getString(R.string.text_all_stops)))
        tabView.addTab(tabView.newTab().setText(getString(R.string.text_any_day)))
        for (day in dayList) {
            tabView.addTab(tabView.newTab().setText(DateUtil.longToDateShortened(day)))
        }
    }

    fun selectTab(tabIndex: Int) {
        tabView.getTabAt(tabIndex)?.select()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("TP_MODE", tpMode.toString())
    }

    private fun onModeChange() {
        tabView.visibility = if (tpMode == TravelPlannerMode.DEST) View.VISIBLE else View.GONE
        tabTrick.visibility = when (tpMode) {
            TravelPlannerMode.TRIP, TravelPlannerMode.DEST -> View.VISIBLE
            else -> View.GONE
        }
        flyoutView.visibility = when (tpMode) {
            TravelPlannerMode.HOME, TravelPlannerMode.TRIP, TravelPlannerMode.DEST -> View.VISIBLE
            else -> View.GONE
        }
        tpToolbarButtonEditEnable = when (tpMode) {
            TravelPlannerMode.TRIP, TravelPlannerMode.DEST -> true
            else -> false
        }
        //supportActionBar?.invalidateOptionsMenu() // HACK: build warning raised
        this.invalidateOptionsMenu() // TODO: fix?
        supportActionBar?.setDisplayHomeAsUpEnabled(tpMode != TravelPlannerMode.HOME)
    }

    override fun onGlobalLayout() {
        if (tabTrick.visibility != View.GONE) {
            detailView.setPadding(0, tabTrick.height, 0, 0)
            detailView.invalidate()
        } else {
            detailView.setPadding(0, 0, 0, 0)
            detailView.invalidate()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount <= 1) {
            super.onBackPressed()
        } else {
            supportFragmentManager.popBackStackImmediate()
            tpMode = TravelPlannerMode.valueOf(supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1).name!!)
            onModeChange()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        tpViewModel.editing = false
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.toolbar_button_edit).isVisible = tpToolbarButtonEditEnable
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_button_swap -> {
                if (slidingPaneLayout.isOpen) {
                    slidingPaneLayout.close()
                    item.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_fluent_map_24_filled, theme)
                } else {
                    slidingPaneLayout.open()
                    item.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_fluent_task_list_ltr_24_filled, theme)
                }
            }
            R.id.toolbar_button_edit -> {
                tpViewModel.editing = true
                when (tpMode) {
                    TravelPlannerMode.TRIP -> tpLaunchMode(TravelPlannerMode.TRIP_EDIT)
                    TravelPlannerMode.DEST -> tpLaunchMode(TravelPlannerMode.DEST_EDIT)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        var pos = tab?.position
        if (pos != null) {
            if (pos == 0) {
                tpViewModel.setSelectedDayIndex(TravelPlannerViewModel.SELECTED_ALL_STOPS)
            } else if (pos == 1) {
                tpViewModel.setSelectedDayIndex(TravelPlannerViewModel.SELECTED_ANY_DAY)
            } else {
                tpViewModel.setSelectedDayIndex(pos - 2)
            }
        } else {
            tpViewModel.setSelectedDayIndex(TravelPlannerViewModel.SELECTED_ALL_STOPS)
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
    }

    override fun tpLaunchMode(newTPMode: TravelPlannerMode) {
        tpMode = newTPMode

        val transaction = supportFragmentManager.beginTransaction()

        val newDetailFragment = when (tpMode) {
            TravelPlannerMode.HOME -> HomeFragment()
            TravelPlannerMode.TRIP -> TripFragment()
            TravelPlannerMode.TRIP_EDIT -> TripEditFragment()
            TravelPlannerMode.DEST -> DestFragment()
            TravelPlannerMode.DEST_EDIT -> DestEditFragment()
            TravelPlannerMode.STOP -> StopFragment()
            TravelPlannerMode.TRAVEL -> TravelFragment()
        }

        transaction.replace(R.id.detail, newDetailFragment)
        transaction.addToBackStack(tpMode.toString())
        transaction.setReorderingAllowed(true)
        transaction.commit()

        onModeChange()
    }

    override var tpToolbar: MaterialToolbar
        get() = toolbar
        set(value) {}
    override var tpFlyoutAdapter: CardAdapter
        get() = flyoutAdapter
        set(value) {}
    override var tpMapFragment: MapFragment
        get() = mapFragment
        set(value) {}
    override var tpHeaderNotesText: TextView
        get() = headerNotesText
        set(value) {}
    override var tpHeaderTitleText: TextView
        get() = headerTitleText
        set(value) {}
    override var tpTakeImageLauncher: ActivityResultLauncher<Uri>?
        get() = takeImageLauncher
        set(value) {}
    override var tpTakeImageCallback: ActivityResultCallback<Boolean>?
        get() = takeImageCallback
        set(value) { takeImageCallback = value }
    override var tpImportImageLauncher: ActivityResultLauncher<String>?
        get() = importImageLauncher
        set(value) {}
    override var tpImportImageCallback: ActivityResultCallback<Uri>?
        get() = importImageCallback
        set(value) { importImageCallback = value }
}
