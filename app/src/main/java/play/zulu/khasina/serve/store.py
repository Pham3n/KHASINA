from playzulu.models import GameSession


class InMemorySessionStore:
    def __init__(self) -> None:
        self._sessions: dict[str, GameSession] = {}

    def save(self, session: GameSession) -> None:
        self._sessions[session.id] = session

    def get(self, session_id: str) -> GameSession | None:
        return self._sessions.get(session_id)
