/**
 * This demo illustrates the compact nature of Spark when paired with the annotation processor Lombok
 */
import static spark.Spark.get;
import static spark.Spark.post;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// This demo will be used in the context of a blog service
public class BlogServiceDemo {

    private static final int HTTP_BAD_REQUEST = 400;

    // Simple interface for validating data of Post object
    interface Validable {
        boolean isValid();
    }

    @Data // Lombok's Data annotation used for defining getters and setters
    // Object representing post data that will be used for validation
    static class NewPostPayload {
        private String title;
        private List categories = new LinkedList<>();
        private String content;

        // Defines interface method
        public boolean isValid() {
            return title != null && !title.isEmpty() && !categories.isEmpty();
        }
    }

    // Model object used to store sample posts (data would be retrieved from a DB ordinarily
    public static class Model {
        private int nextId = 1;
        private Map posts = new HashMap<>();


        @Data // Lombok's Data annotation used for defining getters and setters
        // The sample post data used with model
        class Post {
            private int id;
            private String title;
            private List categories;
            private String content;
        }

        // Creates a post using field data from Post
        public int createPost(String title, String content, List categories){
            int id = nextId++;
            Post post = new Post();
            post.setId(id);
            post.setTitle(title);
            post.setContent(content);
            post.setCategories(categories);
            posts.put(id, post);
            return id;
        }

        // Returns all posts
        public List getAllPosts(){
            return (List) posts.keySet().stream().sorted().map((id) -> posts.get(id)).collect(Collectors.toList());
        }
    }

    // Converts post data to JSON string using mapper
    public static String dataToJson(Object data) {
        // Returns json string if valid
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, data);
            return sw.toString();
        // Throws exception otherwise
        } catch (IOException e){
            throw new RuntimeException("IOException from a StringWriter?");
        }
    }

    public static void main( String[] args) {
        Model model = new Model();

        // Insert a post (using HTTP post method)
        post("/posts", (req, res) -> {
            try {
                // Instantiate a mapper for post data, instantiate sample post payload using data from mapper
                ObjectMapper mapper = new ObjectMapper();
                NewPostPayload creation = mapper.readValue(req.body(), NewPostPayload.class);
                // If field data is not valid, change status to 400 and return
                if (!creation.isValid()) {
                    res.status(HTTP_BAD_REQUEST);
                    return "";
                }
                // Create an id from post data stored in model object, set status/type, and return ID
                int id = model.createPost(creation.getTitle(), creation.getContent(), creation.getCategories());
                res.status(200);
                res.type("application/json");
                return id;
                // Handle JSON Parse Exception
            } catch (JsonParseException jpe) {
                res.status(HTTP_BAD_REQUEST);
                return "";
            }
        });

        // Get all posts (using HTTP get method)
        get("/posts", (request, response) -> {
            response.status(200);
            response.type("application/json");
            return dataToJson(model.getAllPosts());
        });
    }
}