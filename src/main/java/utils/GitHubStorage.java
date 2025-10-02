package utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.regex.Pattern;

// TODO: Temporary GitHub-based backup system. Will migrate to more robust solutions
//       (AWS S3, Cloud Storage, or dedicated database) in future releases
public class GitHubStorage {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    // private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss"); // to use hours, minutes and seconds
    private static final Pattern DATE_TAG = Pattern.compile("^\\d{8}$");
    //private static final Pattern DATE_TAG = Pattern.compile("^\\d{8}(\\d{6})?$"); // to use hours, minutes and seconds

    private static String getRepo() {
        return Env.getRequired("GITHUB_REPO");
    }

    private static String getToken() {
        return Env.getRequired("GITHUB_TOKEN");
    }

    private static HttpRequest.Builder base(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + getToken())
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Nat20bot-Backup/1.0");
    }

    private static String defaultBranch() throws IOException, InterruptedException {
        HttpRequest req = base("https://api.github.com/repos/" + getRepo()).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new IOException("Failed to get repo info: " + res.statusCode());
        return new JSONObject(res.body()).getString("default_branch");
    }

    private static String getFileSha(String path, String ref) throws IOException, InterruptedException {
        // Query GitHub API to check if file exists and get its SHA (needed for updates)
        String url = "https://api.github.com/repos/" + getRepo() + "/contents/" + path
                + (ref != null ? "?ref=" + ref : "");
        HttpRequest req = base(url).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 404) return null;
        if (res.statusCode() != 200)
            throw new IOException("Failed to get file SHA: " + res.statusCode());
        return new JSONObject(res.body()).getString("sha");
    }

    private static void putFile(String path, byte[] bytes, String message, String branch)
            throws IOException, InterruptedException {
        String sha = getFileSha(path, branch);
        JSONObject body = new JSONObject()
                .put("message", message)
                .put("content", Base64.getEncoder().encodeToString(bytes))
                .put("branch", branch);
        if (sha != null) body.put("sha", sha);

        HttpRequest req = base("https://api.github.com/repos/" + getRepo() + "/contents/" + path)
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200 && res.statusCode() != 201)
            throw new IOException("Failed to upload file: " + res.statusCode());
    }

    private static void createOrMoveTag(String tag, String commitSha) throws IOException, InterruptedException {
        JSONObject create = new JSONObject().put("ref", "refs/tags/" + tag).put("sha", commitSha);
        HttpRequest createReq = base("https://api.github.com/repos/" + getRepo() + "/git/refs")
                .POST(HttpRequest.BodyPublishers.ofString(create.toString())).build();
        HttpResponse<String> createRes = client.send(createReq, HttpResponse.BodyHandlers.ofString());
        
        if (createRes.statusCode() == 201) return;
        
        if (createRes.statusCode() == 422) {
            JSONObject patch = new JSONObject().put("sha", commitSha).put("force", true);
            HttpRequest patchReq = base("https://api.github.com/repos/" + getRepo() + "/git/refs/tags/" + tag)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(patch.toString()))
                    .build();
            HttpResponse<String> patchRes = client.send(patchReq, HttpResponse.BodyHandlers.ofString());
            if (patchRes.statusCode() == 200) return;
            throw new IOException("Failed to update tag: " + patchRes.statusCode());
        }
        throw new IOException("Failed to create tag: " + createRes.statusCode());
    }

    private static String latestDateTag() throws IOException, InterruptedException {
        // Fetch all repository tags and find the most recent one in yyyyMMdd format
        HttpRequest req = base("https://api.github.com/repos/" + getRepo() + "/tags").build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new IOException("Failed to list tags: " + res.statusCode());

        JSONArray arr = new JSONArray(res.body());
        String latest = null;
        for (int i = 0; i < arr.length(); i++) {
            String name = arr.getJSONObject(i).getString("name");
            // Find the lexicographically largest date tag (most recent)
            if (DATE_TAG.matcher(name).matches() && (latest == null || name.compareTo(latest) > 0)) {
                latest = name;
            }
        }
        if (latest == null)
            throw new IOException("No backup tags found in format yyyyMMdd");
        return latest;
    }

    private static byte[] downloadFile(String path, String ref) throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + getRepo() + "/contents/" + path + "?ref=" + ref;
        HttpRequest req = base(url).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new IOException("Failed to download file: " + res.statusCode());

        JSONObject json = new JSONObject(res.body());
        if (!"base64".equals(json.getString("encoding")))
            throw new IOException("Unexpected encoding: " + json.getString("encoding"));
        
        return Base64.getDecoder().decode(json.getString("content").replaceAll("\\s", ""));
    }

    /**
     * Upload backup file to GitHub with date tag
     */
    public static void upload(File file) throws IOException, InterruptedException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String branch = defaultBranch();
        String today = LocalDate.now().format(DATE_FMT);
        //String today = LocalDateTime.now().format(DATE_FMT); // to use hours, minutes and seconds
        String path = "backups/game_" + today + ".dnd";

        // Upload the backup file (overwrites if exists)
        putFile(path, bytes, "Backup " + today, branch);
        
        // Create or update a git tag pointing to this backup commit
        HttpRequest req = base("https://api.github.com/repos/" + getRepo() + "/commits?path=" + path + "&per_page=1").build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) {
            JSONArray commits = new JSONArray(res.body());
            if (commits.length() > 0) {
                String commitSha = commits.getJSONObject(0).getString("sha");
                createOrMoveTag(today, commitSha);
                System.out.println("[OK] Backup uploaded: " + path + " (tag: " + today + ")");
                return;
            }
        }
        System.out.println("[OK] Backup uploaded: " + path + " (no tag created)");
    }

    /**
     * Download the most recent backup available (not necessarily today's)
     * This ensures recovery even if bot crashed before creating today's backup
     */
    public static File downloadLatest() throws IOException, InterruptedException {
        String tag = latestDateTag();
        String filename = "game_" + tag + ".dnd";
        String path = "backups/" + filename;
        
        byte[] bytes = downloadFile(path, tag);
        
        // Save to local backup directory
        File backupDir = new File("backup/");
        backupDir.mkdirs();
        File dest = new File(backupDir, filename);
        Files.write(dest.toPath(), bytes);

        System.out.println("[OK] Downloaded backup from " + tag + " (" + bytes.length + " bytes)");
        return dest;
    }
}