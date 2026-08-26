from decimal import Decimal, ROUND_HALF_UP


def round_monetary(value: Decimal) -> Decimal:
    """Rounds a Decimal value to 2 decimal places using ROUND_HALF_UP."""
    return value.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
