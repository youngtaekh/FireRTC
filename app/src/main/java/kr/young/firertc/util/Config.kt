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

        const val PORTFOLIO_ID = "portfolioId"
        const val IN_PRICE = "inPrice"
        const val OUT_PRICE = "outPrice"
        const val CURRENT = "current"
        const val RATE = "rate"
        const val CURRENCY = "currency"
        const val TICKER = "ticker"
        const val AMOUNT = "amount"
        const val AVERAGE_PRICE = "averagePrice"
        const val CURRENT_PRICE = "currentPrice"
        const val PRICE = "price"
        const val KRW = "KRW"
        const val KRW_SYMBOL = "₩"
        const val USD = "USD"
        const val USD_SYMBOL = "$"
        const val JPY = "JPY"
        const val JPY_SYMBOL = "¥"
        const val CNY = "CNY"
        const val CNY_SYMBOL = "¥"
        const val TWD = "TWD"
        const val TWD_SYMBOL = "NT$"
        const val GBP = "GBP"
        const val GBP_SYMBOL = "£"
        const val WITHDRAW = "WITHDRAW"
        const val DEPOSIT = "DEPOSIT"
        val CURRENCY_ARRAY = arrayOf(KRW, USD, JPY, CNY, TWD, GBP)

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
        val IMAGE_LIST = listOf(
            "https://cdn.pixabay.com/photo/2020/05/17/20/21/cat-5183427_1280.jpg",
            "https://cdn.pixabay.com/photo/2014/04/13/20/49/cat-323262_1280.jpg",
            "https://cdn.pixabay.com/photo/2017/11/09/21/41/cat-2934720_1280.jpg",
            "https://cdn.pixabay.com/photo/2017/12/11/15/34/lion-3012515_1280.jpg",
            "https://cdn.pixabay.com/photo/2017/07/25/01/22/cat-2536662_1280.jpg",
            "https://cdn.pixabay.com/photo/2015/03/27/13/16/maine-coon-694730_1280.jpg",
            "https://cdn.pixabay.com/photo/2015/04/23/21/59/tree-736877_1280.jpg",
            "https://cdn.pixabay.com/photo/2018/08/23/22/29/girl-3626901_640.jpg",
            "https://cdn.pixabay.com/photo/2017/06/07/08/43/head-2379686_640.png",
            "https://cdn.pixabay.com/photo/2017/10/22/17/54/wolf-2878633_640.jpg",
            "https://cdn.pixabay.com/photo/2020/07/08/19/13/girl-5384878_640.jpg",
            "https://cdn.pixabay.com/photo/2017/04/06/19/34/girl-2209147_640.jpg",
            "https://cdn.pixabay.com/photo/2017/06/01/07/31/elephant-2362696_640.png",
            "https://cdn.pixabay.com/photo/2017/03/26/11/33/binary-2175285_640.jpg",
            "https://cdn.pixabay.com/photo/2020/06/21/15/54/bohemian-5325610_640.png",
            "https://cdn.pixabay.com/photo/2017/06/07/08/43/head-2379687_640.png",
            "https://cdn.pixabay.com/photo/2013/07/18/09/11/woman-163425_640.jpg",
            "https://cdn.pixabay.com/photo/2019/05/05/20/29/portrait-4181643_640.jpg",
            "https://cdn.pixabay.com/photo/2019/12/02/20/15/girl-4668620_640.jpg",
            "https://cdn.pixabay.com/photo/2020/02/11/18/35/profile-4840593_640.jpg",
            "https://cdn.pixabay.com/photo/2017/06/21/01/20/model-2425659_640.jpg",
            "https://cdn.pixabay.com/photo/2016/08/22/22/23/cat-1613088_640.jpg",
            "https://cdn.pixabay.com/photo/2017/10/22/17/54/wolf-2878633_640.jpg",
        )

        val BACKGROUND = listOf(
            "https://i.pinimg.com/736x/ea/41/b0/ea41b0956cda2f60cb3aa9553dd32e09.jpg",
            "https://i.pinimg.com/736x/9a/1c/b5/9a1cb580823d0054d083d32ab354509b.jpg",
            "https://i.pinimg.com/736x/aa/2e/f4/aa2ef41f1e7e96a74c033cff58c5ef03.jpg",
            "https://i.pinimg.com/736x/97/57/b3/9757b35bf3f2f853a29a87bb852e52ce.jpg",
            "https://i.pinimg.com/736x/1f/f9/d0/1ff9d0dd8ba9ef565f4149b1f3fa7ea7.jpg"
        )
    }
}