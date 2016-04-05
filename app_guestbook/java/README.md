# Getting Started with the Crate.IO JDBC Sample App
This Crate JDBC backend uses [Spark][1] as web framework and the Crate [JDBC driver][2].

## Requirements
- [Java 8][3]

## Usage
To build and run the sample application we use the [Apache Maven][4] build tool.

### Build the Sample Application

```console
mvn clean install
```

See _README.txt_ in root folder of the project for instructions on how to create table schemas and populate the country data.

### Run Backend Application
By default the sample application runs on port _8080_.

```console
mvn exec:java -Dexec.mainClass="io.crate.jdbc.sample.App"
```

The port can be explicitly specified:

```console
$ mvn exec:java -Dexec.mainClass="io.crate.jdbc.sample.App"  -Dexec.args="8080"
```

Java backend only works with the following change into src/main/java/io/crate/jdbc/sample/Controller.java:

```console
The Java backend works only with this change below, according to http://www.mastertheboss.com/cool-stuff/create-a-rest-services-layer-with-spark
I've added in src/main/java/io/crate/jdbc/sample/Controller.java the following thing:

        options("/*", (request,response)->{

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if(accessControlRequestMethod != null){
            response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });
I've done what they say about activating CORS in Spark (search for CORS in the text.)  
```


[1]: http://sparkjava.com/  
[2]: https://github.com/crate/crate-jdbc  
[3]: http://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html  
[4]: https://maven.apache.org/index.html  

##solution to reuse Jetty embedded also as web server  

Add new attribute in the config.properties file:  

> web.frontend.app.location=/home/ubuntu/crate-java-php-python-demo/frontend

In the Controller.java should be added following lines at the beginning of the constructor:

> String webFrontend=DataProvider.getProperty("web.frontend.app.location");
    	
> externalStaticFileLocation(webFrontend);


##Add comments to already inserted post  

###Backend  
Create new table where will be stored the comments: ../sql/addCommentsScema.sql  
 
```console
DROP TABLE IF EXISTS guestbook.comments;
CREATE TABLE guestbook.comments (
    id STRING PRIMARY KEY,
    text STRING INDEX USING FULLTEXT WITH (analyzer = 'english'),
    created TIMESTAMP,
    post_id STRING
) WITH (number_of_replicas = '0-2');
```  

```bash
   sudo /usr/share/crate/bin/crash < sql/addCommentsSchema.sql
```  

Create new methods in the DataProvider class for insert/retrieve the data in Crate:  

```console
.................
private static final String COMMENTS_TABLE = "guestbook.comments";
..................

 public Map<String, Object> insertComment(String postId,String text) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(String.format(
                    "INSERT INTO %s " +
                    "(id, text, created, post_id) " +
                    "VALUES (?, ?, ?, ?)", COMMENTS_TABLE));
            String id = UUID.randomUUID().toString();
            statement.setString(1, id);
            statement.setObject(2, text);
            statement.setObject(3, System.currentTimeMillis());
            statement.setObject(4, postId);

            int res=statement.executeUpdate();
            System.out.println("rez:"+res);
            if (res == 0) {
                return ImmutableMap.of();
            }
            connection.createStatement()
                    .execute(String.format("REFRESH TABLE %s", COMMENTS_TABLE));
            return getPost(postId);

        }

  public List<Map<String, Object>> getPostWithComments(String id) throws SQLException {
        String sql=String.format(
                "SELECT p.*, c.name as country, c.geometry as area, comment.* " +
                "FROM %s AS p , %s AS c, %s as comment " +
                "WHERE  within(p.user['location'], c.geometry) "+
                "AND p.id=comment.post_id " +
                "AND p.id = ? ORDER BY comment.created DESC ", POST_TABLE,COUNTRIES_TABLE,COMMENTS_TABLE);
        System.out.println(sql +" " + id);
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, id);
        ResultSet results = statement.executeQuery();
        return resultSetToListOfMaps(results);
    }


```  
  
Add new requests endpoint via Spark framework in Controller.java:  

```console
.....................................
 put("/post/:id/comment/:text", (request, response) -> {
                        String id = request.params(":id");
                        String text = request.params(":text");
                        System.out.println("Add new comment for post:"+id);
                        Map post = model.insertComment(id,text);
                        if (post.isEmpty()) {
                                return notFound(response, (String.format("Post with id=\"%s\" not found", id)));
                        }
                        response.status(OK);
                        return post;
                }, gson::toJson);

   get("/post/:id/comments", (request, response) ->
                model.getPostWithComments(request.params(":id")), gson::toJson);
..............................................

```  

###Frontend

Add elements in the post.html and the end of the main div    

