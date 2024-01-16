package kr.young.firertc.util

class Config {
    companion object {
        const val DIRECTION = "direction"
        const val SPACE_ID = "spaceId"
        const val CREATED_AT = "createdAt"
        const val MODIFIED_AT = "modifiedAt"
        const val CONNECTED_AT = "connectedAt"
        const val TERMINATED_AT = "terminatedAt"
        const val TERMINATED = "terminated"
        const val NAME = "name"
        const val OS = "os"
        const val STATUS = "status"
        const val CALLS = "calls"
        const val FCM_TOKEN = "fcmToken"
        const val TYPE = "type"
        const val CALL_TYPE = "callType"
        const val CALL_ID = "callId"
        const val DATA = "data"
        const val TARGET_OS = "targetOS"
        const val SDP = "sdp"
        const val CANDIDATES = "candidates"
        const val USER_ID = "userId"
        const val CHAT_ID = "chatId"
        const val MESSAGE_ID = "messageId"
        const val MESSAGE = "message"
        const val MESSAGE_PAGE_SIZE = 50L
        const val HISTORY_PAGE_SIZE = 50L
        const val MIN_LONG = -1L
        const val MAX_LONG = 9_223_372_036_854_775_807L

        //chat
        const val TITLE = "title"
        const val PARTICIPANTS = "participants"
        const val IS_GROUP = "isGroup"
        const val LAST_MESSAGE = "lastMessage"
        const val LAST_SEQUENCE = "lastSequence"

        //message
        const val BODY = "body"
        const val SEQUENCE = "sequence"

        //relation
        const val FROM = "from"
        const val TO = "to"

        val exampleMessages = listOf(
            "How was your day? I was the best.",
            "How is the weather? It’s so hot here.",
            "What are you going to do? I will read a book.",
            "I’m not busy. Take your time, please.",
            "That’s what I’m saying. I was really surprised!",
            "Is there a hotel in this area? I had it until last year.",
            "I really don’t know what to say. I’m sorry.",
            "I’m about to leave. Please hold for a moment.",
            "Did you clean this place? It’s really clean.",
            "Have you heard of that? I was the only one who didn’t know.",
            "Please take me to the mart on the way to the hospital.",
            "Who are you meeting today? Why are you dressed up?",
            "Who’s your favorite movie star? I like the main character of Spider-Man.",
            "Would you like some coffee? Or do you want some water?",
            "Do you really think so? Well, I think we need to talk about it.",
        )
    }
}