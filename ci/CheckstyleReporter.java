import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Parses the Checkstyle XML report and posts a formatted comment on the pull request summarizing
 * found violations.
 *
 * <p>Usage: {@code java CheckstyleReporter.java <reportPath> <repo> <prNumber> <token>}
 */
public class CheckstyleReporter {

  /**
   * Entry point for the reporter script.
   *
   * @param args command-line arguments: report path, repository, PR number, and GitHub token
   * @throws Exception if the report cannot be parsed or the API call fails
   */
  public static void main(String[] args) throws Exception {
    String reportPath = args[0];
    String repo = args[1];
    String prNumber = args[2];
    String token = args[3];

    Document doc = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(Path.of(reportPath).toFile());

    NodeList fileNodes = doc.getElementsByTagName("file");
    StringBuilder filesSection = new StringBuilder();
    int totalErrors = 0;

    for (int i = 0; i < fileNodes.getLength(); i++) {
      Element fileEl = (Element) fileNodes.item(i);
      NodeList errorNodes = fileEl.getElementsByTagName("error");

      if (errorNodes.getLength() == 0) {
        continue;
      }

      totalErrors += errorNodes.getLength();
      filesSection.append("Arquivo `")
          .append(fileEl.getAttribute("name"))
          .append("`\n\n");

      for (int j = 0; j < errorNodes.getLength(); j++) {
        Element errorEl = (Element) errorNodes.item(j);
        filesSection.append("* Linha ")
            .append(errorEl.getAttribute("line"))
            .append(": `")
            .append(errorEl.getAttribute("message"))
            .append("`\n");
      }
      filesSection.append("\n");
    }

    StringBuilder body = new StringBuilder();
    if (totalErrors == 0) {
      body.append("**Não foram encontrados erros**");
    } else {
      body.append("**Foram encontrados ")
          .append(totalErrors)
          .append(" avisos:**\n\n")
          .append(filesSection);
    }

    postComment(repo, prNumber, token, body.toString());
  }

  private static void postComment(String repo, String prNumber, String token, String body)
      throws IOException, InterruptedException {
    String jsonBody = "{\"body\": " + toJsonString(body) + "}";

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(
            "https://api.github.com/repos/" + repo + "/issues/" + prNumber + "/comments"))
        .header("Authorization", "token " + token)
        .header("Accept", "application/vnd.github+json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
        .build();

    HttpClient client = HttpClient.newHttpClient();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println("GitHub API response: " + response.statusCode());
  }

  private static String toJsonString(String raw) {
    String escaped = raw
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n");
    return "\"" + escaped + "\"";
  }
}