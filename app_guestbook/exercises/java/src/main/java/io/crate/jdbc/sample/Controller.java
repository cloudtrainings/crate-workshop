package io.crate.jdbc.sample;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import spark.Response;
import spark.utils.IOUtils;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import java.io.*;

import java.sql.SQLException;
import java.util.*;

import static spark.Spark.*;

public class Controller {

	private final Gson gson = new GsonBuilder().serializeNulls().create();

	private static final int INTERNAL_ERROR = 500;
	private static final int BAD_REQUEST = 400;
	private static final int NOT_FOUND = 404;

	private static final int NO_CONTENT = 204;
	private static final int CREATED = 201;
	private static final int OK = 200;

	public Controller(final DataProvider model) {

		String webFrontend=DataProvider.getProperty("web.frontend.app.location");

		System.out.println("web.frontend.app.location:"+webFrontend);

		externalStaticFileLocation(webFrontend);
		
		before(((request, response) -> {
			response.header("Access-Control-Allow-Origin", "*");
			response.header("Access-Control-Request-Method", "*");
			response.header("Access-Control-Allow-Headers", "*");
		}));

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

		get("/posts", (request, response) ->
		  model.getPosts(), gson::toJson);

		post("/posts", (request, response) -> {
			String body = request.body();
			if (body.isEmpty()) {
				return argumentRequired(response, "Request body is required");
			}

			Map post = gson.fromJson(body, Map.class);
			if (!post.containsKey("text")) {
				return argumentRequired(response, "Argument \"text\" is required");
			}

			Map user = (Map) post.get("user");
			if (!user.containsKey("location")) {
				return argumentRequired(response, "Argument \"location\" is required");
			}
			response.status(CREATED);
			return model.insertPost(post);
		}, gson::toJson);

		get("/post/:id", (request, response) -> {
			String id = request.params(":id");
			Map post = model.getPost(id);
			if (post.isEmpty()) {
				return notFound(response, (String.format("Post with id=\"%s\" not found", id)));
			}
			response.status(OK);
			return post;
		}, gson::toJson);

		put("/post/:id", (request, response) -> {
			String body = request.body();
			if (body.isEmpty()) {
				return argumentRequired(response, "Request body is required");
			}

			Map post = gson.fromJson(body, Map.class);
			if (!post.containsKey("text")) {
				return argumentRequired(response, "Argument \"text\" is required");
			}

			String id = request.params(":id");
			Map updatePost = model.updatePost(id, (String) post.get("text"));
			if (updatePost.isEmpty()) {
				return notFound(response, (String.format("Post with id=\"%s\" not found", id)));
			}
			response.status(OK);
			return updatePost;
		}, gson::toJson);

		delete("/post/:id", (request, response) -> {
			String id = request.params(":id");
			if (model.deletePost(id)) {
				response.status(NO_CONTENT);
				return response;
			} else {
				return gson.toJson(
						notFound(response, String.format("Post with id=\"%s\" not found", id))
						);
			}
		});

		put("/post/:id/like", (request, response) -> {
			String id = request.params(":id");
			Map post = model.incrementLike(id);
			if (post.isEmpty()) {
				return notFound(response, (String.format("Post with id=\"%s\" not found", id)));
			}
			response.status(OK);
			return post;
		}, gson::toJson);

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

		get("/images", (request, response) -> model.getBlobs(), gson::toJson);

		post("/images", (request, response) -> {
			String body = request.body();
			if (body.isEmpty()) {
				return argumentRequired(response, "Request body is required");
			}

			Map blobMap = gson.fromJson(body, Map.class);
			if (!blobMap.containsKey("blob")) {
				return argumentRequired(response, "Argument \"blob\" is required");
			}

			byte[] decoded = Base64.getDecoder().decode((String) blobMap.get("blob"));
			String digest = DigestUtils.shaHex(decoded);
			Map<String, String> responseMap = model.insertBlob(digest, decoded);

			response.status(Integer.parseInt(responseMap.get("status")));
			return responseMap;
		}, gson::toJson);

		get("/image/:digest", (request, response) -> {
			String digest = request.params(":digest");
			if (model.blobExists(digest)) {
				System.out.println("enters here  get(/image/:digest,");
				CloseableHttpResponse closeableHttpResponse = model.getBlob(digest);
				Arrays.stream(closeableHttpResponse.getAllHeaders()).forEach(header ->
				response.header(header.getName(), header.getValue())
						);

				System.out.println("aaaaaaaaaaaaa:"+ closeableHttpResponse.getStatusLine().getStatusCode());
				response.status(closeableHttpResponse.getStatusLine().getStatusCode());
				//String blea=IOUtils.toString(closeableHttpResponse.getEntity().getContent());

				InputStream in = closeableHttpResponse.getEntity().getContent();
				byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(in);

				response.type("image/gif");
				try (ServletOutputStream out = response.raw().getOutputStream()) {
					org.apache.commons.io.IOUtils.write(bytes, out);
					out.close();

				}
				return response;


			} else {
				return gson.toJson(
						notFound(response, String.format("Image with digest=\"%s\" not found", digest))
						);
			}
		});

		delete("/image/:digest", (request, response) -> {
			String digest = request.params(":digest");
			if (model.blobExists(digest)) {
				CloseableHttpResponse closeableHttpResponse = model.deleteBlob(digest);
				response.status(closeableHttpResponse.getStatusLine().getStatusCode());
				return response;
			} else {
				return gson.toJson(
						notFound(response, String.format("Image with digest=\"%s\" not found", digest))
						);
			}
		});

		post("/search", (request, response) -> {
			String body = request.body();
			if (body.isEmpty()) {
				return argumentRequired(response, "Request body is required");
			}
			Map bodyMap = gson.fromJson(body, Map.class);
			if (!bodyMap.containsKey("query_string")) {
				return argumentRequired(response, "Argument \"query_string\" is required");
			}
			return model.searchPosts((String) bodyMap.get("query_string"));
		}, gson::toJson);
		exception(SQLException.class, (e, request, response) -> {
			response.status(INTERNAL_ERROR);
			response.body(e.getLocalizedMessage());
		});

		get("/search", (request, response) -> {
			String body = request.body();
			if (body.isEmpty()) {
				return argumentRequired(response, "Request body is required");
			}
			Map bodyMap = gson.fromJson(body, Map.class);
			if (!bodyMap.containsKey("query_string")) {
				return argumentRequired(response, "Argument \"query_string\" is required");
			}
			return model.searchPosts((String) bodyMap.get("query_string"));
		}, gson::toJson);
		exception(SQLException.class, (e, request, response) -> {
			response.status(INTERNAL_ERROR);
			response.body(e.getLocalizedMessage());
		});
	}

	private Map<String, Object> notFound(Response response, String msg) {
		System.out.println("Not found: " + msg);
		return errorResponse(response, msg, NOT_FOUND);
	}

	private Map<String, Object> argumentRequired(Response response, String msg) {
		System.out.println("Argument required: " + msg);
		return errorResponse(response, msg, BAD_REQUEST);
	}

	private Map<String, Object> errorResponse(Response response, String msg, int code) {
		System.out.println("Error response: " + msg);
		response.status(code);
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("status", code);
		responseMap.put("error", msg);
		return responseMap;
	}

}
