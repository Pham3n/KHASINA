from typing import Any
from uuid import uuid4

from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field

from playzulu.database import SQLiteUserStore
from playzulu.games import registry
from playzulu.models import User
from playzulu.store import InMemorySessionStore

router = APIRouter()
store = InMemorySessionStore()
user_store = SQLiteUserStore()


class CreateSessionRequest(BaseModel):
    game_id: str = Field(..., examples=["umlabalaba"])
    players: list[str] = Field(..., min_length=2, max_length=8)


class GameActionRequest(BaseModel):
    player_id: str
    type: str
    payload: dict[str, Any] = Field(default_factory=dict)


class CreateUserRequest(BaseModel):
    username: str = Field(..., min_length=1, max_length=40)
    country: str = Field(..., min_length=1, max_length=80)
    gender: str = Field(..., min_length=1, max_length=40)


@router.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "PlayZulu"}


@router.get("/games")
def list_games() -> list[dict[str, Any]]:
    return [game.metadata.model_dump() for game in registry.all()]


@router.post("/sessions", status_code=status.HTTP_201_CREATED)
def create_session(request: CreateSessionRequest) -> dict[str, Any]:
    game = registry.get(request.game_id)
    if game is None:
        raise HTTPException(status_code=404, detail=f"Unknown game: {request.game_id}")

    try:
        session = game.create_session(session_id=str(uuid4()), player_ids=request.players)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    store.save(session)
    return session.model_dump(mode="json")


@router.get("/sessions/{session_id}")
def get_session(session_id: str) -> dict[str, Any]:
    session = store.get(session_id)
    if session is None:
        raise HTTPException(status_code=404, detail=f"Unknown session: {session_id}")
    return session.model_dump(mode="json")


@router.post("/sessions/{session_id}/actions")
def apply_action(session_id: str, request: GameActionRequest) -> dict[str, Any]:
    session = store.get(session_id)
    if session is None:
        raise HTTPException(status_code=404, detail=f"Unknown session: {session_id}")

    game = registry.get(session.game_id)
    if game is None:
        raise HTTPException(status_code=500, detail=f"Game handler missing: {session.game_id}")

    try:
        updated = game.apply_action(
            session=session,
            player_id=request.player_id,
            action_type=request.type,
            payload=request.payload,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    store.save(updated)
    return updated.model_dump(mode="json")


@router.post("/users", status_code=status.HTTP_201_CREATED)
def create_user(request: CreateUserRequest) -> User:
    try:
        return user_store.create_user(
            username=request.username,
            country=request.country,
            gender=request.gender,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.get("/users")
def list_users() -> list[User]:
    return user_store.list_users()


@router.get("/users/{user_id}")
def get_user(user_id: str) -> User:
    user = user_store.get_user(user_id)
    if user is None:
        raise HTTPException(status_code=404, detail=f"Unknown user: {user_id}")
    return user
