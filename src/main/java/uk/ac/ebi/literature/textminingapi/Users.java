package uk.ac.ebi.literature.textminingapi;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "#{@environment.getProperty('mongoDb.usersCollection')}")
public class Users {
    @Id
    public ObjectId _id;
    public String username;
    public String password;
    

    public Users() {
    }

    public void set_id(ObjectId _id) {
        this._id = _id;
    }

    public String get_id() {
        return this._id.toHexString();
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
    
}