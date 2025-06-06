package db

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.cdimascio.dotenv.dotenv
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider

/*
Add .env file to the root of the project (next to gitignore, build.gradle.kts, etc.)
and put database link into MONGODB_URI variable
*/
object MongoClientProvider {
    private val dotenv = dotenv()
    private val connectionString = ConnectionString(
        dotenv["MONGODB_URI"] ?: "mongodb://localhost:27017/"
    )

    private val pojoCodecProvider = PojoCodecProvider.builder()
        .automatic(true)
        .build()

    private val codecRegistry = CodecRegistries.fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        CodecRegistries.fromProviders(pojoCodecProvider)
    )

    private val settings = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .codecRegistry(codecRegistry)
        .build()

    val client: MongoClient = MongoClient.create(settings)
}