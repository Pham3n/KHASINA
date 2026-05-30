from pathlib import Path
from sqlite3 import Connection, IntegrityError, Row, connect
from uuid import uuid4

from playzulu.models import User


class SQLiteUserStore:
    def __init__(self, database_path: str | Path = "playzulu.sqlite3") -> None:
        self.database_path = Path(database_path)
        self._initialize()

    def create_user(self, username: str, country: str, gender: str) -> User:
        user = User(
            id=str(uuid4()),
            username=username.strip(),
            country=country.strip(),
            gender=gender.strip(),
        )
        if not user.username:
            raise ValueError("username is required.")
        if not user.country:
            raise ValueError("country is required.")
        if not user.gender:
            raise ValueError("gender is required.")

        with self._connection() as connection:
            try:
                connection.execute(
                    """
                    INSERT INTO users (id, username, country, gender)
                    VALUES (?, ?, ?, ?)
                    """,
                    (user.id, user.username, user.country, user.gender),
                )
            except IntegrityError as exc:
                raise ValueError("username is already taken.") from exc
        return user

    def get_user(self, user_id: str) -> User | None:
        with self._connection() as connection:
            row = connection.execute(
                "SELECT id, username, country, gender FROM users WHERE id = ?",
                (user_id,),
            ).fetchone()
        return self._row_to_user(row)

    def list_users(self) -> list[User]:
        with self._connection() as connection:
            rows = connection.execute(
                "SELECT id, username, country, gender FROM users ORDER BY username"
            ).fetchall()
        return [self._row_to_user(row) for row in rows if row is not None]

    def _initialize(self) -> None:
        self.database_path.parent.mkdir(parents=True, exist_ok=True)
        with self._connection() as connection:
            connection.execute(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    username TEXT NOT NULL UNIQUE,
                    country TEXT NOT NULL,
                    gender TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
            )

    def _connection(self) -> Connection:
        connection = connect(self.database_path)
        connection.row_factory = Row
        return connection

    def _row_to_user(self, row: Row | None) -> User | None:
        if row is None:
            return None
        return User(
            id=row["id"],
            username=row["username"],
            country=row["country"],
            gender=row["gender"],
        )
