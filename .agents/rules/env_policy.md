# Environment Variable and Secrets Policy

This rule enforces strict constraints on how environment secrets and configs are handled in Paradox.

## strict Constraints

1. **Zero-Touch `.env` and `.env.local` Files**:
   - The agent must **never** create, read, edit, delete, or parse `.env`, `.env.local`, or any file matching `.env*` in the repository.
   - Do not use command-line utilities (like PowerShell `Get-Content`, `Select-String`, etc.) or custom scripts to read or write credentials to these files.
   - The contents of `.env.local` are private to the user; do not attempt to print them.

2. **Environment Variable Driven**:
   - All configurations (database URLs, CORS whitelists, environments) must be resolved dynamically in the application code using standard process environment lookups (e.g., Pydantic `BaseSettings` or `os.environ`).
   - Do not use inline `load_dotenv` or similar file-based loaders inside the codebase.

3. **Execution & Setup Guidance**:
   - When running commands that require credentials (such as Alembic migrations or database seeding scripts), the agent must not assume it can fetch them automatically.
   - Instruct the user on how to run these commands in their own terminal session where environment variables are loaded, or explicitly tell them how to prepend `$env:VAR="value"` (PowerShell) / `VAR=value` (Bash) to run commands locally.
