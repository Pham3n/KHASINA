from abc import ABC, abstractmethod
from typing import Any

from playzulu.models import GameMetadata, GameSession


class Game(ABC):
    metadata: GameMetadata

    @abstractmethod
    def create_session(self, session_id: str, player_ids: list[str]) -> GameSession:
        raise NotImplementedError

    @abstractmethod
    def apply_action(
        self,
        session: GameSession,
        player_id: str,
        action_type: str,
        payload: dict[str, Any],
    ) -> GameSession:
        raise NotImplementedError
