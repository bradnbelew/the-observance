#!/usr/bin/env python3
"""Capture one exact visible Windows window without injecting any input.

This is a bounded Windows 10 fallback for hosts where Windows.Graphics.Capture
fails before returning a frame. It captures only the selected window's extended
frame bounds from the visible desktop and writes a hash-bound JSON receipt.
"""

from __future__ import annotations

import argparse
import ctypes
from ctypes import wintypes
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import platform
import sys
import time

from PIL import ImageGrab, ImageStat


PROCESS_QUERY_LIMITED_INFORMATION = 0x1000
DWMWA_EXTENDED_FRAME_BOUNDS = 9

user32 = ctypes.WinDLL("user32", use_last_error=True)
dwmapi = ctypes.WinDLL("dwmapi", use_last_error=True)
kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)

EnumWindowsProc = ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)

user32.EnumWindows.argtypes = (EnumWindowsProc, wintypes.LPARAM)
user32.EnumWindows.restype = wintypes.BOOL
user32.IsWindowVisible.argtypes = (wintypes.HWND,)
user32.IsWindowVisible.restype = wintypes.BOOL
user32.IsIconic.argtypes = (wintypes.HWND,)
user32.IsIconic.restype = wintypes.BOOL
user32.GetWindowTextLengthW.argtypes = (wintypes.HWND,)
user32.GetWindowTextLengthW.restype = ctypes.c_int
user32.GetWindowTextW.argtypes = (wintypes.HWND, wintypes.LPWSTR, ctypes.c_int)
user32.GetWindowTextW.restype = ctypes.c_int
user32.GetWindowThreadProcessId.argtypes = (wintypes.HWND, ctypes.POINTER(wintypes.DWORD))
user32.GetWindowThreadProcessId.restype = wintypes.DWORD
user32.GetWindowRect.argtypes = (wintypes.HWND, ctypes.POINTER(wintypes.RECT))
user32.GetWindowRect.restype = wintypes.BOOL
user32.GetSystemMetrics.argtypes = (ctypes.c_int,)
user32.GetSystemMetrics.restype = ctypes.c_int

dwmapi.DwmGetWindowAttribute.argtypes = (
    wintypes.HWND,
    wintypes.DWORD,
    wintypes.LPVOID,
    wintypes.DWORD,
)
dwmapi.DwmGetWindowAttribute.restype = ctypes.c_long

kernel32.OpenProcess.argtypes = (wintypes.DWORD, wintypes.BOOL, wintypes.DWORD)
kernel32.OpenProcess.restype = wintypes.HANDLE
kernel32.CloseHandle.argtypes = (wintypes.HANDLE,)
kernel32.CloseHandle.restype = wintypes.BOOL
kernel32.QueryFullProcessImageNameW.argtypes = (
    wintypes.HANDLE,
    wintypes.DWORD,
    wintypes.LPWSTR,
    ctypes.POINTER(wintypes.DWORD),
)
kernel32.QueryFullProcessImageNameW.restype = wintypes.BOOL


def window_text(hwnd: int) -> str:
    length = user32.GetWindowTextLengthW(hwnd)
    if length <= 0:
        return ""
    buffer = ctypes.create_unicode_buffer(length + 1)
    user32.GetWindowTextW(hwnd, buffer, len(buffer))
    return buffer.value


def window_pid(hwnd: int) -> int:
    pid = wintypes.DWORD()
    user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
    return int(pid.value)


def process_image(pid: int) -> str | None:
    handle = kernel32.OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, False, pid)
    if not handle:
        return None
    try:
        capacity = wintypes.DWORD(32768)
        buffer = ctypes.create_unicode_buffer(capacity.value)
        if not kernel32.QueryFullProcessImageNameW(handle, 0, buffer, ctypes.byref(capacity)):
            return None
        return buffer.value
    finally:
        kernel32.CloseHandle(handle)


def enumerate_windows() -> list[dict[str, object]]:
    windows: list[dict[str, object]] = []
    callback_errors: list[str] = []

    @EnumWindowsProc
    def callback(hwnd: int, _lparam: int) -> int:
        try:
            if user32.IsWindowVisible(hwnd):
                title = window_text(hwnd)
                if title:
                    pid = window_pid(hwnd)
                    windows.append({
                        "hwnd": int(hwnd),
                        "pid": pid,
                        "title": title,
                        "minimized": bool(user32.IsIconic(hwnd)),
                        "process_image": process_image(pid),
                    })
        except Exception as error:  # Keep enumerating unrelated windows.
            callback_errors.append(f"{type(error).__name__}: {error}")
        return 1

    if not user32.EnumWindows(callback, 0):
        raise ctypes.WinError(ctypes.get_last_error())
    if callback_errors and not windows:
        raise RuntimeError(f"window enumeration failed: {callback_errors[0]}")
    return windows


