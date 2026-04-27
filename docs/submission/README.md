# Submission evidence (course / assignment)

Place **screenshots** and **log excerpts** here before packaging your submission. Suggested filenames:

| Artifact | Suggested path | How to produce |
|----------|----------------|----------------|
| Stub `mvn verify` green | `logs/mvn-verify-stub.txt` | `mvn clean verify` (requires Docker for Testcontainers) |
| REST chat (weather) | `screenshots/rest-chat-weather.png` | `POST /api/v1/chat/messages` with a weather question (stub profile) |
| REST AskUser + resume | `screenshots/rest-askuser-flow.png` | Two-step `ASK_USER` then `POST .../answers/{ticket}` |
| Thymeleaf chat + disclaimer | `screenshots/ui-chat-disclaimer.png` | Browser `/chat` |
| Evaluation report | `screenshots/eval-report.png` | `POST /api/v1/evaluation/run` or `/evaluation` UI |
| Optional: CLI eval | `logs/eval-cli.txt` | See repository [README — Evaluation CLI](https://github.com/berdachuk/ai-architect-6-agents/blob/main/README.md#evaluation-cli-no-http-server) |

Reference the paths you actually used in your written submission (README or cover document).
