package Database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClientSettings;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static org.bson.codecs.configuration.CodecRegistries.*;

@Component
@Lazy
public class DB implements AutoCloseable {

    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public DB() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        String uri = dotenv.get("MONGODB_URI");
        String dbName = dotenv.get("MONGODB_DATABASE");

        if (uri == null || dbName == null) {
            throw new IllegalStateException(
                    "MONGODB_URI and MONGODB_DATABASE must be configured"
            );
        }

        CodecRegistry pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(
                        PojoCodecProvider.builder()
                                .automatic(true)
                                .build()
                )
        );

        mongoClient = MongoClients.create(uri);

        database = mongoClient.getDatabase(dbName)
                .withCodecRegistry(pojoCodecRegistry);

        initializeCollections();
    }

    private void initializeCollections() {
        createCollectionIfNotExists("users");
        createCollectionIfNotExists("doctors");
        createCollectionIfNotExists("appointments");
        createCollectionIfNotExists("reports");
    }

    private void createCollectionIfNotExists(String name) {
        if (!database.listCollectionNames().into(
                new java.util.ArrayList<>()
        ).contains(name)) {
            database.createCollection(name);
        }
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    @Override
    public void close() {
        mongoClient.close();
    }
}