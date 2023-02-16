package kr.young.examplewebrtc.model

class User constructor() {
    lateinit var id: String
    lateinit var password: String
    lateinit var name: String

    constructor(
        id: String,
        password: String,
        name: String
    ): this() {
        this.id = id
        this.password = password
        this.name = name
    }

    constructor(data: Map<String, Any>): this() {
        this.id = data["id"]!! as String
        this.password = data["password"]!! as String
        this.name = data["name"]!! as String
    }

    fun toMap(): HashMap<String, String> {
        return hashMapOf(
            "id" to id,
            "password" to password,
            "name" to name
        )
    }

    companion object {
        const val COLLECTION = "users"
    }
}
