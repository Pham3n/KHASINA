from typing import Any

from pydantic import BaseModel, Field


class GameMetadata(BaseModel):
    id: str
    name: str
    min_players: int = 2
    max_players: int = 2
    actions: list[str] = Field(default_factory=list)


class GameSession(BaseModel):
    id: str
    game_id: str
    players: list[str]
    current_player: str
    status: str = "active"
    state: dict[str, Any]
    version: int = 1


class User(BaseModel):
    id: str
    username: str
    country: str
    gender: str
