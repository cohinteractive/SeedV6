"""Delete one private, verified file. Never recursively delete a directory."""
from contextlib import ExitStack
import hashlib
import os
from pathlib import Path
import stat


def identity(info):
    return [info.st_dev, info.st_ino, info.st_mtime_ns]


def verify(stream, record):
    info = os.fstat(stream.fileno())
    if (not stat.S_ISREG(info.st_mode) or info.st_nlink != 1 or info.st_size != record['bytes'] or
            identity(info) != record['identity'] or hashlib.file_digest(stream, 'sha256').hexdigest() != record['sha256']):
        raise ValueError('Deletion target changed while opening it')


def unlink_verified(root, name, record):
    from training_lifecycle import checked, relative
    path = relative(root, name)
    checked(path)
    if os.name == 'nt':
        import ctypes
        from ctypes import wintypes as w
        import msvcrt
        kernel = ctypes.WinDLL('kernel32', use_last_error=True)
        kernel.CreateFileW.argtypes = [w.LPCWSTR, w.DWORD, w.DWORD, w.LPVOID, w.DWORD, w.DWORD, w.HANDLE]
        kernel.CreateFileW.restype = w.HANDLE
        kernel.CloseHandle.argtypes = [w.HANDLE]
        kernel.SetFileInformationByHandle.argtypes = [w.HANDLE, ctypes.c_int, w.LPVOID, w.DWORD]
        kernel.SetFileInformationByHandle.restype = w.BOOL
        invalid = ctypes.c_void_p(-1).value
        def opened(p, access, share, flags):
            handle = kernel.CreateFileW(str(p), access, share, None, 3, flags, None)
            if handle == invalid:
                raise ctypes.WinError(ctypes.get_last_error())
            return handle
        with ExitStack() as stack:
            # Deny deletion/renaming of every ancestor until the file is gone.
            for parent in reversed(path.parents):
                handle = opened(parent, 0, 3, 0x02000000 | 0x00200000)
                stack.callback(kernel.CloseHandle, handle)
                checked(parent, directory=True)
            # Deny other writers/deleters, open the reparse point itself, then
            # compare the actual open file before setting delete disposition.
            handle = opened(path, 0x80000000 | 0x00010000, 1, 0x00200000)
            try:
                fd = msvcrt.open_osfhandle(handle, os.O_RDONLY | os.O_BINARY)
            except BaseException:
                kernel.CloseHandle(handle)
                raise
            with os.fdopen(fd, 'rb') as stream:
                checked(path)
                verify(stream, record)
                disposition = ctypes.c_ubyte(1)
                if not kernel.SetFileInformationByHandle(handle, 4, ctypes.byref(disposition), 1):
                    raise ctypes.WinError(ctypes.get_last_error())
        if path.exists():
            raise OSError('Delete disposition did not remove target')
    else:
        import fcntl
        with ExitStack() as stack:
            directory = os.open(path.anchor, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
            stack.callback(os.close, directory)
            for part in path.parts[1:-1]:
                directory = os.open(part, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=directory)
                stack.callback(os.close, directory)
            fd = os.open(path.name, os.O_RDONLY | os.O_NOFOLLOW, dir_fd=directory)
            with os.fdopen(fd, 'rb') as stream:
                fcntl.flock(stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
                verify(stream, record)
                current = os.stat(path.name, dir_fd=directory, follow_symlinks=False)
                if identity(current) != record['identity'] or current.st_nlink != 1:
                    raise ValueError('Target replaced before unlink')
                checked(path)
                os.unlink(path.name, dir_fd=directory)
                os.fsync(directory)
