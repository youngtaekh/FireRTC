package kr.young.firertc.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.AddContactActivity
import kr.young.firertc.ProfileActivity
import kr.young.firertc.R
import kr.young.firertc.adapter.ContactAdapter
import kr.young.firertc.vm.UserViewModel

class ContactFragment : Fragment(), OnClickListener {

    private val userViewModel = UserViewModel.instance
    private lateinit var contactAdapter: ContactAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_home, container, false)
        val swipe = layout.findViewById<SwipeRefreshLayout>(R.id.swipe)
        val recyclerView = layout.findViewById<RecyclerView>(R.id.recycler_view)
        val tvEmpty = layout.findViewById<TextView>(R.id.tv_empty)
        val fabAdd: FloatingActionButton = layout.findViewById(R.id.fab_add)
        fabAdd.visibility = GONE

        swipe.setOnRefreshListener {
            d(TAG, "contacts swipe refresh")
            userViewModel.getRelations()
            swipe.isRefreshing = false
        }

        contactAdapter = ContactAdapter(userViewModel.contacts)
        contactAdapter.setOnItemClickListener(clickListener, longClickListener)
        recyclerView.adapter = contactAdapter

        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = layoutManager

        userViewModel.refreshContacts.observe(viewLifecycleOwner) {
            if (it) {
                d(TAG, "refresh contacts observe")
                if (userViewModel.contacts.isEmpty()) {
                    tvEmpty.visibility = VISIBLE
                } else {
                    tvEmpty.visibility = INVISIBLE
                    contactAdapter.notifyItemRangeChanged(0, userViewModel.contacts.size)
                }
            }
        }

        // Inflate the layout for this fragment
        return layout
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fab_add -> { startActivity(Intent(context, AddContactActivity::class.java)) }
        }
    }

    private fun removeContact(pos: Int) {
        val user = userViewModel.contacts[pos]
        userViewModel.deleteRelation(user.id)
        userViewModel.contacts.removeAt(pos)
        contactAdapter.notifyItemRemoved(pos)
    }

    private val clickListener = object: ContactAdapter.ClickListener {
        override fun onClick(pos: Int, v: View) {
            d(TAG, "onClick($pos, view)")
            userViewModel.selectedProfile = userViewModel.contacts[pos]
            startActivity(Intent(context, ProfileActivity::class.java))
        }
    }

    private val longClickListener = object: ContactAdapter.LongClickListener {
        override fun onLongClick(pos: Int, v: View) {
            d(TAG, "onLongClick($pos, v)")
            val user = userViewModel.contacts[pos]
            val builder = AlertDialog.Builder(context!!)
                .setTitle(user.name)
                .setMessage(getString(R.string.delete_contact))
                .setCancelable(true)
                .setPositiveButton(
                    R.string.confirm
                ) { _, _ -> removeContact(pos) }
            builder.show()
        }
    }

    companion object {
        private const val TAG = "ContactFragment"
    }
}