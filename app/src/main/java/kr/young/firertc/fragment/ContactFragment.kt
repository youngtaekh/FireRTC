package kr.young.firertc.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.R
import kr.young.firertc.ProfileActivity
import kr.young.firertc.adapter.ContactAdapter
import kr.young.firertc.model.Relation
import kr.young.firertc.model.User
import kr.young.firertc.repo.RelationRepository
import kr.young.firertc.vm.UserViewModel

class ContactFragment : Fragment() {

    private val userViewModel = UserViewModel.instance
    private lateinit var contactList: MutableList<User>
    private lateinit var contactAdapter: ContactAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_home, container, false)
        val swipe = layout.findViewById<SwipeRefreshLayout>(R.id.swipe)
        val recyclerView = layout.findViewById<RecyclerView>(R.id.recycler_view)
        val tvEmpty = layout.findViewById<TextView>(R.id.tv_empty)

        swipe.setOnRefreshListener {
            d(TAG, "contacts swipe refresh")
            userViewModel.readAllRelation()
            swipe.isRefreshing = false
        }

        contactList = userViewModel.contacts
        contactAdapter = ContactAdapter(contactList)
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
                    contactAdapter.notifyDataSetChanged()
//                    for (i in 0 until contactList.size) {
//                        d(TAG, "nofityItemInserted($i)")
//                        contactAdapter.notifyItemInserted(i)
//                    }
//                    contactAdapter.notifyItemRangeInserted(0, contactList.size)
                }
            }
        }

        // Inflate the layout for this fragment
        return layout
    }

    override fun onResume() {
        super.onResume()
        d(TAG, "onResume")

        userViewModel.readAllRelation()
    }

    private fun removeContact(pos: Int) {
        val user = contactList[pos]
        userViewModel.deleteRelation(user.id)
        contactList.removeAt(pos)
        contactAdapter.notifyItemRemoved(pos)
    }

    private fun addTestContacts() {
        for (i in 0 until 15) {
            if (i < 10) {
                RelationRepository.post(Relation(to = "test0$i"))
//                UserRepository.post(User(id = "test0$i", name = "Test0$i"))
            } else {
                RelationRepository.post(Relation(to = "test$i"))
//                UserRepository.post(User(id = "test$i", name = "Test$i"))
            }
        }
    }

    private val clickListener = object: ContactAdapter.ClickListener {
        override fun onClick(pos: Int, v: View) {
            d(TAG, "onClick($pos, view)")
            userViewModel.selectedProfile = contactList[pos]
            startActivity(Intent(context, ProfileActivity::class.java))
        }
    }

    private val longClickListener = object: ContactAdapter.LongClickListener {
        override fun onLongClick(pos: Int, v: View) {
            d(TAG, "onLongClick($pos, v)")
            val user = contactList[pos]
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