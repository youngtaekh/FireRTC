package kr.young.examplewebrtc.util

class ResponseCode {
    companion object {
        const val OK = 20000

        //General code
        const val FIRESTORE_FAILURE = 10000
        const val FIRESTORE_FAILURE_TEXT = "Firestore Failure"
        //User Code 11000 ~
        const val WRONG_PASSWORD = 11000
        const val WRONG_PASSWORD_TEXT = "Incorrect password. Check again."
        const val NO_USER = 11001
        //Space Code 12000 ~
        //Call Code 13000 ~
    }
}