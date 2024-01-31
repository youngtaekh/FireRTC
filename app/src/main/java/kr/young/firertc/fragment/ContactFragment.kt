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
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.AddContactActivity
import kr.young.firertc.ProfileActivity
import kr.young.firertc.R
import kr.young.firertc.adapter.ContactAdapter
import kr.young.firertc.db.AppRoomDatabase
import kr.young.firertc.model.User
import kr.young.firertc.vm.UserViewModel

class ContactFragment : Fragment(), OnClickListener {

    private val userVM = UserViewModel.instance
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
            userVM.getRelations()
            swipe.isRefreshing = false
        }

        contactAdapter = ContactAdapter()
        contactAdapter.setOnItemClickListener(clickListener, longClickListener)
        recyclerView.adapter = contactAdapter

        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = layoutManager

        userVM.contacts.observe(viewLifecycleOwner) {
            d(TAG, "contacts observe size ${it.size}")
            contactAdapter.submitList(it.toList())
            tvEmpty.visibility = if (it.isEmpty()) VISIBLE else INVISIBLE
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
        val user = contactAdapter.currentList[pos]
        userVM.deleteRelation(user.id)
        Observable.just(user)
            .observeOn(Schedulers.io())
            .doOnNext { AppRoomDatabase.getInstance()!!.userDao().deleteUser(user) }
            .subscribe()
        val list = mutableListOf<User>()
        list.addAll(userVM.contacts.value!!)
        list.removeAt(pos)
        userVM.setContactsLiveData(list)
    }

    private val clickListener = object: ContactAdapter.ClickListener {
        override fun onClick(pos: Int, v: View) {
            d(TAG, "onClick($pos, view)")
            userVM.selectedProfile = contactAdapter.currentList[pos]
            startActivity(Intent(context, ProfileActivity::class.java))
        }
    }

    private val longClickListener = object: ContactAdapter.LongClickListener {
        override fun onLongClick(pos: Int, v: View) {
            d(TAG, "onLongClick($pos, v)")
            val user = contactAdapter.currentList[pos]
            val builder = AlertDialog.Builder(requireContext())
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