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

    int fileCount = countFilesWithErrors(fileNodes);
    String body = buildCommentBody(totalErrors, fileCount, filesSection.toString());

    postComment(repo, prNumber, token, body);
  }

  private static int countFilesWithErrors(NodeList fileNodes) {
    int count = 0;
    for (int i = 0; i < fileNodes.getLength(); i++) {
      Element fileEl = (Element) fileNodes.item(i);
      if (fileEl.getElementsByTagName("error").getLength() > 0) {
        count++;
      }
    }
    return count;
  }

  private static String buildCommentBody(int totalErrors, int fileCount, String filesSection) {
    StringBuilder body = new StringBuilder();

    if (totalErrors == 0) {
      body.append("## ✅ Checkstyle — Nenhuma violação encontrada\n\n")
          .append("Todo o código analisado está de acordo com o guia de estilo "
              + "(Google Java Style).\n");
      return body.toString();
    }

    body.append("## ⚠️ Checkstyle — ")
        .append(totalErrors)
        .append(totalErrors == 1 ? " violação encontrada\n\n" : " violações encontradas\n\n")
        .append("| Métrica | Valor |\n")
        .append("|---|---|\n")
        .append("| Arquivos afetados | ").append(fileCount).append(" |\n")
        .append("| Total de violações | ").append(totalErrors).append(" |\n")
        .append("| Guia de estilo | Google Java Style |\n\n")
        .append("<details>\n")
        .append("<summary>Ver detalhes das violações</summary>\n\n")
        .append(filesSection)
        .append("</details>\n\n")
        .append("---\n")
        .append("_Comentário gerado automaticamente pelo Checkstyle CI._\n");

    return body.toString();
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