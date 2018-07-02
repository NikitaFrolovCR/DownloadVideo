package com.nikita_frolov_cr.downloadvideo

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.source.dash.offline.DashDownloadHelper
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider
import com.google.android.exoplayer2.ui.TrackNameProvider
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.Util
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.CopyOnWriteArraySet

/** Listens for changes in the tracked downloads.  */
interface Listener {

    /** Called when the tracked downloads changed.  */
    fun onDownloadsChanged()
}

class DownloadTracker : DownloadManager.Listener {

    private val context: Context
    private val dataSourceFactory: DataSource.Factory
    private val trackNameProvider: TrackNameProvider
    private val listeners: CopyOnWriteArraySet<Listener>
    private val trackedDownloadStates: HashMap<Uri, DownloadAction>
    private val actionFile: ActionFile
    private val actionFileWriteHandler: Handler

    constructor(
            context: Context,
            dataSourceFactory: DataSource.Factory,
            actionFile: File,
            deserializers: DownloadAction.Deserializer) {
        this.context = context.applicationContext
        this.dataSourceFactory = dataSourceFactory
        this.actionFile = ActionFile(actionFile)
        trackNameProvider = DefaultTrackNameProvider(context.resources)
        listeners = CopyOnWriteArraySet()
        trackedDownloadStates = HashMap()
        val actionFileWriteThread = HandlerThread("DownloadTracker")
        actionFileWriteThread.start()
        actionFileWriteHandler = Handler(actionFileWriteThread.looper)
        loadTrackedActions(deserializers)
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun isDownloaded(uri: Uri): Boolean {
        return trackedDownloadStates.containsKey(uri)
    }

    fun <K : Comparable<K>?> getOfflineStreamKeys(uri: Uri): List<K> {
        if (!trackedDownloadStates.containsKey(uri)) return emptyList()
        val action = trackedDownloadStates[uri]
        return (action as? SegmentDownloadAction<K>)?.keys ?: emptyList()
    }

    fun toggleDownload(activity: Activity, name: String, uri: Uri, extension: String?) {
        if (isDownloaded(uri)) {
            val removeAction =  DashDownloadHelper(uri, dataSourceFactory).getRemoveAction(Util.getUtf8Bytes(name))
            startServiceWithAction(removeAction)
        } else {
            val helper = StartDownloadDialogHelper(activity,  DashDownloadHelper(uri, dataSourceFactory), name)
            helper.prepare()
        }
    }

    private fun loadTrackedActions(deserializers: DownloadAction.Deserializer) {
        try {
            actionFile.load(deserializers).forEach { trackedDownloadStates[it.uri] = it }
        } catch (e: IOException) {
            Log.e("DownloadTracker", "Failed to load tracked actions", e)
        }

    }

    private fun startServiceWithAction(action: DownloadAction) {
        DownloadService.startWithAction(context, DemoDownloadService::class.java, action, false)
    }

    override fun onTaskStateChanged(downloadManager: DownloadManager, taskState: DownloadManager.TaskState) {
        val action = taskState.action
        val uri = action.uri
        if (action.isRemoveAction && taskState.state == DownloadManager.TaskState.STATE_COMPLETED || !action.isRemoveAction && taskState.state == DownloadManager.TaskState.STATE_FAILED) {
            // A download has been removed, or has failed. Stop tracking it.
            if (trackedDownloadStates.remove(uri) != null) {
                handleTrackedDownloadStatesChanged()
            }
        }
    }

    override fun onIdle(downloadManager: DownloadManager?) {
        //do nothing
    }

    override fun onInitialized(downloadManager: DownloadManager?) {
        //do nothing
    }

    private inner class StartDownloadDialogHelper(
            activity: Activity, private val downloadHelper: DownloadHelper, private val name: String) : DownloadHelper.Callback, DialogInterface.OnClickListener {

        private val builder: AlertDialog.Builder
        private val dialogView: View
        private val trackKeys: MutableList<TrackKey>
        private val trackTitles: ArrayAdapter<String>
        private val representationList: ListView

        init {
            builder = AlertDialog.Builder(activity)
                    .setTitle(R.string.exo_download_description)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, null)

            // Inflate with the builder's context to ensure the correct style is used.
            val dialogInflater = LayoutInflater.from(builder.context)
            dialogView = dialogInflater.inflate(R.layout.start_download_dialog, null)

            trackKeys = ArrayList()
            trackTitles = ArrayAdapter(
                    builder.context, android.R.layout.simple_list_item_multiple_choice)
            representationList = dialogView.findViewById(R.id.representation_list)
            representationList.choiceMode = ListView.CHOICE_MODE_MULTIPLE
            representationList.adapter = trackTitles
        }

        fun prepare() {
            downloadHelper.prepare(this)
        }

        override fun onPrepared(helper: DownloadHelper) {
            for (i in 0 until downloadHelper.periodCount) {
                val trackGroups = downloadHelper.getTrackGroups(i)
                for (j in 0 until trackGroups.length) {
                    val trackGroup = trackGroups.get(j)
                    for (k in 0 until trackGroup.length) {
                        trackKeys.add(TrackKey(i, j, k))
                        trackTitles.add(trackNameProvider.getTrackName(trackGroup.getFormat(k)))
                    }
                }
                if (!trackKeys.isEmpty()) {
                    builder.setView(dialogView)
                }
                builder.create().show()
            }
        }

        override fun onPrepareError(helper: DownloadHelper, e: IOException) {
            Toast.makeText(
                    context.applicationContext, R.string.download_start_error, Toast.LENGTH_LONG)
                    .show()
        }

        override fun onClick(dialog: DialogInterface, which: Int) {
            val selectedTrackKeys = ArrayList<TrackKey>()
            for (i in 0 until representationList.childCount) {
                if (representationList.isItemChecked(i)) {
                    selectedTrackKeys.add(trackKeys[i])
                }
            }
            if (!selectedTrackKeys.isEmpty() || trackKeys.isEmpty()) {
                // We have selected keys, or we're dealing with single stream content.
                val downloadAction = downloadHelper.getDownloadAction(Util.getUtf8Bytes(name), selectedTrackKeys)
                startDownload(downloadAction)
            }
        }
    }

    private fun startDownload(action: DownloadAction) {
        if (trackedDownloadStates.containsKey(action.uri)) {
            // This content is already being downloaded. Do nothing.
            return
        }
        trackedDownloadStates[action.uri] = action
        handleTrackedDownloadStatesChanged()
        startServiceWithAction(action)
    }

    private fun handleTrackedDownloadStatesChanged() {
        for (listener in listeners) {
            listener.onDownloadsChanged()
        }
        val actions = trackedDownloadStates.values.toTypedArray()
        actionFileWriteHandler.post {
            try {
                actionFile.store(*actions)
            } catch (e: IOException) {
                Log.e("DownloadTracker", "Failed to store tracked actions", e)
            }
        }
    }

}