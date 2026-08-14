# AI DEVELOPMENT RULES

## Project
Android application written in Kotlin.

## Architecture
Use MVVM with clear separation between:
- UI
- ViewModel
- Domain
- Repository
- Data

## UI
Use Jetpack Compose and Material 3.

## Rules

1. Do not rewrite unrelated files.
2. Do not introduce a dependency without explaining why.
3. Do not duplicate existing functionality.
4. Search the repository before creating a new class.
5. Follow existing naming conventions.
6. Keep functions small.
7. Use Kotlin coroutines for asynchronous operations.
8. Do not perform network operations on the main thread.
9. Do not block the UI thread.
10. Handle errors explicitly.
11. Never log passwords, tokens, URLs containing credentials, or private data.
12. Never trust remote filenames or paths.
13. Validate all network input.
14. Write tests for important business logic.
15. Do not silently change architecture.
16. Do not remove working functionality.
17. Explain changed files after implementation.
18. Build/test after completing the task.
19. Make only the requested change.
20. If requirements are ambiguous, ask before making architectural changes.