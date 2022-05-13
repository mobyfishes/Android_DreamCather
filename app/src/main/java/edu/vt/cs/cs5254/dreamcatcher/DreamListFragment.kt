package edu.vt.cs.cs5254.dreamcatcher

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs.cs5254.dreamcatcher.R
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamListBinding
import edu.vt.cs.cs5254.dreamcatcher.databinding.ListItemDreamBinding
import java.util.*

class DreamListFragment : Fragment() {

    interface Callbacks{
        fun onDreamSelected(dreamId: UUID)
    }

    private var callbacks: Callbacks? = null
    private var _binding: FragmentDreamListBinding? = null
    private val ui get() = _binding!!

    private val viewModel: DreamListViewModel by lazy {
        ViewModelProvider(this).get(DreamListViewModel::class.java)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private var adapter: DreamAdapter? = DreamAdapter(emptyList())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentDreamListBinding.inflate(inflater, container, false)
        val view = ui.root
        ui.dreamRecyclerView.layoutManager = LinearLayoutManager(context)
        ui.dreamRecyclerView.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.dreamListLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { dreams: List<Dream> ->
                dreams?.let {
                    updateUI(dreams)
                }
            })
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_dream -> {
                val dreamWithEntries = DreamWithEntries(Dream(), emptyList())
                dreamWithEntries.dreamEntries += DreamEntry(kind = DreamEntryKind.CONCEIVED, dreamId = dreamWithEntries.dream.id)
                viewModel.addDreamWithEntries(dreamWithEntries)
                callbacks?.onDreamSelected(dreamWithEntries.dream.id)
                true
            }
            R.id.delete_all_dreams -> {
                viewModel.deleteAllDreams()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(dreams: List<Dream>) {
        adapter = DreamAdapter(dreams)
        ui.dreamRecyclerView.adapter = adapter
    }

    inner class DreamHolder(val itemBinding: ListItemDreamBinding)
        : RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {
        private lateinit var dream: Dream
        init {
            itemView.setOnClickListener(this)
        }
        fun bind(dream: Dream) {
            this.dream = dream
            itemBinding.dreamItemTitle.text = this.dream.title
            itemBinding.dreamItemDate.text = this.dream.date.toString()
            if (dream.isFulfilled && !dream.isDeferred) {
                itemBinding.dreamItemImage.setImageResource(R.drawable.dream_fulfilled_icon)
                itemBinding.dreamItemImage.tag = R.drawable.dream_fulfilled_icon
                itemBinding.dreamItemImage.visibility = View.VISIBLE

            }
            else if (dream.isDeferred && !dream.isFulfilled){
                itemBinding.dreamItemImage.setImageResource(R.drawable.dream_deferred_icon)
                itemBinding.dreamItemImage.tag = R.drawable.dream_deferred_icon
                itemBinding.dreamItemImage.visibility = View.VISIBLE

            }
            else {
                itemBinding.dreamItemImage.visibility = View.INVISIBLE
                itemBinding.dreamItemImage.tag = 0
            }
        }
        override fun onClick(v: View) {
            callbacks?.onDreamSelected(dream.id)
        }
    }

    private inner class DreamAdapter(var dreams: List<Dream>)
        : RecyclerView.Adapter<DreamHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamHolder {
            val itemBinding = ListItemDreamBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return DreamHolder(itemBinding)
        }
        override fun getItemCount() = dreams.size

        override fun onBindViewHolder(holder: DreamHolder, position: Int) {
            val dream = dreams[position]
            holder.bind(dream)
        }
    }

    companion object {
        fun newInstance(): DreamListFragment {
            return DreamListFragment()
        }
    }
}