package edu.vt.cs.cs5254.dreamcatcher

import edu.vt.cs.cs5254.dreamcatcher.R
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamDetailBinding

import java.util.*
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs.cs5254.dreamcatcher.databinding.ListItemDreamBinding
import edu.vt.cs.cs5254.dreamcatcher.databinding.ListItemDreamEntryBinding
import java.io.File

private const val REQUEST_KEY_ADD_REFLECTION = "request_key"
private const val BUNDLE_KEY_REFLECTION_TEXT = "reflection_text"
private const val ARG_DREAM_ID = "dream_id"
class DreamDetailFragment : Fragment() {

    private var _binding: FragmentDreamDetailBinding? = null
    private val ui: FragmentDreamDetailBinding
        get() = _binding!!

    private lateinit var dreamWithEntries: DreamWithEntries

    private var mItemTouchHelper: ItemTouchHelper? = null

    private val viewModel: DreamDetailViewModel by lazy {
        ViewModelProvider(this).get(DreamDetailViewModel::class.java)
    }

    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var photoLauncher: ActivityResultLauncher<Uri>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val dreamID: UUID = arguments?.getSerializable(ARG_DREAM_ID) as UUID
        dreamWithEntries = DreamWithEntries(Dream(), emptyList())
        viewModel.loadDream(dreamID)

        photoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                updatePhotoView()
            }
            requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    private var adapter: DreamDetailFragment.DreamEntryAdapter? = DreamEntryAdapter(emptyList())

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDreamDetailBinding.inflate(inflater, container, false)
        val view = ui.root
        ui.dreamEntryRecyclerView.layoutManager = LinearLayoutManager(context)
        ui.dreamEntryRecyclerView.adapter = adapter

        //updateUI(this.dreamWithEntries.dreamEntries)

        val callback: ItemTouchHelper.Callback = SwipeToDeleteCallback(adapter!!)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper?.attachToRecyclerView(ui.dreamEntryRecyclerView)

        //refreshView()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.dreamWithEntriesLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { dreamWithEntries ->
                dreamWithEntries?.let {
                    this.dreamWithEntries = dreamWithEntries
                    photoFile = viewModel.getPhotoFile(dreamWithEntries.dream)
                    photoUri = FileProvider.getUriForFile(requireActivity(),
                        "edu.vt.cs.cs5254.dreamcatcher.fileprovider",
                        photoFile)
                    refreshView()
                }
            })
        //updateUI(dreamWithEntries.dreamEntries)
    }

    override fun onStart() {
        super.onStart()
        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(sequence: CharSequence?,
                                       start: Int, before: Int, count: Int) {
                dreamWithEntries.dream.title = sequence.toString()
            }
            override fun afterTextChanged(sequence: Editable?) { }
        }

        ui.dreamTitleText.addTextChangedListener(titleWatcher)

        ui.dreamFulfilledCheckbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isFulfilled = isChecked
                jumpDrawablesToCurrentState()
                refreshView()
            }
        }

        ui.dreamDeferredCheckbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isDeferred = isChecked
                jumpDrawablesToCurrentState()
                refreshView()
            }
        }


        ui.addReflectionButton.setOnClickListener{
            AddReflectionFragment().show(parentFragmentManager,REQUEST_KEY_ADD_REFLECTION )
        }

        parentFragmentManager.setFragmentResultListener(
            REQUEST_KEY_ADD_REFLECTION,
            viewLifecycleOwner)
        { _, bundle ->
            var getStringFromAdd = bundle.getSerializable(BUNDLE_KEY_REFLECTION_TEXT) as String
            dreamWithEntries.dreamEntries += DreamEntry(kind = DreamEntryKind.REFLECTION, text = getStringFromAdd, dreamId = dreamWithEntries.dream.id)
            updateUI(this.dreamWithEntries.dreamEntries)
        }

        //updateUI(this.dreamWithEntries.dreamEntries)
        //refreshView()
    }

    private fun updateEntriesList(addItem : DreamEntryKind, removeItem : DreamEntryKind){
        dreamWithEntries.dreamEntries += DreamEntry(kind = addItem, dreamId = dreamWithEntries.dream.id)

        for (dreamEntry in  dreamWithEntries.dreamEntries) {
            if (dreamEntry.kind == removeItem){
                dreamWithEntries.dreamEntries -= dreamEntry
                break
            }
        }
        updateUI(this.dreamWithEntries.dreamEntries)
    }

    private fun updateUI(entires : List<DreamEntry>){
        adapter = DreamEntryAdapter(entires)
        ui.dreamEntryRecyclerView.adapter = adapter
        ui.dreamTitleText.setText(dreamWithEntries.dream.title)
        updatePhotoView()
    }

    private fun refreshView(){

        if (dreamWithEntries.dream.isFulfilled){
            ui.dreamFulfilledCheckbox.isChecked = true
            ui.dreamDeferredCheckbox.isEnabled = false

            ui.addReflectionButton.isEnabled = false
            var flag1: Boolean = false
            for (dreamEntry in  dreamWithEntries.dreamEntries) {
                if (dreamEntry.kind == DreamEntryKind.FULFILLED){
                    flag1 = true
                    break
                }
            }

            if(!flag1){
                updateEntriesList(DreamEntryKind.FULFILLED, DreamEntryKind.DEFERRED)
            }

        } else if (dreamWithEntries.dream.isDeferred){
            ui.dreamDeferredCheckbox.isChecked = true
            ui.dreamFulfilledCheckbox.isEnabled = false
            ui.addReflectionButton.isEnabled = true

            var flag1: Boolean = false
            for (dreamEntry in  dreamWithEntries.dreamEntries) {
                if (dreamEntry.kind == DreamEntryKind.DEFERRED){
                    flag1 = true
                    break
                }
            }
            if(!flag1){
                updateEntriesList(DreamEntryKind.DEFERRED, DreamEntryKind.FULFILLED)
            }
        }
        else{
            ui.dreamFulfilledCheckbox.isEnabled = true
            ui.dreamDeferredCheckbox.isEnabled = true
            ui.addReflectionButton.isEnabled = true

            dreamWithEntries.dream.isDeferred = false
            dreamWithEntries.dream.isFulfilled = false

            for (dreamEntry in  dreamWithEntries.dreamEntries) {
                if (dreamEntry.kind == DreamEntryKind.DEFERRED || dreamEntry.kind == DreamEntryKind.FULFILLED){
                    dreamWithEntries.dreamEntries -= dreamEntry
                }
            }
        }

        updateUI(this.dreamWithEntries.dreamEntries)
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveDreamWithEntries(dreamWithEntries)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(dreamID: UUID): DreamDetailFragment {
            val args = Bundle().apply{
                putSerializable(ARG_DREAM_ID, dreamID)
            }
            return DreamDetailFragment().apply {
                arguments = args
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        //callbacks = null
    }

    inner class DreamEntryHolder(val itemBinding: ListItemDreamEntryBinding)
        : RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {
        private lateinit var entry: DreamEntry

        init {
            itemView.setOnClickListener(this)
        }

        fun bind (entry: DreamEntry) {
            this.entry = entry
            updateEntryButton(itemBinding.dreamEntryButton, entry)
        }

        private fun updateEntryButton(button : Button, entry: DreamEntry){

            if (entry.kind == DreamEntryKind.CONCEIVED){
                button.apply {
                    text = "CONCEIVED"
                    isVisible = true
                    isEnabled = true
                }
            }
            else if (entry.kind == DreamEntryKind.DEFERRED){
                button.apply {
                    text = "DEFERRED"
                    isVisible = true
                    isEnabled = true
                    backgroundTintList = ColorStateList.valueOf( Color.parseColor("#5FD453"))
                }
            }
            else if (entry.kind == DreamEntryKind.FULFILLED){
                button.apply {
                    text = "FULFILLED"
                    isVisible = true
                    isEnabled = true
                    backgroundTintList = ColorStateList.valueOf( Color.parseColor("#5FD453"))
                }
            }
            else if (entry.kind == DreamEntryKind.REFLECTION){
                button.apply {
                    val myString = DateFormat.format("MMM dd yyyy",  entry.date)
                    text = myString.toString() + ": "+ entry.text
                    isVisible = true
                    isEnabled = true
                    backgroundTintList = ColorStateList.valueOf( Color.parseColor("#e95d2a"))
                }
            }
        }

        override fun onClick(v: View) {
            //callbacks?.onDreamEntrySelected(entry.id)
        }
    }

    inner class DreamEntryAdapter (var dreamEntries: List<DreamEntry>)
        : RecyclerView.Adapter<DreamEntryHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamDetailFragment.DreamEntryHolder {
            val itemBinding = ListItemDreamEntryBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return DreamEntryHolder(itemBinding)
        }
        override fun getItemCount() = dreamEntries.size

        fun deleteItem(i : Int){
            if (dreamWithEntries.dreamEntries[i].kind == DreamEntryKind.REFLECTION){
                dreamWithEntries.dreamEntries -= dreamWithEntries.dreamEntries[i]
            }
            updateUI( dreamWithEntries.dreamEntries)
        }

        override fun onBindViewHolder(holder: DreamDetailFragment.DreamEntryHolder, position: Int) {
            val dreamEntry = dreamEntries[position]
            holder.bind(dreamEntry)
        }
    }


    //Photo Menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_detail, menu)
        val cameraAvailable = PictureUtils.isCameraAvailable(requireActivity())
        val menuItem = menu.findItem(R.id.take_dream_photo)
        menuItem.apply {
            //Log.d(TAG, "Camera available: $cameraAvailable")
            isEnabled = cameraAvailable
            isVisible = cameraAvailable
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_dream -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getDreamReport())
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.share_dream_subject))
                }.also{ intent ->
                    val chooserIntent =
                        Intent.createChooser(intent, getString(R.string.send_report))
                    startActivity(chooserIntent)
                }

                true
            }
            R.id.take_dream_photo -> {
                val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                }
                requireActivity().packageManager
                    .queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    .forEach { cameraActivity ->
                        requireActivity().grantUriPermission(
                            cameraActivity.activityInfo.packageName,
                            photoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                photoLauncher.launch(photoUri)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun getDreamReport() : String {
        val dream_title = getString(R.string.share_dream_title, dreamWithEntries.dream.title) + '\n'
        val dateString = DateFormat.format("MMM dd, yyyy", dreamWithEntries.dream.date).toString()
        val dream_head = getString(R.string.share_dream_head, dateString) + "\n"
        val dream_reflections = getString(R.string.share_reflections) + "\n"
        var entiresString: String = ""

        for (entry in dreamWithEntries.dreamEntries) {
            if (entry.kind == DreamEntryKind.REFLECTION) {
                val newString = "- " + entry.text + "\n"
                entiresString += newString
            } else if (entry.kind == DreamEntryKind.DEFERRED) {
                entiresString += getString(R.string.share_dream_end) + "\n"
                break
            }
            else if (entry.kind == DreamEntryKind.FULFILLED) {
                entiresString += "this dream has been Fulfilled" + "\n"
                break
            }
        }
        return getString(R.string.share_dream_report, dream_title, dream_head, dream_reflections, entiresString)
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = PictureUtils.getScaledBitmap(photoFile.path, 133, 130)
            ui.dreamPhoto.setImageBitmap(bitmap)
        } else {
            ui.dreamPhoto.setImageDrawable(null)
        }
    }
}
















