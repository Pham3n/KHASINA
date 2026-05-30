from enum import StrEnum
from typing import Any

from pydantic import BaseModel, Field

from playzulu.games.base import Game
from playzulu.models import GameMetadata, GameSession


class UmlabalabaPhase(StrEnum):
    PLACEMENT = "PLACEMENT"
    MOVEMENT = "MOVEMENT"


class UmlabalabaState(BaseModel):
    board: list[str | None] = Field(default_factory=lambda: [None] * 24)
    nodes: list[dict[str, Any]]
    phase: UmlabalabaPhase = UmlabalabaPhase.PLACEMENT
    pieces_to_place: dict[str, int]
    pieces_on_board: dict[str, int]
    pending_capture_for: str | None = None
    winner: str | None = None


class UmlabalabaGame(Game):
    metadata = GameMetadata(
        id="umlabalaba",
        name="UMLABALABA",
        min_players=2,
        max_players=2,
        actions=["place", "move", "capture", "resign"],
    )

    node_coordinates: tuple[tuple[int, int], ...] = (
        (0, 0),
        (3, 0),
        (6, 0),
        (6, 3),
        (6, 6),
        (3, 6),
        (0, 6),
        (0, 3),
        (1, 1),
        (3, 1),
        (5, 1),
        (5, 3),
        (5, 5),
        (3, 5),
        (1, 5),
        (1, 3),
        (2, 2),
        (3, 2),
        (4, 2),
        (4, 3),
        (4, 4),
        (3, 4),
        (2, 4),
        (2, 3),
    )

    # Board topology and rows ported from the Kotlin UMLABALABA client.
    mills: tuple[tuple[int, int, int], ...] = (
        (0, 1, 2),
        (2, 3, 4),
        (4, 5, 6),
        (6, 7, 0),
        (8, 9, 10),
        (10, 11, 12),
        (12, 13, 14),
        (14, 15, 8),
        (16, 17, 18),
        (18, 19, 20),
        (20, 21, 22),
        (22, 23, 16),
        (1, 9, 17),
        (3, 11, 19),
        (5, 13, 21),
        (7, 15, 23),
        (0, 8, 16),
        (2, 10, 18),
        (4, 12, 20),
        (6, 14, 22),
    )

    adjacency: dict[int, set[int]] = {
        0: {1, 7, 8},
        1: {0, 2, 9},
        2: {1, 3, 10},
        3: {2, 4, 11},
        4: {3, 5, 12},
        5: {4, 6, 13},
        6: {5, 7, 14},
        7: {6, 0, 15},
        8: {0, 9, 15, 16},
        9: {1, 8, 10, 17},
        10: {2, 9, 11, 18},
        11: {3, 10, 12, 19},
        12: {4, 11, 13, 20},
        13: {5, 12, 14, 21},
        14: {6, 13, 15, 22},
        15: {7, 14, 8, 23},
        16: {8, 17, 23},
        17: {9, 16, 18},
        18: {10, 17, 19},
        19: {11, 18, 20},
        20: {12, 19, 21},
        21: {13, 20, 22},
        22: {14, 21, 23},
        23: {15, 22, 16},
    }

    def create_session(self, session_id: str, player_ids: list[str]) -> GameSession:
        if len(player_ids) != 2:
            raise ValueError("UMLABALABA requires exactly 2 players.")
        if len(set(player_ids)) != 2:
            raise ValueError("Player ids must be unique.")

        state = UmlabalabaState(
            nodes=self._nodes([None] * 24),
            pieces_to_place={player_ids[0]: 12, player_ids[1]: 12},
            pieces_on_board={player_ids[0]: 0, player_ids[1]: 0},
        )
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
        self._ensure_active(session, player_id)
        state = UmlabalabaState.model_validate(session.state)

        if action_type == "place":
            self._place(session, state, player_id, payload)
        elif action_type == "move":
            self._move(session, state, player_id, payload)
        elif action_type == "capture":
            self._capture(session, state, player_id, payload)
        elif action_type == "resign":
            state.winner = self._opponent(session, player_id)
            session.status = "finished"
        else:
            raise ValueError(f"Unsupported UMLABALABA action: {action_type}")

        state.nodes = self._nodes(state.board)
        session.state = state.model_dump(mode="json")
        session.version += 1
        return session

    def _place(
        self,
        session: GameSession,
        state: UmlabalabaState,
        player_id: str,
        payload: dict[str, Any],
    ) -> None:
        self._require_turn(session, state, player_id)
        if state.phase != UmlabalabaPhase.PLACEMENT:
            raise ValueError("Pieces can only be placed during PLACEMENT.")
        if state.pieces_to_place[player_id] <= 0:
            raise ValueError("This player has no pieces left to place.")

        position = self._position(payload, "position")
        if state.board[position] is not None:
            raise ValueError("That board position is already occupied.")

        state.board[position] = player_id
        state.pieces_to_place[player_id] -= 1
        state.pieces_on_board[player_id] += 1
        self._finish_piece_action(session, state, player_id, position)

        if all(count == 0 for count in state.pieces_to_place.values()):
            state.phase = UmlabalabaPhase.MOVEMENT

    def _move(
        self,
        session: GameSession,
        state: UmlabalabaState,
        player_id: str,
        payload: dict[str, Any],
    ) -> None:
        self._require_turn(session, state, player_id)
        if state.phase != UmlabalabaPhase.MOVEMENT:
            raise ValueError("Pieces can only be moved during MOVEMENT.")

        source = self._position(payload, "from")
        target = self._position(payload, "to")
        if state.board[source] != player_id:
            raise ValueError("The source position does not hold this player's piece.")
        if state.board[target] is not None:
            raise ValueError("The target position is already occupied.")
        can_fly = state.pieces_on_board[player_id] == 3
        if target not in self.adjacency[source] and not can_fly:
            raise ValueError("Pieces must move to an adjacent empty position unless the player has exactly 3 pieces.")

        state.board[source] = None
        state.board[target] = player_id
        self._finish_piece_action(session, state, player_id, target)

    def _capture(
        self,
        session: GameSession,
        state: UmlabalabaState,
        player_id: str,
        payload: dict[str, Any],
    ) -> None:
        if state.pending_capture_for != player_id:
            raise ValueError("This player does not have a pending capture.")

        position = self._position(payload, "position")
        opponent = self._opponent(session, player_id)
        if state.board[position] != opponent:
            raise ValueError("Captures must remove an opponent piece.")
        if self._is_in_mill(state, position) and self._has_piece_outside_mill(state, opponent):
            raise ValueError("Cannot capture a piece from a mill while another opponent piece is outside a mill.")

        state.board[position] = None
        state.pieces_on_board[opponent] -= 1
        state.pending_capture_for = None

        if state.phase == UmlabalabaPhase.MOVEMENT and state.pieces_on_board[opponent] < 3:
            state.winner = player_id
            session.status = "finished"
            return

        session.current_player = opponent

    def _nodes(self, board: list[str | None]) -> list[dict[str, Any]]:
        return [
            {
                "id": node_id,
                "occupant": board[node_id],
                "connections": sorted(self.adjacency[node_id]),
                "x": coordinates[0],
                "y": coordinates[1],
            }
            for node_id, coordinates in enumerate(self.node_coordinates)
        ]

    def _finish_piece_action(
        self,
        session: GameSession,
        state: UmlabalabaState,
        player_id: str,
        position: int,
    ) -> None:
        if self._is_in_mill(state, position):
            state.pending_capture_for = player_id
            return
        session.current_player = self._opponent(session, player_id)

    def _ensure_active(self, session: GameSession, player_id: str) -> None:
        if session.status != "active":
            raise ValueError("This session is already finished.")
        if player_id not in session.players:
            raise ValueError("Player is not part of this session.")

    def _require_turn(self, session: GameSession, state: UmlabalabaState, player_id: str) -> None:
        if state.pending_capture_for:
            raise ValueError("A pending capture must be resolved before the next action.")
        if session.current_player != player_id:
            raise ValueError("It is not this player's turn.")

    def _is_in_mill(self, state: UmlabalabaState, position: int) -> bool:
        owner = state.board[position]
        return owner is not None and any(
            position in mill and all(state.board[index] == owner for index in mill)
            for mill in self.mills
        )

    def _has_piece_outside_mill(self, state: UmlabalabaState, player_id: str) -> bool:
        return any(
            owner == player_id and not self._is_in_mill(state, position)
            for position, owner in enumerate(state.board)
        )

    def _opponent(self, session: GameSession, player_id: str) -> str:
        return next(player for player in session.players if player != player_id)

    def _position(self, payload: dict[str, Any], key: str) -> int:
        if key not in payload:
            raise ValueError(f"Missing payload field: {key}")
        position = payload[key]
        if not isinstance(position, int) or not 0 <= position < 24:
            raise ValueError(f"{key} must be a board position from 0 to 23.")
        return position
