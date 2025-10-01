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
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.regex.Pattern;

// TODO Temporary GitHub-based storage system, to be replaced in future releases
public class GitHubStorage {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final DateTimeFormatter TAG_FMT = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd
    private static final Pattern DATE_TAG = Pattern.compile("^\\d{8}$"); // e.g. 20251001

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

    private static String[] ownerRepo() {
        String[] p = getRepo().split("/", 2);
        if (p.length != 2)
            throw new IllegalStateException("GITHUB_REPO must be in the form 'owner/repo'");
        return p;
    }

    /** Returns the repository's default branch */
    private static String defaultBranch() throws IOException, InterruptedException {
        String[] or = ownerRepo();
        HttpRequest req = base("https://api.github.com/repos/" + or[0] + "/" + or[1]).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new IOException("Repo info failed: " + res.statusCode() + " " + res.body());
        return new JSONObject(res.body()).getString("default_branch");
    }

    /** Returns the SHA of a file at a given ref, or null if it doesn't exist */
    private static String getFileSha(String path, String ref) throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + getRepo() + "/contents/" + path
                + (ref != null ? "?ref=" + ref : "");
        HttpRequest req = base(url).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) {
            return new JSONObject(res.body()).getString("sha");
        } else if (res.statusCode() == 404) {
            return null;
        }
        throw new IOException("getFileSha failed: " + res.statusCode() + " " + res.body());
    }

    /** Creates or updates a file in the repository */
    private static JSONObject putFile(String path, byte[] bytes, String message, String branch)
            throws IOException, InterruptedException {
        String sha = getFileSha(path, branch); // can be null
        JSONObject body = new JSONObject()
                .put("message", message)
                .put("content", Base64.getEncoder().encodeToString(bytes))
                .put("branch", branch);
        if (sha != null)
            body.put("sha", sha);

        HttpRequest req = base("https://api.github.com/repos/" + getRepo() + "/contents/" + path)
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200 && res.statusCode() != 201) {
            throw new IOException("putFile failed: " + res.statusCode() + " " + res.body());
        }
        return new JSONObject(res.body()); // contains "commit" -> "sha"
    }

    /** Creates a new tag or moves an existing one to a given commit SHA */
    private static void createOrMoveTagTo(String tag, String commitSha) throws IOException, InterruptedException {
        // try to create
        JSONObject create = new JSONObject().put("ref", "refs/tags/" + tag).put("sha", commitSha);
        HttpRequest createReq = base("https://api.github.com/repos/" + getRepo() + "/git/refs")
                .POST(HttpRequest.BodyPublishers.ofString(create.toString())).build();
        HttpResponse<String> createRes = client.send(createReq, HttpResponse.BodyHandlers.ofString());
        if (createRes.statusCode() == 201)
            return;

        // if already exists, move it with force=true
        if (createRes.statusCode() == 422) {
            JSONObject patch = new JSONObject().put("sha", commitSha).put("force", true);
            HttpRequest patchReq = base("https://api.github.com/repos/" + getRepo() + "/git/refs/tags/" + tag)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(patch.toString()))
                    .build();
            HttpResponse<String> patchRes = client.send(patchReq, HttpResponse.BodyHandlers.ofString());
            if (patchRes.statusCode() == 200)
                return;
            throw new IOException("update tag failed: " + patchRes.statusCode() + " " + patchRes.body());
        }

        throw new IOException("create tag failed: " + createRes.statusCode() + " " + createRes.body());
    }

    /** Finds the most recent tag in yyyyMMdd format */
    private static String latestDateTag() throws IOException, InterruptedException {
        HttpRequest req = base("https://api.github.com/repos/" + getRepo() + "/tags").build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new IOException("list tags failed: " + res.statusCode() + " " + res.body());

        JSONArray arr = new JSONArray(res.body());
        String best = null;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject t = arr.getJSONObject(i);
            String name = t.getString("name");
            if (DATE_TAG.matcher(name).matches() && (best == null || name.compareTo(best) > 0)) {
                best = name;
            }
        }
        if (best == null)
            throw new IOException("No tag in yyyyMMdd format found");
        return best;
    }

    /** Downloads the content of a file at a given ref */
    private static byte[] downloadContentAtRef(String path, String ref) throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + getRepo() + "/contents/" + path + "?ref=" + ref;
        HttpRequest req = base(url).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new IOException("download failed: " + res.statusCode() + " " + res.body());

        JSONObject j = new JSONObject(res.body());
        String enc = j.getString("encoding");
        if (!"base64".equals(enc))
            throw new IOException("Unexpected encoding: " + enc);
        String base64 = j.getString("content").replace("\n", "").replace("\r", "");
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Uploads:
     * - backup.dnd (latest snapshot)
     * - backups/game_yyyyMMdd.dnd (overwrites today's copy)
     * - tag yyyyMMdd pointing to the commit that wrote backup.dnd
     */
    public static void upload(File file) throws IOException, InterruptedException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String branch = defaultBranch();

        // 1) update backup.dnd and get commit SHA from PUT
        JSONObject put1 = putFile("backup.dnd", bytes, "backup update (" + LocalDate.now() + ")", branch);
        String commitSha = put1.getJSONObject("commit").getString("sha");

        // 2) dated copy
        String datedName = "backups/game_" + LocalDate.now().format(TAG_FMT) + ".dnd";
        putFile(datedName, bytes, "backup copy for " + LocalDate.now(), branch);

        // 3) tag of the day -> commit from step 1
        String today = LocalDate.now().format(TAG_FMT);
        createOrMoveTagTo(today, commitSha);
        System.out.println("Uploaded and tagged as " + today + " at " + commitSha);
    }

    /**
     * Downloads 'backups/game_yyyyMMdd.dnd' from the most recent date tag
     * and stores it locally in backup/game_yyyyMMdd.dnd
     */
    public static File downloadLatest() throws IOException, InterruptedException {
        String tag = latestDateTag();
        String filename = "game_" + tag + ".dnd";
        String path = "backups/" + filename;
        byte[] bytes = downloadContentAtRef(path, tag);
        File backupDir = new File("backup/");
        if (!backupDir.exists()) backupDir.mkdirs();
        File dest = new File(backupDir, filename);
        Files.write(dest.toPath(), bytes);

        System.out.println("Downloaded " + dest.getAbsolutePath() + " from tag " + tag);
        return dest;
    }
}
