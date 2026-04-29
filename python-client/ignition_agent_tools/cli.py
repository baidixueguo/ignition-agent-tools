from __future__ import annotations

import argparse
import json
from typing import Any

from .client import IgnitionAgentToolsClient


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Ignition Agent Tools client")
    parser.add_argument("--base-url", required=True, help="Gateway API base URL, e.g. http://localhost:8088/system/data/agent-tools/v1")
    parser.add_argument("--api-key", required=True, help="Bearer token")

    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("health")

    browse = subparsers.add_parser("browse")
    browse.add_argument("--provider", required=True)
    browse.add_argument("--path", default="")
    browse.add_argument("--recursive", action="store_true")

    read = subparsers.add_parser("read")
    read.add_argument("--provider", required=True)
    read.add_argument("paths", nargs="+")

    write = subparsers.add_parser("write")
    write.add_argument("--provider", required=True)
    write.add_argument("--payload", required=True, help="JSON array of {path, value}")

    upsert = subparsers.add_parser("upsert")
    upsert.add_argument("--provider", required=True)
    upsert.add_argument("--payload", required=True, help="JSON array of TagSpec objects")
    upsert.add_argument("--dry-run", action="store_true")
    upsert.add_argument("--continue-on-error", action="store_true")

    delete = subparsers.add_parser("delete")
    delete.add_argument("--provider", required=True)
    delete.add_argument("paths", nargs="+")
    delete.add_argument("--recursive", action="store_true")

    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    client = IgnitionAgentToolsClient(base_url=args.base_url, api_key=args.api_key)

    if args.command == "health":
        print_json(client.health())
    elif args.command == "browse":
        print_json(client.browse(provider=args.provider, path=args.path, recursive=args.recursive))
    elif args.command == "read":
        print_json(client.read(provider=args.provider, paths=args.paths))
    elif args.command == "write":
        print_json(client.write(provider=args.provider, writes=_load_json(args.payload)))
    elif args.command == "upsert":
        print_json(
            client.upsert(
                provider=args.provider,
                items=_load_json(args.payload),
                dry_run=args.dry_run,
                continue_on_error=args.continue_on_error,
            )
        )
    elif args.command == "delete":
        print_json(client.delete(provider=args.provider, paths=args.paths, recursive=args.recursive))


def _load_json(text: str) -> list[dict[str, Any]]:
    value = json.loads(text)
    if not isinstance(value, list):
        raise SystemExit("payload must be a JSON array")
    return value


def print_json(value: Any) -> None:
    print(json.dumps(value, ensure_ascii=False, indent=2))
