from typing import Any

from pydantic import BaseModel, Field

from playzulu.games.base import Game
from playzulu.models import GameMetadata, GameSession


class Card(BaseModel):
    id: str
    label: str


class KhasinaState(BaseModel):
    hands: dict[str, list[Card]]
    floor: list[Card] = Field(default_factory=list)
    discard: list[Card] = Field(default_factory=list)
    notes: list[str] = Field(default_factory=list)


class KhasinaGame(Game):
    metadata = GameMetadata(
        id="khasina",
        name="KHASINA",
        min_players=2,
        max_players=8,
        actions=["add_card_to_hand", "place_on_floor", "resign"],
    )

    def create_session(self, session_id: str, player_ids: list[str]) -> GameSession:
        if len(player_ids) < 2:
            raise ValueError("KHASINA requires at least 2 players.")
        if len(set(player_ids)) != len(player_ids):
            raise ValueError("Player ids must be unique.")

        state = KhasinaState(hands={player_id: [] for player_id in player_ids})
        return GameSession(
            id=session_id,
            game_id=self.metadata.id,
            players=player_ids,
            current_player=player_ids[0],
            state=state.model_dump(mode="json"),
        )

    def apply_action(
        self,
        session: GameSession,
        player_id: str,
        action_type: str,
        payload: dict[str, Any],
    ) -> GameSession:
        if session.status != "active":
            raise ValueError("This session is already finished.")
        if player_id not in session.players:
            raise ValueError("Player is not part of this session.")

        state = KhasinaState.model_validate(session.state)
        if action_type == "add_card_to_hand":
            card = Card.model_validate(payload.get("card"))
            state.hands[player_id].append(card)
        elif action_type == "place_on_floor":
            card_id = payload.get("card_id")
            if not isinstance(card_id, str):
                raise ValueError("card_id is required.")
            card = self._remove_card_from_hand(state, player_id, card_id)
            state.floor.append(card)
            session.current_player = self._next_player(session, player_id)
        elif action_type == "resign":
            session.status = "finished"
            state.notes.append(f"{player_id} resigned.")
        else:
            raise ValueError(f"Unsupported KHASINA action: {action_type}")

        session.state = state.model_dump(mode="json")
        session.version += 1
        return session

    def _remove_card_from_hand(self, state: KhasinaState, player_id: str, card_id: str) -> Card:
        hand = state.hands[player_id]
        for index, card in enumerate(hand):
            if card.id == card_id:
                return hand.pop(index)
        raise ValueError("Card is not in this player's hand.")

    def _next_player(self, session: GameSession, player_id: str) -> str:
        index = session.players.index(player_id)
        return session.players[(index + 1) % len(session.players)]
