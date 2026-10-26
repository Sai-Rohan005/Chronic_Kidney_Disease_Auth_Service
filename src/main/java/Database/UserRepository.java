package Database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@Lazy
public class UserRepository {
    private final MongoCollection<Document> users;
    public UserRepository(DB db) {
        this.users = db.getDatabase().getCollection("users");
        this.users.createIndex(Indexes.ascending("email"),
                new IndexOptions().unique(true));
    }

    public Document findUser(String email) {

        return users.find(Filters.eq("email", email)).first();
    }


    public String findRole(String email) {
        Document user = findUser(email);
        return user == null ? null : user.getString("role");
    }



    public void createUser(String name, String email, String password) {
        Document user = new Document("name", name)
                .append("email", email)
                .append("passwordHash", BCrypt.hashpw(password, BCrypt.gensalt()))
                .append("role", "Patient");
        users.insertOne(user);
    }

    public boolean updatePassword(String email, String password) {
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        UpdateResult result = users.updateOne(
                Filters.eq("email", email),
                Updates.set("passwordHash", passwordHash)
        );
        return result.getMatchedCount() > 0;
    }

    public List<Document> getDoctors(){
        return users.find(Filters.eq("role", "Doctor")).into(new ArrayList<>());
    }


}