```console
..........................................
 <div
                 id="comment-{{ comment.id }}"
                 ng-include="'comments.html'">

        </div>
        </p>

                <p>
                        <label for="text">Your comment. Number of comments: {{comments[post.id].length}}</label>
                        <textarea class="form-control" id="comment-{{ post.id }}" rows="1"></textarea>
                </p>

        <p>
                <button type="button" class="btn btn-sm btn-default"
                        ng-click="guestbookCtlr.commentPost(post)">
                        <span class="badge">Send</span>
                </button>
        </p>

......................................................

```  
Create new file comments.html with the content from bellow:  

```console  
<div ng-repeat="comment in comments[post.id] track by comment.id">
        <p class="lead">
                <b> {{ comment.text }}</b> wrote on <em>{{ comment.created |
                        date:'MMM d, y h:mm a' }}</em>
        </p>
</div>

```

Add new js method in app.js inside  .controller('GuestbookController', function($scope, $q, apiHost, Api, Location, objectArrayIndexOf) { :  

```console
........................................
$scope.comments = {};

// comment an existing post
    this.commentPost = function(post) {
        var text=document.getElementById('comment-'+post.id);

     // window.alert("comment post:"+post.id+" "+text.value);
      var api = new Api('/post/' + post.id + '/comment/'+text.value);
      api.put().then(function(response) {
         // window.alert(response);
        },function(e) {
        console.warn(e);
        window.alert('Liking the post failed.');
      });
       //loadComments(post);
      loadPosts();
      text.value='';
    };

var loadComments = function(post) {
        var api = new Api('/post/'+post.id+'/comments');
        api.get().then(function(response) {
          $scope.comments[post.id] = response.data;
        }, function(e) {
          console.warn(e);
          $scope.comments = {};
        });
      };

............................................

```   
 
Load comments for each post inside loadPosts method:  
  
```console

.....................................................

        for (var i=0; i<$scope.posts.length; i++) {
            loadComments($scope.posts[i]);
        }

........................................................


var loadPosts = function() {
      var api = new Api('/posts');
      api.get().then(function(response) {
        $scope.posts = response.data;
        for (var i=0; i<$scope.posts.length; i++) {
            loadComments($scope.posts[i]);
        }
      }, function(e) {
        console.warn(e);
        $scope.posts = [];
      });
    };

```


##Report of the posts coming from each distinct location

###Backend

Add new request endpoint via Spark framework in Controller.java:  

```console  
       get("/reports/countPostsByDistinctLocation", (request, response) -> {

			 List<Map<String, Object>> posts=model.getPosts();
			 HashMap<Object,Integer> nrOfPostsByLocation=new HashMap<>();
        	 for(Map<String, Object> records:posts){
        		System.out.println("Location:"+records.get("country"));
        		Integer count=nrOfPostsByLocation.get(records.get("country"));
        		if(count == null)
        			count=0;
        		nrOfPostsByLocation.put(records.get("country"),count+1);
        		
        	 }
			System.out.println("Report size:"+nrOfPostsByLocation.size());
			Object[] keys=nrOfPostsByLocation.keySet().toArray();
			 List<Map<String, Object>> retList=new ArrayList<>();
        	 for(Object key:keys){
        		 System.out.println( key+" " + nrOfPostsByLocation.get(key));
        		 Map<String, Object> row=new HashMap<>();
        		 row.put("location",key);
        		 row.put("nrofposts", nrOfPostsByLocation.get(key));
        		 retList.add(row);
        	 }
        	
       	 return retList;
		} , gson::toJson);
  
```  

###Frontend

Add following code in index.html just inside <h2 class="post-item">  

```console  
<table style="width:100%">
				  <tr>
				    <th>Location</th>
				    <th>Number of Posts</th> 
				  </tr>
		  
		  <tr ng-repeat="row in postonlocation">
    			<td>{{row.location}}</td>
                 <td>{{row.nrofposts}}</td> 
         </tr>
          </table>
          <br/>
``` 

Add new function in app.js:

```console 
 
var loadReport = function() {
    	
        var api = new Api('/reports/countPostsByDistinctLocation');
        api.get().then(function(response) {
          $scope.postonlocation = response.data;          
        }, function(e) {
          console.warn(e);
          $scope.postonlocation = [];
        });
      };
``` 

Load report inside loadPosts and submitPost  method:  

```console

  var loadPosts = function() {
      var api = new Api('/posts');
      api.get().then(function(response) {
        $scope.posts = response.data;
        for (var i=0; i<$scope.posts.length; i++) {
            loadComments($scope.posts[i]);
        }
        loadReport();
      }, function(e) {
        console.warn(e);
        $scope.posts = [];
      });
    };
    
    var submitPost = function() {
      var api = new Api('/posts');
      api.post($scope.formdata).then(function(response) {
        var posts = response.data;
        for (var i=0; i<posts.length; i++) {
          $scope.posts.unshift(posts[0]);
        }
        resetForm();
        loadReport();
      }, function(e) {
        console.warn(e);
        window.alert('Creating the post failed.');
      })
    };

  
``` 

Notes: files which contains all above changes are in exercises folder.  


