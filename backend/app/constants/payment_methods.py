import uuid

STARTER_PAYMENT_METHODS = {
    "Cash": uuid.UUID("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    "Debit Card": uuid.UUID("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
    "Credit Card": uuid.UUID("cccccccc-cccc-cccc-cccc-cccccccccccc"),
    "Bank Transfer": uuid.UUID("dddddddd-dddd-dddd-dddd-dddddddddddd"),
    "Digital Wallet": uuid.UUID("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"),
    "Other": uuid.UUID("ffffffff-ffff-ffff-ffff-ffffffffffff"),  # Fallback payment method
}

OTHER_PAYMENT_METHOD_ID = STARTER_PAYMENT_METHODS["Other"]
