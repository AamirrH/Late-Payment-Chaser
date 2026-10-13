from datetime import date
from typing import Annotated, Literal
from pydantic import BaseModel, ConfigDict, Field, StrictInt

Money = Annotated[str, Field(pattern=r"^[0-9]{1,12}\.[0-9]{2}$")]


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Numbers(StrictModel):
    principal: Money
    interest: Money
    total: Money
    daysOverdue: Annotated[StrictInt, Field(ge=0)]
    dueDate: date


class ExtractRequest(StrictModel):
    contentBase64: Annotated[str, Field(min_length=1, max_length=14_000_000)]
    mimeType: Literal["image/png", "image/jpeg", "application/pdf"]


class ExtractResponse(StrictModel):
    invoiceNo: Annotated[str, Field(max_length=80)] | None
    amount: Money | None
    buyer: Annotated[str, Field(max_length=160)] | None
    invoiceDate: date | None
    acceptedDate: date | None
    agreedCreditDays: Annotated[StrictInt, Field(ge=0, le=365)] | None
    fieldNotes: Annotated[list[Annotated[str, Field(max_length=500)]], Field(max_length=20)]


class DraftRequest(StrictModel):
    stage: Literal["reminded", "formal", "notice", "filing_ready"]
    tone: Literal["friendly", "firm", "formal"]
    supplier: Annotated[str, Field(min_length=1, max_length=160)]
    buyer: Annotated[str, Field(min_length=1, max_length=160)]
    numbers: Numbers


class DraftResponse(StrictModel):
    subject: Annotated[str, Field(min_length=1, max_length=200)]
    body: Annotated[str, Field(min_length=1, max_length=12000)]
    citedNumbers: Numbers
