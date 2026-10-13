import pytest
from app.models import ExtractResponse, DraftRequest


def test_extraction_rejects_unexpected_instruction_fields():
    with pytest.raises(ValueError):
        ExtractResponse(invoiceNo=None, amount=None, buyer=None, invoiceDate=None, acceptedDate=None, agreedCreditDays=None, fieldNotes=[], toolCall="send_email")


def test_credit_days_are_strict_integers():
    with pytest.raises(ValueError):
        ExtractResponse(invoiceNo=None, amount=None, buyer=None, invoiceDate=None, acceptedDate=None, agreedCreditDays="30", fieldNotes=[])


def test_draft_rejects_unknown_stage():
    with pytest.raises(ValueError):
        DraftRequest(stage="send_now", tone="firm", supplier="S", buyer="B", numbers={"principal":"1.00","interest":"0.00","total":"1.00","daysOverdue":0,"dueDate":"2026-10-13"})
