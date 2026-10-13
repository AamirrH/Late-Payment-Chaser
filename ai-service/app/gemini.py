import os
from google import genai
from google.genai import types
from pydantic import BaseModel


def generate(schema: type[BaseModel], system: str, contents: list, timeout_ms: int = 20_000) -> BaseModel:
    key, model = os.getenv("GEMINI_API_KEY"), os.getenv("GEMINI_MODEL")
    if not key or not model:
        raise RuntimeError("Gemini is not configured")
    # One bounded call here. Extraction retries at the endpoint; draft retries
    # belong to the finance orchestrator so verification attempts stay auditable.
    if not 1 <= timeout_ms <= 20_000:
        raise ValueError("Gemini timeout must be between 1 ms and 20,000 ms")
    with genai.Client(api_key=key, http_options=types.HttpOptions(
        timeout=timeout_ms, retry_options=types.HttpRetryOptions(attempts=1)
    )) as client:
        response = client.models.generate_content(
            model=model,
            contents=contents,
            config=types.GenerateContentConfig(
                system_instruction=system,
                temperature=0.1,
                response_mime_type="application/json",
                response_schema=schema,
            ),
        )
        return schema.model_validate_json(response.text)
