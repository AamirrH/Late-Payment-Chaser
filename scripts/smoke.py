"""Exercise the real local finance -> AI failure -> verified template chain."""
import json
import os
import urllib.request

base = os.environ.get("FINANCE_URL", "http://127.0.0.1:8080")
token = None


def call(path, method="GET", data=None, binary=False):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode() if data is not None else None
    req = urllib.request.Request(base + path, data=body, method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=85) as response:
        result = response.read()
        return result if binary else json.loads(result)


assert call("/health")["mode"] == "demo", "Run this synthetic smoke test only in demo mode"
token = call("/demo/session", "POST")["token"]
case = call("/cases/demo/overdue", "POST")
cid = case["id"]
fields = {k: v for k, v in case["extracted"].items() if k != "fieldNotes"}
fields["supplier"] = {"businessName": "Synthetic smoke studio", "udyamNumber": "UDYAM-ZZ-00-0000000", "category": "small"}
case = call(f"/cases/{cid}/confirm", "PATCH", fields)
assert case["lastCalc"]["interest"] == "690.41"
case = call(f"/cases/{cid}/draft", "POST", {"tone": "friendly"})
assert case["notices"][-1]["verified"] is True
for expected in ("1450.77", "2197.13"):
    case = call(f"/cases/{cid}/simulate", "POST", {"days": 30})
    assert case["lastCalc"]["interest"] == expected
assert case["stage"] == "filing_ready"
assert call(f"/cases/{cid}/filing-pack", "POST", binary=True).startswith(b"%PDF")
notice = case["notices"][-1]
assert b"2197.13" in call(f"/cases/{cid}/notices/{notice['id']}.txt", binary=True)
call(f"/cases/{cid}/paid", "POST")
assert call("/cases")["totalPrincipal"] == "0.00"
call(f"/cases/{cid}", "DELETE")
print("PASS: real HTTP golden path, verified drafts, simulation, PDF, paid totals and deletion")
