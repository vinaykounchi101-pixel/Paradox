from decimal import Decimal
from app.utils.money import round_monetary


def test_round_monetary():
    assert round_monetary(Decimal("10.123")) == Decimal("10.12")
    assert round_monetary(Decimal("10.125")) == Decimal("10.13")
    assert round_monetary(Decimal("10.126")) == Decimal("10.13")
    assert round_monetary(Decimal("0.004")) == Decimal("0.00")
    assert round_monetary(Decimal("0.005")) == Decimal("0.01")
