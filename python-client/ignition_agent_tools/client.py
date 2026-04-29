from __future__ import annotations

import json
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any


@dataclass
class IgnitionAgentToolsClient:
    base_url: str
    api_key: str
    timeout: int = 30

    def health(self) -> dict[str, Any]:
        return self._request("GET", "/health")

    def browse(
        self,
        provider: str,
        path: str = "",
        recursive: bool = False,
        max_results: int | None = None,
        continuation_point: str | None = None,
    ) -> dict[str, Any]:
        payload = {
            "provider": provider,
            "path": path,
            "recursive": recursive,
            "maxResults": max_results,
            "continuationPoint": continuation_point,
        }
        return self._request("POST", "/tags/browse", payload)

    def read(self, provider: str, paths: list[str]) -> dict[str, Any]:
        return self._request("POST", "/tags/read", {"provider": provider, "paths": paths})

    def write(self, provider: str, writes: list[dict[str, Any]]) -> dict[str, Any]:
        return self._request("POST", "/tags/write", {"provider": provider, "writes": writes})

    def upsert(
        self,
        provider: str,
        items: list[dict[str, Any]],
        dry_run: bool = False,
        continue_on_error: bool = False,
    ) -> dict[str, Any]:
        payload = {
            "provider": provider,
            "dryRun": dry_run,
            "continueOnError": continue_on_error,
            "items": items,
        }
        return self._request("POST", "/tags/upsert", payload)

    def delete(self, provider: str, paths: list[str], recursive: bool = False) -> dict[str, Any]:
        return self._request(
            "POST",
            "/tags/delete",
            {"provider": provider, "paths": paths, "recursive": recursive},
        )

    def _request(self, method: str, path: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
        body = None if payload is None else json.dumps(payload).encode("utf-8")
        request = urllib.request.Request(
            self.base_url.rstrip("/") + path,
            data=body,
            method=method,
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                return json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            payload_text = exc.read().decode("utf-8")
            try:
                data = json.loads(payload_text)
            except json.JSONDecodeError:
                data = {"success": False, "errors": [{"code": "http_error", "message": payload_text or str(exc)}]}
            raise RuntimeError(data) from exc
