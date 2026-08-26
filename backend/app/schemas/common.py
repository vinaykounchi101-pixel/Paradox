from typing import Generic, List, TypeVar
from pydantic import BaseModel

T = TypeVar("T")


class PaginationMeta(BaseModel):
    page: int
    page_size: int
    total_items: int
    total_pages: int


class PaginatedEnvelope(BaseModel, Generic[T]):
    data: List[T]
    meta: PaginationMeta


class DataEnvelope(BaseModel, Generic[T]):
    data: T
