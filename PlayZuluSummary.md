# PlayZulu Project Summary (KHASINA)

## Project Structure
- **Android App**: `KHASINA` (Kotlin/Compose)
- **Backend Microservices**: Python (FastAPI/SQLAlchemy)
  - **PLAYAUTH**: Port 8000. Handles Users, Profiles, Tokens.
  - **PLAYCHAT**: Port 8001. Handles Rooms, Messages, Presence (WebSocket).
  - **PLAYGAME**: Port 8002. Handles Match sessions, actions, events.
- **Database**: PostgreSQL (`playzulu`) on port 5432.

## Connection Details (Local Test)
- **Range**: `192.168.8.100` to `105`
- **Discovery**: Dynamic scanning of above range via `/identity` endpoint.
- **Ports**: 8000 (Auth), 8001 (Chat), 8002 (Game)
- **Startup**: Run `powershell -File "C:\Users\phamen\Documents\PlayZulu 2\start_servers.ps1"` to activate all microservices.

## Data Models
- **User**: `id (UUID)`, `username`, `email`, `rating`
- **Profile**: `user_id`, `display_name`, `country`, `bio`
- **ChatMessage**: `sender`, `text`, `timestamp`
- **GameSession**: `id`, `players`, `status`, `state`, `version`

## Key Logic
- **Authentication**: JWT Bearer token required for protected endpoints.
- **Matchmaking**: 
  1. Fetch online players from `PLAYCHAT/presence/online-players`.
  2. Initiate match via `PLAYGAME/sessions`.
  3. Real-time sync via event polling (3s).
- **Gameplay**: Manual selection of hand/floor cards. 5s delay for multi-stage plays. 3s delay for AI turn.

## Status Indicators
- ⚪ **Guest**: Not logged in.
- 🟠 **Offline**: Authenticated but server unreachable.
- 🟡 **Connecting**: Discovery or Auth in progress.
- 🟢 **Online**: Fully connected and authenticated.
