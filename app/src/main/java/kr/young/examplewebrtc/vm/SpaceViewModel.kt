package kr.young.examplewebrtc.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.i
import kr.young.examplewebrtc.fcm.SendFCM
import kr.young.examplewebrtc.fcm.SendFCM.FCMType
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.model.Space
import kr.young.examplewebrtc.model.User
import kr.young.examplewebrtc.repo.SpaceRepository
import kr.young.rtp.RTPManager
import kr.young.rtp.util.SDPEditor

class SpaceViewModel: ViewModel() {
    val isOffer = MutableLiveData<Boolean?>(null)
    val space = MutableLiveData<Space>()
    val participants = MutableLiveData<MutableMap<String, User>>(mutableMapOf())
    val calls = MutableLiveData<MutableList<Call>>(mutableListOf())
    val newSdp = MutableLiveData<String>()
    val newIce = MutableLiveData<String>()

    fun setOffer(isOffer: Boolean?) {
        this.isOffer.value = isOffer
    }

    private fun setSpace(space: Space?) {
        if (space != null) {
            d(TAG, "setSpace name ${space.name}")
            this.space.value = space
        }
    }

    private fun addCall(call: Call) {
        d(TAG, "addCall user ${call.userId}")
        val callList = getCalls()
        callList.add(call)
        calls.value = callList
    }

    internal fun setCalls(callList: MutableList<Call>) {
        d(TAG, "setCalls size ${callList.size}")
        calls.value = callList
    }

    fun getCalls() = calls.value!!

    fun setNewSdp(sdp: String) {
        newSdp.value = sdp
    }

    fun setNewIce(ice: String) {
        newIce.value = ice
    }

    private fun addParticipants(user: User) {
        d(TAG, "addParticipants userId ${user.id}")
        val participantList = participants.value!!
        participantList[user.id] = user
        participants.value = participantList
    }

    fun release() {
        d(TAG, "release")
        space.value = null
        participants.value = mutableMapOf()
        calls.value = mutableListOf()
    }

    fun createSpace(name: String) {
        d(TAG, "createSpace")
        setSpace(Space(name = name, createdBy = MyDataViewModel.instance.getMyId()))
        SpaceRepository.post(space.value!!)
    }

    fun getSpace(id: String = space.value!!.id) {
        d(TAG, "getSpace")
        SpaceRepository.getSpace(id) { document ->
            setSpace(document.toObject<Space>()!!)
        }
    }

    fun joinSpace(name: String) {
        d(TAG, "joinSpace")
        SpaceRepository.getActiveSpace(name) { documents ->
            d(TAG, "getActiveSpaceSuccess")
            if (documents.isEmpty) {
                createSpace(name)
                setOffer(true)
            } else {
                setSpace(documents.documents[0].toObject())
                CallViewModel.instance.getCallsBySpaceId(this.space.value!!.id)
            }
        }
    }

    fun updateSpaceStatus(status: Space.SpaceStatus) {
        if (space.value == null)
            return
        i(TAG, "updateSpaceStatus ${space.value!!.status} -> $status")
        val space = space.value!!
        space.status = status
        setSpace(space)
        SpaceRepository.updateStatus(space)
    }

    fun makeOffer() {
        d(TAG, "makeOffer")
        val call = Call(
            userId = MyDataViewModel.instance.getMyId(),
            token = MyDataViewModel.instance.myData.value!!.fcmToken,
            spaceId = space.value!!.id,
            direction = Call.CallDirection.Offer
        )

        CallViewModel.instance.setCall(call)
        addCall(call)
        SpaceRepository.addCallList(space.value!!.id, call.id)
    }

    fun makeAnswer(): String {
        d(TAG, "makeAnswer")
        val call = Call(
            userId = MyDataViewModel.instance.getMyId(),
            token = MyDataViewModel.instance.myData.value!!.fcmToken,
            spaceId = space.value!!.id,
            direction = Call.CallDirection.Answer
        )

        var sdp = ""
        var candidates = mutableListOf<String>()
        for (remoteCall in getCalls()) {
            i(TAG, "makeAnswer() call-${remoteCall.userId}, direction-${remoteCall.direction}")
//            CallViewModel.instance.getCall(callSimple.id)
            if (remoteCall.direction == Call.CallDirection.Offer) {
                d(TAG, "Find Offer Call!!!!!!!!!!!")
                sdp = remoteCall.sdp!!
                candidates = remoteCall.candidates
            }
            if (remoteCall.userId != MyDataViewModel.instance.myData.value!!.id) {
                SendFCM.sendMessage(
                    to = remoteCall.token!!,
                    type = FCMType.New,
                    spaceId = remoteCall.spaceId,
                    callId = remoteCall.id
                )
            }
        }

        CallViewModel.instance.setCall(call)
        addCall(call)
        SpaceRepository.addCallList(space.value!!.id, call.id)
        return SDPEditor().addIceCandidate(sdp, candidates as ArrayList<String>)
    }

    fun checkSpaceId(spaceId: String?) =
                spaceId != null &&
                space.value != null &&
                spaceId == space.value!!.id

    private object Holder {
        val INSTANCE = SpaceViewModel()
    }

    companion object {
        private const val TAG = "SpaceViewModel"
        val instance: SpaceViewModel by lazy { Holder.INSTANCE }
    }
}