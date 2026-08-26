from app.db.session import get_db

# Re-export get_db for router usage
__all__ = ["get_db"]
