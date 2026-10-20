"""Private per-user directories for the jdtls helpers (no predictable /tmp path)."""

import os
import stat
from pathlib import Path


def check_private_dir(path: Path) -> None:
    """Refuse a directory that is a symlink, not owned by us, or writable by others."""
    st = os.lstat(path)
    if not stat.S_ISDIR(st.st_mode):
        raise PermissionError(f"{path} is not a real directory")
    if st.st_uid != os.geteuid():
        raise PermissionError(f"{path} is not owned by the current user")
    if st.st_mode & 0o022:
        raise PermissionError(f"{path} is writable by other users")


def ensure_private_dir(path) -> Path:
    """Create `path` (and missing parents) with mode 0700, or validate it when it exists."""
    path = Path(path)
    if not path.exists() and not path.is_symlink():
        if not path.parent.exists():
            ensure_private_dir(path.parent)
        try:
            os.mkdir(path, 0o700)
        except FileExistsError:
            pass
    check_private_dir(path)
    return path


def base_dir() -> Path:
    """Per-user base directory: $XDG_RUNTIME_DIR, else $XDG_CACHE_HOME or ~/.cache."""
    runtime = os.environ.get("XDG_RUNTIME_DIR")
    if runtime and os.path.isabs(runtime):
        root = Path(runtime)
    else:
        cache = os.environ.get("XDG_CACHE_HOME")
        root = Path(cache) if cache and os.path.isabs(cache) else Path.home() / ".cache"
    return ensure_private_dir(root / "archidata-jdtls")


def workspace_dir(project_name: str) -> Path:
    """Private jdtls `-data` directory of one project."""
    if not project_name or "/" in project_name or project_name in (".", ".."):
        raise ValueError(f"invalid project name: {project_name!r}")
    return ensure_private_dir(ensure_private_dir(base_dir() / "workspaces") / f"jdtls-{project_name}")