def window_bounds(hwnd: int) -> tuple[int, int, int, int]:
    rect = wintypes.RECT()
    result = dwmapi.DwmGetWindowAttribute(
        hwnd,
        DWMWA_EXTENDED_FRAME_BOUNDS,
        ctypes.byref(rect),
        ctypes.sizeof(rect),
    )
    if result != 0 and not user32.GetWindowRect(hwnd, ctypes.byref(rect)):
        raise ctypes.WinError(ctypes.get_last_error())
    return int(rect.left), int(rect.top), int(rect.right), int(rect.bottom)


def virtual_screen_bounds() -> tuple[int, int, int, int]:
    left = user32.GetSystemMetrics(76)  # SM_XVIRTUALSCREEN
    top = user32.GetSystemMetrics(77)  # SM_YVIRTUALSCREEN
    width = user32.GetSystemMetrics(78)  # SM_CXVIRTUALSCREEN
    height = user32.GetSystemMetrics(79)  # SM_CYVIRTUALSCREEN
    return left, top, left + width, top + height


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--title", required=True, help="Exact case-sensitive window title")
    parser.add_argument("--process-id", type=int, help="Optional exact process id")
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--receipt", required=True, type=Path)
    parser.add_argument("--settle-ms", type=int, default=500)
    return parser.parse_args()


def main() -> int:
    if sys.platform != "win32":
        raise SystemExit("Windows only")
    args = parse_args()
    if args.settle_ms < 0 or args.settle_ms > 10_000:
        raise SystemExit("--settle-ms must be between 0 and 10000")

    candidates = [row for row in enumerate_windows() if row["title"] == args.title]
    if args.process_id is not None:
        candidates = [row for row in candidates if row["pid"] == args.process_id]
    if len(candidates) != 1:
        print(json.dumps({"status": "refused", "candidates": candidates}, indent=2), file=sys.stderr)
        raise SystemExit(f"expected exactly one exact-title window; found {len(candidates)}")

    target = candidates[0]
    if target["minimized"]:
        raise SystemExit("target window is minimized")
    bounds = window_bounds(int(target["hwnd"]))
    if bounds[2] <= bounds[0] or bounds[3] <= bounds[1]:
        raise SystemExit(f"invalid target bounds: {bounds}")
    virtual = virtual_screen_bounds()
    if not (bounds[0] >= virtual[0] and bounds[1] >= virtual[1]
            and bounds[2] <= virtual[2] and bounds[3] <= virtual[3]):
        raise SystemExit(f"target is partially outside the virtual screen: {bounds} not within {virtual}")

    time.sleep(args.settle_ms / 1000)
    image = ImageGrab.grab(bbox=bounds, all_screens=True).convert("RGB")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    image.save(temporary, format="PNG")
    os.replace(temporary, args.output)

    grayscale = image.convert("L")
    histogram = grayscale.histogram()
    pixels = image.width * image.height
    black_ratio = sum(histogram[:8]) / pixels
    white_ratio = sum(histogram[248:]) / pixels
    extrema = ImageStat.Stat(image).extrema
    entropy = grayscale.entropy()
    receipt = {
        "status": "captured" if entropy >= 1.0 and black_ratio < 0.98 else "degenerate_frame",
        "captured_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "capture_backend": "Pillow.ImageGrab visible-desktop exact-window bounds",
        "input_injected": False,
        "window": {**target, "bounds": list(bounds)},
        "virtual_screen_bounds": list(virtual),
        "image": {
            "path": args.output.as_posix(),
            "sha256": sha256(args.output),
            "width": image.width,
            "height": image.height,
            "grayscale_entropy": round(entropy, 6),
            "black_pixel_ratio": round(black_ratio, 6),
            "white_pixel_ratio": round(white_ratio, 6),
            "channel_extrema": [list(pair) for pair in extrema],
        },
        "host": {
            "platform": platform.platform(),
            "windows_release": platform.release(),
            "windows_version": platform.version(),
        },
    }
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    args.receipt.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, indent=2))
    return 0 if receipt["status"] == "captured" else 2


if __name__ == "__main__":
    raise SystemExit(main())
