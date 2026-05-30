from playzulu.games.base import Game


class GameRegistry:
    def __init__(self) -> None:
        self._games: dict[str, Game] = {}

    def register(self, game: Game) -> None:
        self._games[game.metadata.id] = game

    def get(self, game_id: str) -> Game | None:
        return self._games.get(game_id)

    def all(self) -> list[Game]:
        return list(self._games.values())
